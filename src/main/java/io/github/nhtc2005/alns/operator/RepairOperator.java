package io.github.nhtc2005.alns.operator;

import io.github.nhtc2005.alns.model.Solution;

@FunctionalInterface
public interface RepairOperator<S extends Solution<S>> extends SolutionOperator<S> {}
