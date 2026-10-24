package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.Solution;

public record AlnsResult<S extends Solution<S>>(
    S bestSolution, long totalIterations, long elapsedNanos) {}
