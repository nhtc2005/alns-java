package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects destroy and repair operators independently in proportion to their weights.
 *
 * <p>For each operator group, \(P(i) = w_i / \sum_j w_j\). All weights start at 1; if all weights
 * in a group are zero, selection is uniform. Feedback updates only the selected weights:
 *
 * <p>\[ w_i' = \theta w_i + (1 - \theta) s. \]
 *
 * <p>Here \(\theta\) is {@code decay}, and \(s\) is the outcome score, or the rejected-outcome
 * score {@code scores[3]} when reward is ineligible. Instances hold mutable weights and must not be
 * shared by concurrent runs.
 *
 * @param <S> the solution type
 */
public class RouletteWheelOperatorSelector<S extends Solution<S>> implements OperatorSelector<S> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouletteWheelOperatorSelector.class);

  private final double[] scores;
  private final double decay;
  private final double[] destroyOperatorWeights;
  private final double[] repairOperatorWeights;

  /**
   * Creates a selector for fixed operator counts.
   *
   * @param destroyOperatorCount the positive number of destroy operators
   * @param repairOperatorCount the positive number of repair operators
   * @param scores four finite non-negative scores in BEST, BETTER, ACCEPTED, REJECTED order; copied
   *     on construction
   * @param decay the finite weight retention factor in {@code [0, 1]}
   * @throws IllegalArgumentException if any numeric parameter or score count is invalid
   * @throws NullPointerException if {@code scores} is null
   */
  public RouletteWheelOperatorSelector(
      int destroyOperatorCount, int repairOperatorCount, double[] scores, double decay) {
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

    this.decay = decay;
    this.destroyOperatorWeights = new double[destroyOperatorCount];
    this.repairOperatorWeights = new double[repairOperatorCount];
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

    double oldDestroyWeight = destroyOperatorWeights[destroyOperatorIndex];
    double oldRepairWeight = repairOperatorWeights[repairOperatorIndex];
    destroyOperatorWeights[destroyOperatorIndex] =
        decay * destroyOperatorWeights[destroyOperatorIndex] + (1 - decay) * score;
    repairOperatorWeights[repairOperatorIndex] =
        decay * repairOperatorWeights[repairOperatorIndex] + (1 - decay) * score;
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "Destroy operator {}: score={}, weight {} -> {}",
          destroyOperatorIndex,
          score,
          oldDestroyWeight,
          destroyOperatorWeights[destroyOperatorIndex]);
      LOGGER.trace(
          "Repair operator {}: score={}, weight {} -> {}",
          repairOperatorIndex,
          score,
          oldRepairWeight,
          repairOperatorWeights[repairOperatorIndex]);
    }
  }

  /** Restores all operator weights to 1. */
  @Override
  public void reset() {
    Arrays.fill(this.destroyOperatorWeights, 1.0);
    Arrays.fill(this.repairOperatorWeights, 1.0);
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
