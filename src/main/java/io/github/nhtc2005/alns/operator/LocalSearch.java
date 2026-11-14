package io.github.nhtc2005.alns.operator;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Optionally refines a feasible repaired solution before candidate evaluation.
 *
 * <p>The input is passed directly from repair and may be modified in place or replaced. Improvement
 * is not required. An infeasible result is rejected by the solver.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface LocalSearch<S extends Solution<S>> extends SolutionOperator<S> {}
