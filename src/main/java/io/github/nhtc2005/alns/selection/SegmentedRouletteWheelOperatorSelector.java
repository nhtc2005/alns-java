package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects operators by weight and updates selected operators' weights after each fixed-length
 * feedback segment.
 *
 * <p>Destroy and repair selection is independent, with \(P(i) = w_i / \sum_j w_j\) in each group.
 * Weights start at 1; an all-zero group uses uniform selection. At a segment boundary:
 *
 * <p>\[ w_i' = \theta w_i + (1 - \theta) \frac{\Sigma_i}{n_i}. \]
 *
 * <p>Here \(\theta\) is {@code decay}, and \(\Sigma_i\) is the sum of scores credited to operator
 * \(i\) in that segment, and \(n_i\) is its selection count. Ineligible feedback contributes the
 * rejected-outcome score {@code scores[3]}, counts as a selection, and advances the segment.
 * Operators with no selections retain their weights. Segment scores and selection counts are
 * cleared after updating. Instances hold mutable state and must not be shared by concurrent runs.
 *
 * @param <S> the solution type
 */
public final class SegmentedRouletteWheelOperatorSelector<S extends Solution<S>>
    implements OperatorSelector<S> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SegmentedRouletteWheelOperatorSelector.class);

  private final double[] scores;
  private final double decay;
  private final long segmentLength;
  private final double[] destroyOperatorWeights;
  private final double[] repairOperatorWeights;
  private final double[] destroyOperatorSegmentScores;
  private final double[] repairOperatorSegmentScores;

  private final long[] destroyOperatorSelectionCounts;
  private final long[] repairOperatorSelectionCounts;

  private long iterationsInSegment;

  /**
   * Creates a selector for fixed operator counts.
   *
   * @param destroyOperatorCount the positive number of destroy operators
   * @param repairOperatorCount the positive number of repair operators
   * @param scores four finite non-negative scores in BEST, BETTER, ACCEPTED, REJECTED order; copied
   *     on construction
   * @param decay the finite weight retention factor in {@code [0, 1]}
   * @param segmentLength the positive number of feedback updates per segment
   * @throws IllegalArgumentException if any numeric parameter or score count is invalid
   * @throws NullPointerException if {@code scores} is null
   */
  public SegmentedRouletteWheelOperatorSelector(
      int destroyOperatorCount,
      int repairOperatorCount,
      double[] scores,
      double decay,
      long segmentLength) {
    if (destroyOperatorCount <= 0 || repairOperatorCount <= 0) {
      throw new IllegalArgumentException("Operator counts must be positive.");
    }

    Objects.requireNonNull(scores, "scores must not be null.");
    if (scores.length != 4) {
      throw new IllegalArgumentException("scores must contain exactly four values.");
    }
    this.scores = scores.clone();
    for (double score : this.scores) {
      if (!Double.isFinite(score) || score < 0.0) {
        throw new IllegalArgumentException("scores must be finite and non-negative.");
      }
    }

    if (!Double.isFinite(decay) || decay < 0.0 || decay > 1.0) {
      throw new IllegalArgumentException("decay must be finite and in the range [0.0, 1.0].");
    }
    if (segmentLength <= 0) {
      throw new IllegalArgumentException("segmentLength must be positive.");
    }

    this.decay = decay;
    this.segmentLength = segmentLength;
    this.destroyOperatorWeights = new double[destroyOperatorCount];
    this.repairOperatorWeights = new double[repairOperatorCount];
    this.destroyOperatorSegmentScores = new double[destroyOperatorCount];
    this.repairOperatorSegmentScores = new double[repairOperatorCount];
    this.destroyOperatorSelectionCounts = new long[destroyOperatorCount];
    this.repairOperatorSelectionCounts = new long[repairOperatorCount];
    Arrays.fill(this.destroyOperatorWeights, 1.0);
    Arrays.fill(this.repairOperatorWeights, 1.0);
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if either count differs from this selector's configured count
   */
  @Override
  public void validateOperatorCounts(int destroyOperatorCount, int repairOperatorCount) {
    if (destroyOperatorCount != getDestroyOperatorCount()
        || repairOperatorCount != getRepairOperatorCount()) {
      throw new IllegalArgumentException(
          "Selector operator counts must match the configured destroy and repair operator counts.");
    }
  }

  @Override
  public OperatorSelection select(OperatorSelectionContext<S> context) {
    return new OperatorSelection(
        select(destroyOperatorWeights, context.randomGenerator()),
        select(repairOperatorWeights, context.randomGenerator()));
  }

  /**
   * {@inheritDoc}
   *
   * @throws IndexOutOfBoundsException if either selected operator index is out of range
   * @throws ArithmeticException if accumulated segment scores would become non-finite
   */
  @Override
  public void update(OperatorSelectionFeedback<S> feedback) {
    int destroyOperatorIndex =
        Objects.checkIndex(
            feedback.operatorSelection().destroyOperatorIndex(), destroyOperatorWeights.length);
    int repairOperatorIndex =
        Objects.checkIndex(
            feedback.operatorSelection().repairOperatorIndex(), repairOperatorWeights.length);

    double score =
        feedback.isRewardEligible()
            ? switch (feedback.status()) {
              case BEST -> scores[0];
              case BETTER -> scores[1];
              case ACCEPTED -> scores[2];
              case REJECTED -> scores[3];
            }
            : scores[3];

    double destroySegmentScore = destroyOperatorSegmentScores[destroyOperatorIndex] + score;
    double repairSegmentScore = repairOperatorSegmentScores[repairOperatorIndex] + score;
    if (!Double.isFinite(destroySegmentScore) || !Double.isFinite(repairSegmentScore)) {
      throw new ArithmeticException("Accumulated segment scores must remain finite.");
    }

    destroyOperatorSegmentScores[destroyOperatorIndex] = destroySegmentScore;
    repairOperatorSegmentScores[repairOperatorIndex] = repairSegmentScore;
    destroyOperatorSelectionCounts[destroyOperatorIndex]++;
    repairOperatorSelectionCounts[repairOperatorIndex]++;
    iterationsInSegment++;

    if (iterationsInSegment == segmentLength) {
      updateWeights(
          "Destroy",
          destroyOperatorWeights,
          destroyOperatorSegmentScores,
          destroyOperatorSelectionCounts);
      updateWeights(
          "Repair",
          repairOperatorWeights,
          repairOperatorSegmentScores,
          repairOperatorSelectionCounts);
      Arrays.fill(destroyOperatorSegmentScores, 0.0);
      Arrays.fill(repairOperatorSegmentScores, 0.0);
      Arrays.fill(destroyOperatorSelectionCounts, 0);
      Arrays.fill(repairOperatorSelectionCounts, 0);
      iterationsInSegment = 0;
    }
  }

  /**
   * Restores all weights to 1 and clears segment scores, selection counts and the segment counter.
   */
  @Override
  public void reset() {
    Arrays.fill(destroyOperatorWeights, 1.0);
    Arrays.fill(repairOperatorWeights, 1.0);
    Arrays.fill(destroyOperatorSegmentScores, 0.0);
    Arrays.fill(repairOperatorSegmentScores, 0.0);
    Arrays.fill(destroyOperatorSelectionCounts, 0);
    Arrays.fill(repairOperatorSelectionCounts, 0);
    iterationsInSegment = 0;
  }

  /**
   * Returns the configured number of destroy operators.
   *
   * @return the configured number of destroy operators
   */
  public int getDestroyOperatorCount() {
    return destroyOperatorWeights.length;
  }

  /**
   * Returns the configured number of repair operators.
   *
   * @return the configured number of repair operators
   */
  public int getRepairOperatorCount() {
    return repairOperatorWeights.length;
  }

  /**
   * Returns a copy of the scores in BEST, BETTER, ACCEPTED, REJECTED order.
   *
   * @return a copy of the scores in BEST, BETTER, ACCEPTED, REJECTED order
   */
  public double[] getScores() {
    return scores.clone();
  }

  /**
   * Returns the configured weight retention factor.
   *
   * @return the configured weight retention factor
   */
  public double getDecay() {
    return decay;
  }

  /**
   * Returns the number of feedback updates per segment.
   *
   * @return the number of feedback updates per segment
   */
  public long getSegmentLength() {
    return segmentLength;
  }

  /**
   * Returns the number of feedback updates in the incomplete segment.
   *
   * @return the number of feedback updates in the incomplete segment
   */
  public long getIterationsInSegment() {
    return iterationsInSegment;
  }

  /**
   * Returns a copy of destroy weights indexed by operator.
   *
   * @return a copy of destroy weights indexed by operator
   */
  public double[] getDestroyOperatorWeights() {
    return destroyOperatorWeights.clone();
  }

  /**
   * Returns a copy of repair weights indexed by operator.
   *
   * @return a copy of repair weights indexed by operator
   */
  public double[] getRepairOperatorWeights() {
    return repairOperatorWeights.clone();
  }

  /**
   * Returns a copy of accumulated destroy scores in the incomplete segment.
   *
   * @return a copy of accumulated destroy scores in the incomplete segment
   */
  public double[] getDestroyOperatorSegmentScores() {
    return destroyOperatorSegmentScores.clone();
  }

  /**
   * Returns a copy of accumulated repair scores in the incomplete segment.
   *
   * @return a copy of accumulated repair scores in the incomplete segment
   */
  public double[] getRepairOperatorSegmentScores() {
    return repairOperatorSegmentScores.clone();
  }

  private void updateWeights(
      String operatorType, double[] weights, double[] segmentScores, long[] selectionCounts) {
    for (int i = 0; i < weights.length; i++) {
      double oldWeight = weights[i];
      if (selectionCounts[i] > 0) {
        weights[i] = decay * weights[i] + (1 - decay) * (segmentScores[i] / selectionCounts[i]);
      }
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "{} operator {} at segment end: totalScore={}, selectionCount={}, weight {} -> {}",
            operatorType,
            i,
            segmentScores[i],
            selectionCounts[i],
            oldWeight,
            weights[i]);
      }
    }
  }

  private int select(double[] weights, RandomGenerator randomGenerator) {
    double maxWeight = 0.0;
    for (double weight : weights) {
      maxWeight = Math.max(maxWeight, weight);
    }

    if (maxWeight == 0.0) {
      return randomGenerator.nextInt(weights.length);
    }

    double totalWeight = 0.0;
    for (double weight : weights) {
      totalWeight += weight / maxWeight;
    }

    double randomValue = randomGenerator.nextDouble() * totalWeight;
    double cumulativeWeight = 0.0;
    int lastPositiveIndex = 0;
    for (int i = 0; i < weights.length; i++) {
      double scaledWeight = weights[i] / maxWeight;
      if (scaledWeight > 0.0) {
        lastPositiveIndex = i;
      }
      cumulativeWeight += scaledWeight;
      if (randomValue < cumulativeWeight) {
        return i;
      }
    }

    return lastPositiveIndex;
  }
}
