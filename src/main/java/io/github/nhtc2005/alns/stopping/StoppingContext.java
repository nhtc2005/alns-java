package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

/**
 * Provides the search state when checking a stopping criterion.
 *
 * <p>Solutions are shared references, not snapshots, and must not be modified by the criterion.
 *
 * @param <S> the solution type
 * @param iteration the number of completed iterations; zero before the first iteration
 * @param elapsedNanos the elapsed search time in nanoseconds at this check
 * @param bestSolution the best feasible solution found so far
 * @param currentSolution the current feasible solution
 * @param randomGenerator the random generator used by the search
 */
public record StoppingContext<S extends Solution<S>>(
    long iteration,
    long elapsedNanos,
    S bestSolution,
    S currentSolution,
    RandomGenerator randomGenerator) {}
