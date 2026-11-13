package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Stops once the number of completed iterations reaches the configured limit.
 *
 * @param <S> the solution type
 */
public final class MaxIterations<S extends Solution<S>> implements StoppingCriterion<S> {

  private final long maxIterations;

  /**
   * Creates a criterion with a fixed iteration limit.
   *
   * @param maxIterations the positive maximum number of completed iterations
   * @throws IllegalArgumentException if {@code maxIterations} is not positive
   */
  public MaxIterations(long maxIterations) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("maxIterations must be positive.");
    }
    this.maxIterations = maxIterations;
  }

  @Override
  public boolean shouldStop(StoppingContext<S> context) {
    return context.iteration() >= maxIterations;
  }

  /**
   * Returns the configured iteration limit.
   *
   * @return the configured iteration limit
   */
  public long getMaxIterations() {
    return maxIterations;
  }
}
