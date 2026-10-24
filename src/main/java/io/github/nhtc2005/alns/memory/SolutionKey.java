package io.github.nhtc2005.alns.memory;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface SolutionKey<S extends Solution<S>, K> {

  K keyOf(S solution);
}
