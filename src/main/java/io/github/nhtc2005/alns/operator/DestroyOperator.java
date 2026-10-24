package io.github.nhtc2005.alns.operator;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface DestroyOperator<S extends Solution<S>> extends SolutionOperator<S> {}
