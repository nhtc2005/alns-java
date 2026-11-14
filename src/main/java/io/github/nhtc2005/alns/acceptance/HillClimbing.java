package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Accepts only candidates whose objective is not worse than the current solution's objective.
 *
 * <p>Acceptance requires \(f(s_{\mathrm{candidate}}) \le f(s_{\mathrm{current}})\); candidates with
 * equal objective values are accepted. This criterion is stateless.
 *
 * @param <S> the solution type
 */
public final class HillClimbing<S extends Solution<S>> implements AcceptanceCriterion<S> {

  /** Creates a stateless hill-climbing criterion. */
  public HillClimbing() {}

  @Override
  public boolean shouldAccept(AcceptanceContext<S> context) {
    return context.candidateSolution().getObjectiveValue()
        <= context.currentSolution().getObjectiveValue();
  }
}
