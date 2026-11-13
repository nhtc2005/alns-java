package io.github.nhtc2005.alns.memory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HashSetVisitedSolutionTrackerTest {

  @Mock private SolutionKey<Double, TestSolution> mockSolutionKey;

  @Test
  void constructor_ShouldThrowException_WhenSolutionKeyIsNull() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> new HashSetVisitedSolutionTracker<Double, TestSolution>(null));

    assertEquals("solutionKey must not be null.", exception.getMessage());
  }

  @Test
  void recordIfNew_ShouldCountUniqueKeys_WhenSolutionsAreRecordedRepeatedly() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(TestSolution::getObjectiveValue);
    TestSolution solution = new TestSolution(100.0);

    assertEquals(0, tracker.getVisitedSolutionCount());
    assertTrue(tracker.recordIfNew(solution));
    assertEquals(1, tracker.getVisitedSolutionCount());
    assertFalse(tracker.recordIfNew(solution));
    assertFalse(tracker.recordIfNew(solution.copy()));
    assertEquals(1, tracker.getVisitedSolutionCount());
    assertTrue(tracker.recordIfNew(new TestSolution(90.0)));
    assertEquals(2, tracker.getVisitedSolutionCount());
  }

  @ParameterizedTest
  @CsvSource({"1, 2, 2, 1, false", "0, 31, 1, 0, true"})
  void recordIfNew_ShouldDistinguishStructuralKeys_WhenSolutionsHaveEqualObjectiveValues(
      int firstStart, int firstEnd, int secondStart, int secondEnd, boolean hashCollision) {
    HashSetVisitedSolutionTracker<List<Integer>, RouteSolution> tracker =
        new HashSetVisitedSolutionTracker<>(solution -> List.copyOf(solution.route));
    RouteSolution firstSolution = new RouteSolution(List.of(firstStart, firstEnd));
    RouteSolution secondSolution = new RouteSolution(List.of(secondStart, secondEnd));

    assertEquals(firstSolution.getObjectiveValue(), secondSolution.getObjectiveValue());
    assertEquals(hashCollision, firstSolution.route.hashCode() == secondSolution.route.hashCode());
    assertTrue(tracker.recordIfNew(firstSolution));
    assertTrue(tracker.recordIfNew(secondSolution));
    assertFalse(tracker.recordIfNew(firstSolution.copy()));
    assertFalse(tracker.recordIfNew(secondSolution.copy()));
    assertEquals(2, tracker.getVisitedSolutionCount());
  }

  @Test
  void recordIfNew_ShouldPreserveRecordedKeys_WhenSolutionChangesAfterSnapshotIsCreated() {
    HashSetVisitedSolutionTracker<List<Integer>, RouteSolution> tracker =
        new HashSetVisitedSolutionTracker<>(solution -> List.copyOf(solution.route));
    RouteSolution solution = new RouteSolution(List.of(1, 2));

    assertTrue(tracker.recordIfNew(solution));
    solution.route.set(0, 3);

    assertTrue(tracker.recordIfNew(solution));
    assertFalse(tracker.recordIfNew(new RouteSolution(List.of(1, 2))));
    assertFalse(tracker.recordIfNew(solution.copy()));
    assertEquals(2, tracker.getVisitedSolutionCount());
  }

  @Test
  void recordIfNew_ShouldThrowExceptionWithoutExtractingKey_WhenSolutionIsNull() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(mockSolutionKey);
    TestSolution solution = new TestSolution(100.0);
    when(mockSolutionKey.keyOf(solution)).thenReturn(100.0);
    assertTrue(tracker.recordIfNew(solution));

    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> tracker.recordIfNew(null));

    assertEquals("solution must not be null.", exception.getMessage());
    assertEquals(1, tracker.getVisitedSolutionCount());
    verify(mockSolutionKey).keyOf(solution);
    verifyNoMoreInteractions(mockSolutionKey);
  }

  @Test
  void recordIfNew_ShouldThrowExceptionWithoutChangingCount_WhenExtractedKeyIsNull() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(mockSolutionKey);
    TestSolution firstSolution = new TestSolution(100.0);
    TestSolution secondSolution = new TestSolution(90.0);
    when(mockSolutionKey.keyOf(firstSolution)).thenReturn(100.0);
    when(mockSolutionKey.keyOf(secondSolution)).thenReturn(null).thenReturn(90.0);
    assertTrue(tracker.recordIfNew(firstSolution));

    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> tracker.recordIfNew(secondSolution));

    assertEquals("SolutionKey.keyOf(...) must not return null.", exception.getMessage());
    assertEquals(1, tracker.getVisitedSolutionCount());
    assertTrue(tracker.recordIfNew(secondSolution));
    assertEquals(2, tracker.getVisitedSolutionCount());
    verify(mockSolutionKey).keyOf(firstSolution);
    verify(mockSolutionKey, times(2)).keyOf(secondSolution);
    verifyNoMoreInteractions(mockSolutionKey);
  }

  @Test
  void recordIfNew_ShouldPropagateExceptionWithoutChangingCount_WhenKeyExtractionFails() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(mockSolutionKey);
    TestSolution firstSolution = new TestSolution(100.0);
    TestSolution secondSolution = new TestSolution(90.0);
    IllegalStateException exception = new IllegalStateException("Cannot extract key.");
    when(mockSolutionKey.keyOf(firstSolution)).thenReturn(100.0);
    when(mockSolutionKey.keyOf(secondSolution)).thenThrow(exception);
    assertTrue(tracker.recordIfNew(firstSolution));

    assertSame(
        exception,
        assertThrows(IllegalStateException.class, () -> tracker.recordIfNew(secondSolution)));
    assertEquals(1, tracker.getVisitedSolutionCount());
    assertFalse(tracker.recordIfNew(firstSolution));
  }

  @Test
  void reset_ShouldClearVisitedKeys_WhenTrackerIsReused() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(TestSolution::getObjectiveValue);
    TestSolution firstSolution = new TestSolution(100.0);
    TestSolution secondSolution = new TestSolution(90.0);
    assertTrue(tracker.recordIfNew(firstSolution));
    assertTrue(tracker.recordIfNew(secondSolution));

    tracker.reset();

    assertEquals(0, tracker.getVisitedSolutionCount());
    tracker.reset();
    assertEquals(0, tracker.getVisitedSolutionCount());
    assertTrue(tracker.recordIfNew(firstSolution));
    assertTrue(tracker.recordIfNew(secondSolution));
    assertEquals(2, tracker.getVisitedSolutionCount());
  }

  private static final class RouteSolution implements Solution<RouteSolution> {

    private final List<Integer> route;

    private RouteSolution(List<Integer> route) {
      this.route = new ArrayList<>(route);
    }

    @Override
    public RouteSolution copy() {
      return new RouteSolution(route);
    }

    @Override
    public double getObjectiveValue() {
      return route.size();
    }

    @Override
    public boolean isFeasible() {
      return true;
    }
  }
}
