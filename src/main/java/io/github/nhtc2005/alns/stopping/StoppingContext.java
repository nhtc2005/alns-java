package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

public record StoppingContext<S extends Solution<S>>(
    long iteration,
    long elapsedNanos,
    S bestSolution,
    S currentSolution,
    RandomGenerator randomGenerator) {}
