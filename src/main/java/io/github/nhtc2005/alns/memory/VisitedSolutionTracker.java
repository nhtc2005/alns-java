package io.github.nhtc2005.alns.memory;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface VisitedSolutionTracker<S extends Solution<S>> {

  boolean recordIfNew(S solution);

  default void reset() {}

  static <S extends Solution<S>> VisitedSolutionTracker<S> disabled() {
    return solution -> true;
  }
}
