package io.github.nhtc2005.alns.stopping;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.util.TestSolution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MaxIterationsTest {

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  void constructor_ShouldThrowException_WhenMaxIterationsIsNonPositive(long maxIterations) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new MaxIterations<TestSolution>(maxIterations));

    assertEquals("maxIterations must be positive.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"99, false", "100, true", "101, true"})
  void shouldStop_ShouldCompareIterationWithLimit_WhenCheckingStoppingCondition(
      long iteration, boolean expected) {
    MaxIterations<TestSolution> criterion = new MaxIterations<>(100);
    StoppingContext<TestSolution> context = new StoppingContext<>(iteration, 0, null, null, null);

    assertEquals(100, criterion.getMaxIterations());
    assertEquals(expected, criterion.shouldStop(context));
  }
}
