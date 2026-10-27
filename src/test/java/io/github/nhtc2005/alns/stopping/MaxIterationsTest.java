package io.github.nhtc2005.alns.stopping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.github.nhtc2005.alns.util.TestSolution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaxIterationsTest {

  @Mock private StoppingContext<TestSolution> mockContext;

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  void constructor_ShouldThrowException_WhenMaxIterationsIsNonPositive(long maxIterations) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new MaxIterations<TestSolution>(maxIterations));

    assertEquals("maxIterations must be positive.", exception.getMessage());
  }

  @Test
  void shouldStop_ShouldReturnFalse_WhenIterationIsLessThanMax() {
    MaxIterations<TestSolution> criterion = new MaxIterations<>(100);
    when(mockContext.iteration()).thenReturn(99L);

    assertFalse(criterion.shouldStop(mockContext));
  }

  @Test
  void shouldStop_ShouldReturnTrue_WhenIterationEqualsMax() {
    MaxIterations<TestSolution> criterion = new MaxIterations<>(100);
    when(mockContext.iteration()).thenReturn(100L);

    assertTrue(criterion.shouldStop(mockContext));
  }

  @Test
  void shouldStop_ShouldReturnTrue_WhenIterationExceedsMax() {
    MaxIterations<TestSolution> criterion = new MaxIterations<>(100);
    when(mockContext.iteration()).thenReturn(101L);

    assertTrue(criterion.shouldStop(mockContext));
  }
}
