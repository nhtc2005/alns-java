package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AlnsStatisticsTest {

  @Test
  void
      collectIteration_ShouldTrackHistoriesAndCountsPerStatusAndOperator_WhenFeedbackIsCollected() {
    AlnsStatistics statistics = new AlnsStatistics(2, 3);
    statistics.collectInitial(100.0, 100.0);
    statistics.collectIteration(new OperatorSelection(1, 0), SolutionStatus.BEST, 80.0, 80.0, 5);
    statistics.collectIteration(
        new OperatorSelection(0, 2), SolutionStatus.REJECTED, 80.0, 80.0, 7);
    statistics.collectIteration(new OperatorSelection(1, 2), SolutionStatus.BETTER, 85.0, 80.0, 9);
    statistics.collectIteration(
        new OperatorSelection(0, 0), SolutionStatus.ACCEPTED, 90.0, 80.0, 11);
    statistics.collectIteration(new OperatorSelection(1, 2), SolutionStatus.BEST, 70.0, 70.0, 13);

    assertTrue(statistics.isEnabled());
    assertEquals(
        List.of(100.0, 80.0, 80.0, 85.0, 90.0, 70.0), statistics.getCurrentObjectiveValues());
    assertEquals(List.of(100.0, 80.0, 80.0, 80.0, 80.0, 70.0), statistics.getBestObjectiveValues());
    assertEquals(List.of(0L, 5L, 7L, 9L, 11L, 13L), statistics.getIterationRuntimesNanos());

    // Columns follow SolutionStatus order: BEST, BETTER, ACCEPTED, REJECTED.
    long[] expectedStatusCounts = {2, 1, 1, 1};
    long[][] expectedDestroyOperatorCounts = {{0, 0, 1, 1}, {2, 1, 0, 0}};
    long[][] expectedRepairOperatorCounts = {{1, 0, 1, 0}, {0, 0, 0, 0}, {1, 1, 0, 1}};
    for (SolutionStatus status : SolutionStatus.values()) {
      int column = status.ordinal();
      assertEquals(expectedStatusCounts[column], statistics.getStatusCount(status));
      for (int index = 0; index < expectedDestroyOperatorCounts.length; index++) {
        assertEquals(
            expectedDestroyOperatorCounts[index][column],
            statistics.getDestroyOperatorCount(index, status));
      }
      for (int index = 0; index < expectedRepairOperatorCounts.length; index++) {
        assertEquals(
            expectedRepairOperatorCounts[index][column],
            statistics.getRepairOperatorCount(index, status));
      }
    }
  }

  @Test
  void getObjectiveValuesAndRuntimes_ShouldReturnImmutableSnapshots_WhenCollectionContinues() {
    AlnsStatistics statistics = new AlnsStatistics(1, 1);
    statistics.collectInitial(100.0, 100.0);
    statistics.collectIteration(new OperatorSelection(0, 0), SolutionStatus.BEST, 80.0, 80.0, 5);

    List<Double> currentObjectiveValues = statistics.getCurrentObjectiveValues();
    List<Double> bestObjectiveValues = statistics.getBestObjectiveValues();
    List<Long> iterationRuntimesNanos = statistics.getIterationRuntimesNanos();
    assertThrows(UnsupportedOperationException.class, currentObjectiveValues::clear);
    assertThrows(UnsupportedOperationException.class, bestObjectiveValues::clear);
    assertThrows(UnsupportedOperationException.class, iterationRuntimesNanos::clear);

    statistics.collectIteration(
        new OperatorSelection(0, 0), SolutionStatus.ACCEPTED, 90.0, 80.0, 7);

    assertEquals(List.of(100.0, 80.0), currentObjectiveValues);
    assertEquals(List.of(100.0, 80.0), bestObjectiveValues);
    assertEquals(List.of(0L, 5L), iterationRuntimesNanos);
    assertEquals(List.of(100.0, 80.0, 90.0), statistics.getCurrentObjectiveValues());
    assertEquals(List.of(100.0, 80.0, 80.0), statistics.getBestObjectiveValues());
    assertEquals(List.of(0L, 5L, 7L), statistics.getIterationRuntimesNanos());
  }

  @Test
  void disabled_ShouldExposeEmptyStatistics_WhenCollectionIsDisabled() {
    AlnsStatistics statistics = AlnsStatistics.disabled();

    assertFalse(statistics.isEnabled());
    assertTrue(statistics.getCurrentObjectiveValues().isEmpty());
    assertTrue(statistics.getBestObjectiveValues().isEmpty());
    assertTrue(statistics.getIterationRuntimesNanos().isEmpty());
    for (SolutionStatus status : SolutionStatus.values()) {
      assertEquals(0L, statistics.getStatusCount(status));
    }
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> statistics.getDestroyOperatorCount(0, SolutionStatus.BEST));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> statistics.getRepairOperatorCount(0, SolutionStatus.BEST));
  }

  @ParameterizedTest
  @CsvSource({"-1, -1", "2, 3"})
  void getDestroyAndRepairOperatorCount_ShouldThrowException_WhenOperatorIndexIsInvalid(
      int destroyOperatorIndex, int repairOperatorIndex) {
    AlnsStatistics statistics = new AlnsStatistics(2, 3);

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> statistics.getDestroyOperatorCount(destroyOperatorIndex, SolutionStatus.BEST));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> statistics.getRepairOperatorCount(repairOperatorIndex, SolutionStatus.BEST));
  }

  @Test
  void getStatusAndOperatorCount_ShouldThrowException_WhenStatusIsNull() {
    AlnsStatistics statistics = new AlnsStatistics(1, 1);

    NullPointerException statusException =
        assertThrows(NullPointerException.class, () -> statistics.getStatusCount(null));

    assertEquals("status must not be null.", statusException.getMessage());

    NullPointerException destroyException =
        assertThrows(NullPointerException.class, () -> statistics.getDestroyOperatorCount(0, null));

    assertEquals("status must not be null.", destroyException.getMessage());

    NullPointerException repairException =
        assertThrows(NullPointerException.class, () -> statistics.getRepairOperatorCount(0, null));

    assertEquals("status must not be null.", repairException.getMessage());
  }
}
