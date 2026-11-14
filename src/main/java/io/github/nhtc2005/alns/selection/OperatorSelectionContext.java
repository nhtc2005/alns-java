package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

/**
 * Provides the search state for selecting an operator pair.
 *
 * <p>Solutions are shared references, not snapshots, and must not be modified by the selector.
 *
 * @param <S> the solution type
 * @param iteration the one-based index of the iteration being started; zero denotes the initial
 *     solution
 * @param elapsedNanos the elapsed search time in nanoseconds, measured at the start of the
 *     iteration
 * @param bestSolution the best feasible solution found so far
 * @param currentSolution the current feasible solution
 * @param randomGenerator the random generator used by the search
 */
public record OperatorSelectionContext<S extends Solution<S>>(
    long iteration,
    long elapsedNanos,
    S bestSolution,
    S currentSolution,
    RandomGenerator randomGenerator) {}
