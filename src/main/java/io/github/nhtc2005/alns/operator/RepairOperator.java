package io.github.nhtc2005.alns.operator;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Repairs a destroyed solution to produce a candidate for an ALNS iteration.
 *
 * <p>Repair need not succeed: candidates that remain infeasible are rejected by the solver.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface RepairOperator<S extends Solution<S>> extends SolutionOperator<S> {}
