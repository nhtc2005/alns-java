package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;

public final class MaxIterations<S extends Solution<S>> implements StoppingCriterion<S> {

  private final long maxIterations;

  public MaxIterations(long maxIterations) {
    this.maxIterations = maxIterations;
  }

  @Override
  public boolean shouldStop(StoppingContext<S> context) {
    return context.iteration() >= maxIterations;
  }
}
