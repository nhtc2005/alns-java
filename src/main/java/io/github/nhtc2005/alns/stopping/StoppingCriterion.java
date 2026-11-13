package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Decides whether an ALNS search should stop before the next iteration.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface StoppingCriterion<S extends Solution<S>> {

  /**
   * Checks whether the search should stop, including before the first iteration.
   *
   * <p>Implementations may update their own state but must not modify solutions in the context.
   *
   * @param context the non-null search context
   * @return {@code true} to stop; {@code false} to begin another iteration
   */
  boolean shouldStop(StoppingContext<S> context);

  /**
   * Restores initial state before a solve begins.
   *
   * <p>The default implementation does nothing.
   */
  default void reset() {}
}
