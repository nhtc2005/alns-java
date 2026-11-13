package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Arrays;
import java.util.Objects;
import java.util.random.RandomGenerator;

public class RouletteWheelOperatorSelector<S extends Solution<S>> implements OperatorSelector<S> {

  private final double[] scores;
  private final double decay;
  private final double[] destroyOperatorWeights;
  private final double[] repairOperatorWeights;

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

  @Override
  public OperatorSelection select(OperatorSelectionContext<S> context) {
    return new OperatorSelection(
        select(destroyOperatorWeights, context.randomGenerator()),
        select(repairOperatorWeights, context.randomGenerator()));
  }

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
            : 0.0;

    destroyOperatorWeights[destroyOperatorIndex] =
        decay * destroyOperatorWeights[destroyOperatorIndex] + (1 - decay) * score;
    repairOperatorWeights[repairOperatorIndex] =
        decay * repairOperatorWeights[repairOperatorIndex] + (1 - decay) * score;
  }

  @Override
  public void reset() {
    Arrays.fill(this.destroyOperatorWeights, 1.0);
    Arrays.fill(this.repairOperatorWeights, 1.0);
  }

  public int getDestroyOperatorCount() {
    return destroyOperatorWeights.length;
  }

  public int getRepairOperatorCount() {
    return repairOperatorWeights.length;
  }

  public double[] getScores() {
    return scores.clone();
  }

  public double getDecay() {
    return decay;
  }

  public double[] getDestroyOperatorWeights() {
    return destroyOperatorWeights.clone();
  }

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
