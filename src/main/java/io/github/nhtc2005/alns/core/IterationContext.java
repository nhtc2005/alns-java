package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.selection.OperatorSelection;

/**
 * Provides the search state after a completed ALNS iteration.
 *
 * <p>Solutions are shared references, not snapshots, and must not be modified by the callback.
 *
 * @param <S> the solution type
 * @param iteration the number of completed iterations; starts at one for each solve
 * @param elapsedNanos the elapsed search time in nanoseconds before this callback
 * @param status the candidate outcome
 * @param isRewardEligible whether the configured outcome score may be applied
 * @param operatorSelection the operator pair used in this iteration
 * @param candidateSolution the candidate after repair and optional local search, even if rejected
 * @param currentSolution the current feasible solution after evaluating the candidate
 * @param bestSolution the best feasible solution found so far
 */
public record IterationContext<S extends Solution<S>>(
    long iteration,
    long elapsedNanos,
    SolutionStatus status,
    boolean isRewardEligible,
    OperatorSelection operatorSelection,
    S candidateSolution,
    S currentSolution,
    S bestSolution) {}
