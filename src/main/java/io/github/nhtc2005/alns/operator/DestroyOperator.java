package io.github.nhtc2005.alns.operator;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Removes or disrupts part of a solution before repair in an ALNS iteration.
 *
 * <p>The solver supplies a copy of the current solution. The result may be incomplete or infeasible
 * and is passed to the selected {@link RepairOperator}.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface DestroyOperator<S extends Solution<S>> extends SolutionOperator<S> {}
