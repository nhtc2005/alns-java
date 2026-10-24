package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface AcceptanceCriterion<S extends Solution<S>> {

  boolean shouldAccept(AcceptanceContext<S> context);

  default void reset() {}
}
