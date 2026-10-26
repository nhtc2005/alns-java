package io.github.nhtc2005.alns.memory;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface SolutionKey<K, S extends Solution<S>> {

  K keyOf(S solution);
}
