package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Decides whether to accept a candidate solution during an ALNS iteration.
 *
 * <p>The solver always rejects infeasible candidates and accepts feasible candidates that improve
 * the global best, regardless of this criterion's decision.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface AcceptanceCriterion<S extends Solution<S>> {

  /**
   * Evaluates acceptance, once per iteration, including for infeasible candidates and new bests.
   *
   * <p>Implementations may update their own state but must not modify the solutions in the context.
   *
   * @param context the non-null acceptance context
   * @return {@code true} to accept the candidate, subject to the solver's feasibility checks
   */
  boolean shouldAccept(AcceptanceContext<S> context);

  /**
   * Restores this criterion's initial state before a solve begins.
   *
   * <p>The default implementation does nothing.
   */
  default void reset() {}
}
