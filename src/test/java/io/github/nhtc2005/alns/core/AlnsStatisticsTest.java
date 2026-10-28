package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class AlnsStatisticsTest {

  @ParameterizedTest
  @EnumSource(SolutionStatus.class)
  void collectIteration_ShouldTrackHistoriesAndIndependentCounts_WhenFeedbackIsCollected(
      SolutionStatus status) {
    AlnsStatistics statistics = new AlnsStatistics(2, 3);
    statistics.collectInitial(100.0, 100.0);
    statistics.collectIteration(new OperatorSelection(1, 0), status, 120.0, 100.0, 5);
    statistics.collectIteration(new OperatorSelection(0, 2), status, 110.0, 100.0, 7);
    statistics.collectIteration(new OperatorSelection(1, 2), status, 80.0, 80.0, 9);

    assertTrue(statistics.isEnabled());
    assertEquals(List.of(100.0, 120.0, 110.0, 80.0), statistics.getCurrentObjectiveValues());
    assertEquals(List.of(100.0, 100.0, 100.0, 80.0), statistics.getBestObjectiveValues());
    assertEquals(List.of(5L, 7L, 9L), statistics.getIterationRuntimesNanos());
    for (SolutionStatus outcome : SolutionStatus.values()) {
      assertEquals(outcome == status ? 3L : 0L, statistics.getStatusCount(outcome));
      assertEquals(outcome == status ? 1L : 0L, statistics.getDestroyOperatorCount(0, outcome));
      assertEquals(outcome == status ? 2L : 0L, statistics.getDestroyOperatorCount(1, outcome));
      assertEquals(outcome == status ? 1L : 0L, statistics.getRepairOperatorCount(0, outcome));
      assertEquals(0L, statistics.getRepairOperatorCount(1, outcome));
      assertEquals(outcome == status ? 2L : 0L, statistics.getRepairOperatorCount(2, outcome));
    }
  }

  @Test
  void collectIteration_ShouldSeparateOutcomeCounts_WhenDifferentOutcomesAreCollected() {
    AlnsStatistics statistics = new AlnsStatistics(2, 2);
    statistics.collectInitial(100.0, 100.0);
    statistics.collectIteration(new OperatorSelection(0, 1), SolutionStatus.BEST, 80.0, 80.0, 1);
    statistics.collectIteration(
        new OperatorSelection(1, 0), SolutionStatus.REJECTED, 80.0, 80.0, 2);
    statistics.collectIteration(new OperatorSelection(0, 1), SolutionStatus.BETTER, 85.0, 80.0, 3);

    assertEquals(1L, statistics.getStatusCount(SolutionStatus.BEST));
    assertEquals(1L, statistics.getStatusCount(SolutionStatus.BETTER));
    assertEquals(1L, statistics.getStatusCount(SolutionStatus.REJECTED));
    assertEquals(0L, statistics.getStatusCount(SolutionStatus.ACCEPTED));
    assertEquals(1L, statistics.getDestroyOperatorCount(0, SolutionStatus.BEST));
    assertEquals(1L, statistics.getDestroyOperatorCount(0, SolutionStatus.BETTER));
    assertEquals(0L, statistics.getDestroyOperatorCount(0, SolutionStatus.REJECTED));
    assertEquals(1L, statistics.getDestroyOperatorCount(1, SolutionStatus.REJECTED));
    assertEquals(1L, statistics.getRepairOperatorCount(1, SolutionStatus.BEST));
    assertEquals(1L, statistics.getRepairOperatorCount(0, SolutionStatus.REJECTED));
  }

  @Test
  void getHistories_ShouldReturnImmutableSnapshots_WhenCollectionContinues() {
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
    assertEquals(List.of(5L), iterationRuntimesNanos);
    assertEquals(List.of(100.0, 80.0, 90.0), statistics.getCurrentObjectiveValues());
    assertEquals(List.of(100.0, 80.0, 80.0), statistics.getBestObjectiveValues());
    assertEquals(List.of(5L, 7L), statistics.getIterationRuntimesNanos());
  }

  @Test
  void collectIteration_ShouldKeepStatisticsEmpty_WhenCollectionIsDisabled() {
    AlnsStatistics statistics = AlnsStatistics.disabled();
    statistics.collectInitial(100.0, 100.0);
    statistics.collectIteration(new OperatorSelection(0, 0), SolutionStatus.BEST, 80.0, 80.0, 5);

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
  void getOperatorCount_ShouldThrowException_WhenOperatorIndexIsInvalid(
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
  void getCount_ShouldThrowException_WhenStatusIsNull() {
    AlnsStatistics statistics = new AlnsStatistics(1, 1);

    assertThrows(NullPointerException.class, () -> statistics.getStatusCount(null));
    assertThrows(NullPointerException.class, () -> statistics.getDestroyOperatorCount(0, null));
    assertThrows(NullPointerException.class, () -> statistics.getRepairOperatorCount(0, null));
  }
}
