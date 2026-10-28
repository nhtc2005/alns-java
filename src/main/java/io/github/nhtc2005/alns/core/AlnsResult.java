package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Objects;

public record AlnsResult<S extends Solution<S>>(
    S bestSolution, long totalIterations, long elapsedNanos, AlnsStatistics statistics) {

  public AlnsResult {
    Objects.requireNonNull(statistics, "statistics must not be null.");
  }

  public AlnsResult(S bestSolution, long totalIterations, long elapsedNanos) {
    this(bestSolution, totalIterations, elapsedNanos, AlnsStatistics.disabled());
  }
}
