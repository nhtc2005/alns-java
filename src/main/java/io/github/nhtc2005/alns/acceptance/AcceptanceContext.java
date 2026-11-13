package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

/**
 * Provides the search state for a candidate acceptance decision.
 *
 * <p>Solutions are shared references, not snapshots, and must not be modified by the criterion.
 *
 * @param <S> the solution type
 * @param iteration the zero-based index of the iteration being evaluated
 * @param elapsedNanos the elapsed search time in nanoseconds, measured at the start of the
 *     iteration
 * @param bestSolution the best feasible solution found before evaluating the candidate
 * @param currentSolution the current feasible solution before evaluating the candidate
 * @param candidateSolution the repaired candidate, which may be infeasible
 * @param randomGenerator the random generator used by the search
 */
public record AcceptanceContext<S extends Solution<S>>(
    long iteration,
    long elapsedNanos,
    S bestSolution,
    S currentSolution,
    S candidateSolution,
    RandomGenerator randomGenerator) {}
