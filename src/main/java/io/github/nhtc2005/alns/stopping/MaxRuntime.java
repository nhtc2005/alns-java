package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.time.Duration;
import java.util.Objects;

/**
 * Stops when elapsed search time reaches the configured duration.
 *
 * <p>The limit is checked between iterations and does not interrupt a running operator, so actual
 * runtime may exceed it.
 *
 * @param <S> the solution type
 */
public class MaxRuntime<S extends Solution<S>> implements StoppingCriterion<S> {

  private final long maxRuntimeNanos;

  /**
   * Creates a criterion with a fixed runtime limit.
   *
   * @param maxRuntime the positive maximum search duration, representable in nanoseconds
   * @throws NullPointerException if {@code maxRuntime} is null
   * @throws IllegalArgumentException if the duration is not positive or overflows nanoseconds
   */
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

  /**
   * Returns the configured runtime limit.
   *
   * @return the configured runtime limit
   */
  public Duration getMaxRuntime() {
    return Duration.ofNanos(maxRuntimeNanos);
  }
}
