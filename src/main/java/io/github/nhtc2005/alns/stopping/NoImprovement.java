package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Objects;

public class NoImprovement<S extends Solution<S>> implements StoppingCriterion<S> {

  private final long maxIterations;

  private S bestSolution;
  private long lastImprovementIteration;

  public NoImprovement(long maxIterations) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("maxIterations must be positive.");
    }
    this.maxIterations = maxIterations;
  }

  @Override
  public boolean shouldStop(StoppingContext<S> context) {
    if (bestSolution == null || context.bestSolution().compareTo(bestSolution) < 0) {
      bestSolution =
          Objects.requireNonNull(
              context.bestSolution().copy(), "Solution.copy() must not return null.");
      lastImprovementIteration = context.iteration();
    }
    return context.iteration() - lastImprovementIteration >= maxIterations;
  }

  @Override
  public void reset() {
    bestSolution = null;
    lastImprovementIteration = 0;
  }

  public long getMaxIterations() {
    return maxIterations;
  }

  public long getLastImprovementIteration() {
    return lastImprovementIteration;
  }
}
