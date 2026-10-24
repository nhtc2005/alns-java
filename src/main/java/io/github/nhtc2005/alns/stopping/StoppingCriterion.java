package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface StoppingCriterion<S extends Solution<S>> {

  boolean shouldStop(StoppingContext<S> context);

  default void reset() {}
}
