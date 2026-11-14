package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops when elapsed search time reaches the configured duration.
 *
 * <p>The limit is checked between iterations and does not interrupt a running operator, so actual
 * runtime may exceed it.
 *
 * @param <S> the solution type
 */
public final class MaxRuntime<S extends Solution<S>> implements StoppingCriterion<S> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MaxRuntime.class);

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
    long elapsedNanos = context.elapsedNanos();
    boolean shouldStop = elapsedNanos >= maxRuntimeNanos;
    if (shouldStop) {
      LOGGER.debug(
          "Runtime limit reached: elapsedNanos={}, limitNanos={}", elapsedNanos, maxRuntimeNanos);
    }
    return shouldStop;
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
