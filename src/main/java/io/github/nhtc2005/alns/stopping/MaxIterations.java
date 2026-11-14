package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops once the number of completed iterations reaches the configured limit.
 *
 * @param <S> the solution type
 */
public final class MaxIterations<S extends Solution<S>> implements StoppingCriterion<S> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MaxIterations.class);

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
    long iteration = context.iteration();
    boolean shouldStop = iteration >= maxIterations;
    if (shouldStop) {
      LOGGER.debug("Iteration limit reached: iterations={}, limit={}", iteration, maxIterations);
    }
    return shouldStop;
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
