package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops after a configured number of iterations without a strictly better best objective.
 *
 * <p>Only global-best improvements restart the limit; equal objectives and changes to the current
 * solution do not. The first check establishes the baseline. This criterion retains a solution copy
 * and must not be shared by concurrent runs.
 *
 * @param <S> the solution type
 */
public class NoImprovement<S extends Solution<S>> implements StoppingCriterion<S> {

  private static final Logger LOGGER = LoggerFactory.getLogger(NoImprovement.class);

  private final long maxIterations;

  private S bestSolution;
  private long lastImprovementIteration;

  /**
   * Creates a criterion with a limit on iterations without improvement.
   *
   * @param maxIterations the positive number of completed iterations allowed without improvement
   * @throws IllegalArgumentException if {@code maxIterations} is not positive
   */
  public NoImprovement(long maxIterations) {
    if (maxIterations <= 0) {
      throw new IllegalArgumentException("maxIterations must be positive.");
    }
    this.maxIterations = maxIterations;
  }

  @Override
  public boolean shouldStop(StoppingContext<S> context) {
    if (bestSolution == null
        || context.bestSolution().getObjectiveValue() < bestSolution.getObjectiveValue()) {
      bestSolution =
          Objects.requireNonNull(
              context.bestSolution().copy(), "Solution.copy() must not return null.");
      lastImprovementIteration = context.iteration();
    }
    long iterationsWithoutImprovement = context.iteration() - lastImprovementIteration;
    boolean shouldStop = iterationsWithoutImprovement >= maxIterations;
    if (shouldStop) {
      LOGGER.debug(
          "No-improvement limit reached: iterationsWithoutImprovement={}, limit={}",
          iterationsWithoutImprovement,
          maxIterations);
    }
    return shouldStop;
  }

  /** Clears the saved best solution and resets the last improvement iteration to zero. */
  @Override
  public void reset() {
    bestSolution = null;
    lastImprovementIteration = 0;
  }

  /**
   * Returns the configured iteration limit without improvement.
   *
   * @return the configured iteration limit without improvement
   */
  public long getMaxIterations() {
    return maxIterations;
  }

  /**
   * Returns the completed iteration count at the last observed improvement or baseline; zero after
   * reset.
   *
   * @return the completed iteration count at the last observed improvement or baseline; zero after
   *     reset
   */
  public long getLastImprovementIteration() {
    return lastImprovementIteration;
  }
}
