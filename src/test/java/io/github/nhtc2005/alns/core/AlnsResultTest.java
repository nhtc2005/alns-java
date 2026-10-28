package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlnsResultTest {

  @Test
  void constructor_ShouldProvideDisabledStatistics_WhenUsingThreeArguments() {
    TestSolution bestSolution = new TestSolution(80.0);

    AlnsResult<TestSolution> result = new AlnsResult<>(bestSolution, 3, 100);

    assertSame(bestSolution, result.bestSolution());
    assertEquals(3, result.totalIterations());
    assertEquals(100, result.elapsedNanos());
    assertFalse(result.statistics().isEnabled());
    assertTrue(result.statistics().getCurrentObjectiveValues().isEmpty());
    assertEquals(0L, result.statistics().getStatusCount(SolutionStatus.BEST));
  }

  @Test
  void constructor_ShouldExposeCollectedStatistics_WhenStatisticsAreProvided() {
    TestSolution bestSolution = new TestSolution(80.0);
    AlnsStatistics statistics = new AlnsStatistics(1, 1);
    statistics.collectInitial(80.0, 80.0);

    AlnsResult<TestSolution> result = new AlnsResult<>(bestSolution, 0, 100, statistics);

    assertSame(bestSolution, result.bestSolution());
    assertEquals(0, result.totalIterations());
    assertEquals(100, result.elapsedNanos());
    assertSame(statistics, result.statistics());
    assertTrue(result.statistics().isEnabled());
    assertEquals(List.of(80.0), result.statistics().getBestObjectiveValues());
  }

  @Test
  void constructor_ShouldThrowException_WhenStatisticsAreNull() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> new AlnsResult<>(new TestSolution(80.0), 0, 100, null));

    assertEquals("statistics must not be null.", exception.getMessage());
  }
}
