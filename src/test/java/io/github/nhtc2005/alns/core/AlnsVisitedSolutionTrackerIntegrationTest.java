package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.acceptance.AcceptanceCriterion;
import io.github.nhtc2005.alns.acceptance.SimulatedAnnealing;
import io.github.nhtc2005.alns.memory.HashSetVisitedSolutionTracker;
import io.github.nhtc2005.alns.memory.VisitedSolutionTracker;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.operator.DestroyOperator;
import io.github.nhtc2005.alns.operator.RepairOperator;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import io.github.nhtc2005.alns.selection.OperatorSelectionFeedback;
import io.github.nhtc2005.alns.selection.OperatorSelector;
import io.github.nhtc2005.alns.stopping.MaxIterations;
import io.github.nhtc2005.alns.stopping.StoppingCriterion;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlnsVisitedSolutionTrackerIntegrationTest {

  @Mock private DestroyOperator<TestSolution> mockDestroyOperator;
  @Mock private RepairOperator<TestSolution> mockRepairOperator;
  @Mock private OperatorSelector<TestSolution> mockOperatorSelector;
  @Mock private AcceptanceCriterion<TestSolution> mockAcceptanceCriterion;
  @Mock private StoppingCriterion<TestSolution> mockStoppingCriterion;
  @Mock private VisitedSolutionTracker<TestSolution> mockVisitedSolutionTracker;

  @Test
  void builder_ShouldThrowException_WhenVisitedSolutionTrackerIsNull() {
    Alns.Builder<TestSolution> builder = Alns.builder();

    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> builder.visitedSolutionTracker(null));

    assertEquals("visitedSolutionTracker must not be null.", exception.getMessage());
  }

  @Test
  void solve_ShouldResetTrackerAndRecordInitialCopy_WhenStoppingBeforeFirstIteration() {
    TestSolution initialSolution = new TestSolution(100.0);
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator(mockDestroyOperator)
            .addRepairOperator(mockRepairOperator)
            .operatorSelector(mockOperatorSelector)
            .visitedSolutionTracker(mockVisitedSolutionTracker)
            .build();

    when(mockStoppingCriterion.shouldStop(any())).thenReturn(true);

    AlnsResult<TestSolution> result =
        alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    ArgumentCaptor<TestSolution> solutionCaptor = ArgumentCaptor.forClass(TestSolution.class);
    InOrder order = inOrder(mockVisitedSolutionTracker, mockStoppingCriterion);
    order.verify(mockVisitedSolutionTracker).reset();
    order.verify(mockVisitedSolutionTracker).recordIfNew(solutionCaptor.capture());
    order.verify(mockStoppingCriterion).shouldStop(any());
    verifyNoMoreInteractions(mockVisitedSolutionTracker);
    verifyNoInteractions(mockDestroyOperator, mockRepairOperator);
    verify(mockAcceptanceCriterion, never()).shouldAccept(any());

    assertNotSame(initialSolution, solutionCaptor.getValue());
    assertEquals(100.0, solutionCaptor.getValue().getObjectiveValue());
    assertEquals(0, result.totalIterations());
  }

  @Test
  void solve_ShouldRecordOnlyAcceptedCandidates_WhenCandidatesHaveDifferentOutcomes() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(TestSolution::getObjectiveValue);
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator(mockDestroyOperator)
            .addRepairOperator(mockRepairOperator)
            .operatorSelector(mockOperatorSelector)
            .visitedSolutionTracker(tracker)
            .build();

    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));
    when(mockDestroyOperator.apply(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(mockRepairOperator.apply(any(), any()))
        .thenReturn(
            new TestSolution(90.0),
            new TestSolution(120.0),
            new TestSolution(110.0),
            new TestSolution(130.0),
            new TestSolution(50.0, false));
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(false, true, true, false, true);

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), mockAcceptanceCriterion, new MaxIterations<>(5));

    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.captor();
    verify(mockOperatorSelector, times(5)).update(feedbackCaptor.capture());
    verify(mockAcceptanceCriterion, times(5)).shouldAccept(any());

    assertEquals(
        List.of(
            SolutionStatus.BEST,
            SolutionStatus.ACCEPTED,
            SolutionStatus.BETTER,
            SolutionStatus.REJECTED,
            SolutionStatus.REJECTED),
        feedbackCaptor.getAllValues().stream().map(OperatorSelectionFeedback::status).toList());
    assertEquals(4, tracker.getVisitedSolutionCount());
    assertEquals(5, result.totalIterations());
    assertEquals(90.0, result.bestSolution().getObjectiveValue());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void solve_ShouldSuppressRewardAndPreserveAcceptance_WhenCandidateHasEqualObjective(
      boolean isNew) {
    TestSolution candidateSolution = new TestSolution(100.0);
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator((solution, randomGenerator) -> candidateSolution)
            .operatorSelector(mockOperatorSelector)
            .visitedSolutionTracker(mockVisitedSolutionTracker)
            .collectStatistics(true)
            .build();

    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(true);
    when(mockVisitedSolutionTracker.recordIfNew(any())).thenReturn(isNew);

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), mockAcceptanceCriterion, new MaxIterations<>(3));

    InOrder order =
        inOrder(mockVisitedSolutionTracker, mockAcceptanceCriterion, mockOperatorSelector);
    order.verify(mockVisitedSolutionTracker).reset();
    order.verify(mockVisitedSolutionTracker).recordIfNew(any());
    for (int iteration = 0; iteration < 3; iteration++) {
      order.verify(mockOperatorSelector).select(any());
      order.verify(mockAcceptanceCriterion).shouldAccept(any());
      order.verify(mockVisitedSolutionTracker).recordIfNew(same(candidateSolution));
      order
          .verify(mockOperatorSelector)
          .update(
              new OperatorSelectionFeedback<>(
                  new OperatorSelection(0, 0), candidateSolution, SolutionStatus.ACCEPTED, false));
    }
    verifyNoMoreInteractions(mockVisitedSolutionTracker);

    assertEquals(3, result.totalIterations());
    assertEquals(3, result.statistics().getStatusCount(SolutionStatus.ACCEPTED));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void solve_ShouldRewardAcceptedSolution_WhenPreviousCandidateWithSameKeyWasRejected(
      boolean isFeasible) {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(TestSolution::getObjectiveValue);
    TestSolution firstSolution = new TestSolution(120.0, isFeasible);
    TestSolution candidateSolution = new TestSolution(120.0);
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(mockRepairOperator)
            .operatorSelector(mockOperatorSelector)
            .visitedSolutionTracker(tracker)
            .collectStatistics(true)
            .build();

    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));
    when(mockRepairOperator.apply(any(), any())).thenReturn(firstSolution, candidateSolution);
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(!isFeasible, true);

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), mockAcceptanceCriterion, new MaxIterations<>(2));

    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.captor();
    verify(mockOperatorSelector, times(2)).update(feedbackCaptor.capture());

    assertEquals(SolutionStatus.REJECTED, feedbackCaptor.getAllValues().get(0).status());
    assertEquals(
        new OperatorSelectionFeedback<>(
            new OperatorSelection(0, 0), candidateSolution, SolutionStatus.ACCEPTED, true),
        feedbackCaptor.getAllValues().get(1));
    assertEquals(2, tracker.getVisitedSolutionCount());
    assertEquals(List.of(100.0, 100.0, 120.0), result.statistics().getCurrentObjectiveValues());
  }

  @Test
  void solve_ShouldSuppressRewardWithoutChangingAcceptance_WhenAcceptedSolutionsAreRevisited() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(TestSolution::getObjectiveValue);
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(mockRepairOperator)
            .operatorSelector(mockOperatorSelector)
            .visitedSolutionTracker(tracker)
            .collectStatistics(true)
            .build();

    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));
    when(mockRepairOperator.apply(any(), any()))
        .thenReturn(
            new TestSolution(120.0),
            new TestSolution(110.0),
            new TestSolution(120.0),
            new TestSolution(110.0),
            new TestSolution(100.0));
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(true);

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), mockAcceptanceCriterion, new MaxIterations<>(5));

    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.captor();
    verify(mockOperatorSelector, times(5)).update(feedbackCaptor.capture());
    verify(mockAcceptanceCriterion, times(5)).shouldAccept(any());

    assertEquals(
        List.of(
            SolutionStatus.ACCEPTED,
            SolutionStatus.BETTER,
            SolutionStatus.ACCEPTED,
            SolutionStatus.BETTER,
            SolutionStatus.BETTER),
        feedbackCaptor.getAllValues().stream().map(OperatorSelectionFeedback::status).toList());
    assertEquals(
        List.of(true, true, false, false, false),
        feedbackCaptor.getAllValues().stream()
            .map(OperatorSelectionFeedback::isRewardEligible)
            .toList());
    assertEquals(
        List.of(100.0, 120.0, 110.0, 120.0, 110.0, 100.0),
        result.statistics().getCurrentObjectiveValues());
    assertEquals(3, tracker.getVisitedSolutionCount());
    assertEquals(3, result.statistics().getStatusCount(SolutionStatus.BETTER));
    assertEquals(2, result.statistics().getStatusCount(SolutionStatus.ACCEPTED));
    assertEquals(0, result.statistics().getStatusCount(SolutionStatus.REJECTED));
  }

  @Test
  void solve_ShouldRewardGlobalBest_WhenTrackerReportsDuplicate() {
    TestSolution candidateSolution = new TestSolution(90.0);
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator((solution, randomGenerator) -> candidateSolution)
            .operatorSelector(mockOperatorSelector)
            .visitedSolutionTracker(mockVisitedSolutionTracker)
            .build();

    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), mockAcceptanceCriterion, new MaxIterations<>(1));

    verify(mockAcceptanceCriterion).shouldAccept(any());
    verify(mockVisitedSolutionTracker).recordIfNew(same(candidateSolution));
    verify(mockOperatorSelector)
        .update(
            new OperatorSelectionFeedback<>(
                new OperatorSelection(0, 0), candidateSolution, SolutionStatus.BEST, true));

    assertEquals(90.0, result.bestSolution().getObjectiveValue());
  }

  @Test
  void solve_ShouldClearPreviousVisitedSolutions_WhenSameAlnsIsReused() {
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(TestSolution::getObjectiveValue);
    tracker.recordIfNew(new TestSolution(999.0));
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) -> new TestSolution(solution.getObjectiveValue() - 1))
            .visitedSolutionTracker(tracker)
            .build();

    AlnsResult<TestSolution> firstResult =
        alns.solve(new TestSolution(100.0), context -> true, new MaxIterations<>(2));

    assertEquals(2, firstResult.totalIterations());
    assertEquals(3, tracker.getVisitedSolutionCount());

    AlnsResult<TestSolution> secondResult =
        alns.solve(new TestSolution(200.0), context -> true, new MaxIterations<>(1));

    assertEquals(1, secondResult.totalIterations());
    assertEquals(2, tracker.getVisitedSolutionCount());

    assertTrue(tracker.recordIfNew(new TestSolution(100.0)));
    assertFalse(tracker.recordIfNew(new TestSolution(200.0)));
    assertEquals(3, tracker.getVisitedSolutionCount());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void solve_ShouldPreserveSearchAndRandomSequence_WhenTrackerIsConfigured(
      boolean trackingEnabled) {
    Random randomGeneratorWithoutTracker = new Random(123);
    Random randomGeneratorWithTracker = new Random(123);
    HashSetVisitedSolutionTracker<Double, TestSolution> tracker =
        new HashSetVisitedSolutionTracker<>(TestSolution::getObjectiveValue);

    Alns.Builder<TestSolution> builder =
        Alns.<TestSolution>builder()
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) -> new TestSolution(randomGenerator.nextInt(5) + 1))
            .collectStatistics(true);
    Alns<TestSolution> alnsWithoutTracker =
        builder.randomGenerator(randomGeneratorWithoutTracker).build();
    Alns<TestSolution> alnsWithTracker =
        builder
            .randomGenerator(randomGeneratorWithTracker)
            .visitedSolutionTracker(trackingEnabled ? tracker : VisitedSolutionTracker.disabled())
            .build();

    SimulatedAnnealing<TestSolution> criterionWithoutTracker =
        new SimulatedAnnealing<>(100.0, 1.0, 0.9);
    SimulatedAnnealing<TestSolution> criterionWithTracker =
        new SimulatedAnnealing<>(100.0, 1.0, 0.9);

    AlnsResult<TestSolution> resultWithoutTracker =
        alnsWithoutTracker.solve(
            new TestSolution(3.0), criterionWithoutTracker, new MaxIterations<>(20));
    AlnsResult<TestSolution> resultWithTracker =
        alnsWithTracker.solve(new TestSolution(3.0), criterionWithTracker, new MaxIterations<>(20));

    assertEquals(resultWithoutTracker.totalIterations(), resultWithTracker.totalIterations());
    assertEquals(
        resultWithoutTracker.bestSolution().getObjectiveValue(),
        resultWithTracker.bestSolution().getObjectiveValue());
    assertEquals(
        resultWithoutTracker.statistics().getCurrentObjectiveValues(),
        resultWithTracker.statistics().getCurrentObjectiveValues());
    assertEquals(
        resultWithoutTracker.statistics().getBestObjectiveValues(),
        resultWithTracker.statistics().getBestObjectiveValues());
    assertEquals(criterionWithoutTracker.getTemperature(), criterionWithTracker.getTemperature());
    assertEquals(randomGeneratorWithoutTracker.nextLong(), randomGeneratorWithTracker.nextLong());
    assertEquals(trackingEnabled, tracker.getVisitedSolutionCount() > 0);
  }
}
