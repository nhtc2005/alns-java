package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Objects;

/**
 * Contains the best solution and measurements from a completed ALNS search.
 *
 * <p>The best solution is returned directly; this record does not defensively copy it.
 *
 * @param <S> the solution type
 * @param bestSolution the best feasible solution found by the solver
 * @param totalIterations the number of completed iterations
 * @param elapsedNanos the search duration in nanoseconds, excluding component resets
 * @param statistics the collected statistics, or a disabled statistics object
 */
public record AlnsResult<S extends Solution<S>>(
    S bestSolution, long totalIterations, long elapsedNanos, AlnsStatistics statistics) {

  /**
   * Creates a result with the supplied statistics.
   *
   * @param bestSolution the best feasible solution found by the solver
   * @param totalIterations the number of completed iterations
   * @param elapsedNanos the search duration in nanoseconds, excluding component resets
   * @param statistics the collected statistics, or a disabled statistics object
   * @throws NullPointerException if {@code statistics} is null
   */
  public AlnsResult {
    Objects.requireNonNull(statistics, "statistics must not be null.");
  }

  /**
   * Creates a result with statistics disabled.
   *
   * @param bestSolution the best solution
   * @param totalIterations the number of completed iterations
   * @param elapsedNanos the search duration in nanoseconds
   */
  public AlnsResult(S bestSolution, long totalIterations, long elapsedNanos) {
    this(bestSolution, totalIterations, elapsedNanos, AlnsStatistics.disabled());
  }
}
