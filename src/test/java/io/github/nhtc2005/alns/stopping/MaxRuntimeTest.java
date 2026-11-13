package io.github.nhtc2005.alns.stopping;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.util.TestSolution;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MaxRuntimeTest {

  @Test
  void constructor_ShouldThrowException_WhenMaxRuntimeIsNull() {
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> new MaxRuntime<TestSolution>(null));

    assertEquals("maxRuntime must not be null.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void constructor_ShouldThrowException_WhenMaxRuntimeIsNonPositive(long maxRuntime) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new MaxRuntime<TestSolution>(Duration.ofNanos(maxRuntime)));

    assertEquals("maxRuntime must be positive.", exception.getMessage());
  }

  @Test
  void constructor_ShouldThrowException_WhenMaxRuntimeCannotBeRepresentedInNanoseconds() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new MaxRuntime<TestSolution>(Duration.ofSeconds(Long.MAX_VALUE)));

    assertEquals("maxRuntime is too large to represent in nanoseconds.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"99, false", "100, true", "101, true"})
  void shouldStop_ShouldCompareElapsedTimeWithLimit_WhenCheckingStoppingCondition(
      long elapsedNanos, boolean expected) {
    MaxRuntime<TestSolution> criterion = new MaxRuntime<>(Duration.ofNanos(100));
    StoppingContext<TestSolution> context =
        new StoppingContext<>(0, elapsedNanos, null, null, null);

    assertEquals(Duration.ofNanos(100), criterion.getMaxRuntime());
    assertEquals(expected, criterion.shouldStop(context));
  }
}
