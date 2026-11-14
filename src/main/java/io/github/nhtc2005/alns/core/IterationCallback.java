package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Observes completed ALNS iterations synchronously on the search thread.
 *
 * <p>Callbacks run after selector feedback and statistics collection, independently of whether
 * statistics are enabled. Exceptions propagate to the caller of {@link Alns#solve}.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface IterationCallback<S extends Solution<S>> {

  /**
   * Receives the state after a completed iteration.
   *
   * <p>Solutions must not be modified. Copy them before retaining them or using them on another
   * thread. Callback time is included in total search time, but excluded from iteration statistics.
   *
   * @param context the non-null completed iteration context
   */
  void onIteration(IterationContext<S> context);
}
