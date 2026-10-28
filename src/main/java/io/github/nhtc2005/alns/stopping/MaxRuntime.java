package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.time.Duration;
import java.util.Objects;

public class MaxRuntime<S extends Solution<S>> implements StoppingCriterion<S> {

  private final long maxRuntimeNanos;

  public MaxRuntime(Duration maxRuntime) {
    Objects.requireNonNull(maxRuntime, "maxRuntime must not be null.");

    if (maxRuntime.isZero() || maxRuntime.isNegative()) {
      throw new IllegalArgumentException("maxRuntime must be positive.");
    }

    try {
      this.maxRuntimeNanos = maxRuntime.toNanos();
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(
          "maxRuntime is too large to represent in nanoseconds.", exception);
    }
  }

  @Override
  public boolean shouldStop(StoppingContext<S> context) {
    return context.elapsedNanos() >= maxRuntimeNanos;
  }

  public Duration getMaxRuntime() {
    return Duration.ofNanos(maxRuntimeNanos);
  }
}
