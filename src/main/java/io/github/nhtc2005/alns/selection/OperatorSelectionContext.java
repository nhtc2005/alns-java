package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

public record OperatorSelectionContext<S extends Solution<S>>(
    long iteration,
    long elapsedNanos,
    S bestSolution,
    S currentSolution,
    RandomGenerator randomGenerator) {}
