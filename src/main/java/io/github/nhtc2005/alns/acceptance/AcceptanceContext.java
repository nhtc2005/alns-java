package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

public record AcceptanceContext<S extends Solution<S>>(
    long iteration,
    long elapsedNanos,
    S bestSolution,
    S currentSolution,
    S candidateSolution,
    RandomGenerator randomGenerator) {}
