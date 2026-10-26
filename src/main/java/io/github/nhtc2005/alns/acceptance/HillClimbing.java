package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;

public final class HillClimbing<S extends Solution<S>> implements AcceptanceCriterion<S> {

  @Override
  public boolean shouldAccept(AcceptanceContext<S> context) {
    return context.candidateSolution().getObjectiveValue()
        < context.currentSolution().getObjectiveValue();
  }
}
