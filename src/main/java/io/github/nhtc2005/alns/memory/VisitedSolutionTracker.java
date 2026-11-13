package io.github.nhtc2005.alns.memory;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Tracks solution visits to support reward eligibility during an ALNS search.
 *
 * <p>The solver records the initial solution and accepted candidates. Tracking does not prevent
 * acceptance of repeated solutions.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface VisitedSolutionTracker<S extends Solution<S>> {

  /**
   * Records a solution without modifying it and reports whether it is new to this tracker.
   *
   * @param solution the non-null solution to record
   * @return {@code true} if newly recorded; {@code false} if already recorded
   */
  boolean recordIfNew(S solution);

  /**
   * Clears visit history before a solve begins.
   *
   * <p>The default implementation does nothing.
   */
  default void reset() {}

  /**
   * Returns a stateless tracker that stores nothing and treats every solution as new.
   *
   * @param <S> the solution type
   * @return a tracker that always returns {@code true} from {@code recordIfNew}
   */
  static <S extends Solution<S>> VisitedSolutionTracker<S> disabled() {
    return solution -> true;
  }
}
