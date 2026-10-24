package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface OperatorSelector<S extends Solution<S>> {

  OperatorSelection select(OperatorSelectionContext<S> context);

  default void update(OperatorSelectionFeedback<S> feedback) {}

  default void reset() {}
}
