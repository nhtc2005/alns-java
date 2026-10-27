package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.acceptance.AcceptanceCriterion;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.operator.DestroyOperator;
import io.github.nhtc2005.alns.operator.RepairOperator;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import io.github.nhtc2005.alns.selection.OperatorSelectionFeedback;
import io.github.nhtc2005.alns.selection.OperatorSelector;
import io.github.nhtc2005.alns.stopping.StoppingCriterion;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlnsTest {

  @Mock private RandomGenerator mockRandomGenerator;
  @Mock private DestroyOperator<TestSolution> mockDestroyOperator;
  @Mock private RepairOperator<TestSolution> mockRepairOperator;
  @Mock private OperatorSelector<TestSolution> mockOperatorSelector;
  @Mock private AcceptanceCriterion<TestSolution> mockAcceptanceCriterion;
  @Mock private StoppingCriterion<TestSolution> mockStoppingCriterion;

  private Alns<TestSolution> alns;

  @BeforeEach
  void setUp() {
    alns =
        Alns.<TestSolution>builder()
            .randomGenerator(mockRandomGenerator)
            .addDestroyOperator(mockDestroyOperator)
            .addRepairOperator(mockRepairOperator)
            .operatorSelector(mockOperatorSelector)
            .build();
  }

  @Test
  void builder_ShouldThrowException_WhenFieldsAreMissing() {
    Alns.Builder<TestSolution> builder = Alns.builder();

    assertThrows(NullPointerException.class, builder::build);

    builder.randomGenerator(mockRandomGenerator);
    assertThrows(IllegalStateException.class, builder::build);

    builder.addDestroyOperator(mockDestroyOperator);
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  void builder_ShouldUseDefaultSelector_WhenSelectorNotSet() {
    Alns<TestSolution> alnsWithDefaultSelector =
        Alns.<TestSolution>builder()
            .randomGenerator(mockRandomGenerator)
            .addDestroyOperator(mockDestroyOperator)
            .addRepairOperator(mockRepairOperator)
            .build();

    assertNotNull(alnsWithDefaultSelector);
  }

  @Test
  void copyNonEmpty_ShouldThrowNullPointerException_WhenListIsNull() {
    Alns.Builder<TestSolution> builder = Alns.builder();

    NullPointerException destroyException =
        assertThrows(NullPointerException.class, () -> builder.addDestroyOperator(null));
    assertEquals("destroyOperator must not be null.", destroyException.getMessage());

    NullPointerException repairException =
        assertThrows(NullPointerException.class, () -> builder.addRepairOperator(null));
    assertEquals("repairOperator must not be null.", repairException.getMessage());
  }

  @Test
  void copyNonEmpty_ShouldThrowIllegalArgumentException_WhenListIsEmpty() {
    List<DestroyOperator<TestSolution>> emptyDestroyOperators = List.of();
    List<RepairOperator<TestSolution>> validRepairOperators =
        List.of((solution, randomGenerator) -> null);

    IllegalArgumentException destroyException =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new Alns<>(
                    mockRandomGenerator,
                    emptyDestroyOperators,
                    validRepairOperators,
                    mockOperatorSelector));
    assertEquals("destroyOperators must not be empty.", destroyException.getMessage());

    List<DestroyOperator<TestSolution>> validDestroyOperators =
        List.of((solution, randomGenerator) -> null);
    List<RepairOperator<TestSolution>> emptyRepairOperators = List.of();

    IllegalArgumentException repairException =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new Alns<>(
                    mockRandomGenerator,
                    validDestroyOperators,
                    emptyRepairOperators,
                    mockOperatorSelector));
    assertEquals("repairOperators must not be empty.", repairException.getMessage());
  }

  @Test
  void solve_ShouldThrowException_WhenInitialSolutionIsInfeasible() {
    TestSolution infeasibleSolution = new TestSolution(100.0, false);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> alns.solve(infeasibleSolution, mockAcceptanceCriterion, mockStoppingCriterion));

    assertEquals("initialSolution must be feasible.", exception.getMessage());
  }

  @Test
  void solve_ShouldUpdateBestSolution_WhenCandidateIsBest() {
    TestSolution initialSolution = new TestSolution(100.0, true);
    TestSolution partialSolution = new TestSolution(100.0, true);
    TestSolution candidateSolution = new TestSolution(50.0, true);

    setupBasicMocks(initialSolution, partialSolution, candidateSolution);
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(true);

    AlnsResult<TestSolution> result =
        alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    assertNotNull(result);
    assertEquals(50.0, result.bestSolution().getObjectiveValue());
    assertEquals(1, result.totalIterations());

    verify(mockOperatorSelector).reset();
    verify(mockAcceptanceCriterion).reset();
    verify(mockStoppingCriterion).reset();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.forClass(OperatorSelectionFeedback.class);
    verify(mockOperatorSelector).update(feedbackCaptor.capture());
    assertEquals(SolutionStatus.BEST, feedbackCaptor.getValue().status());
  }

  @Test
  void solve_ShouldSetBetterStatus_WhenCandidateIsBetterThanCurrent() {
    TestSolution initialSolution = new TestSolution(100.0, true);
    TestSolution partialSolution = new TestSolution(100.0, true);
    TestSolution worseCandidateSolution = new TestSolution(120.0, true);
    TestSolution betterCandidateSolution = new TestSolution(110.0, true);

    when(mockStoppingCriterion.shouldStop(any()))
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(true);

    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));
    when(mockDestroyOperator.apply(any(), any())).thenReturn(partialSolution);

    when(mockRepairOperator.apply(any(), any()))
        .thenReturn(worseCandidateSolution)
        .thenReturn(betterCandidateSolution);

    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(true);

    AlnsResult<TestSolution> result =
        alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    assertNotNull(result);
    assertEquals(2, result.totalIterations());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.forClass(OperatorSelectionFeedback.class);
    verify(mockOperatorSelector, atLeastOnce()).update(feedbackCaptor.capture());

    List<OperatorSelectionFeedback<TestSolution>> feedbacks = feedbackCaptor.getAllValues();
    assertEquals(SolutionStatus.ACCEPTED, feedbacks.get(0).status());
    assertEquals(SolutionStatus.BETTER, feedbacks.get(1).status());
  }

  @Test
  void solve_ShouldSetAcceptedStatus_WhenCandidateIsWorseThanCurrentButAccepted() {
    TestSolution initialSolution = new TestSolution(100.0, true);
    TestSolution partialSolution = new TestSolution(100.0, true);
    TestSolution candidateSolution = new TestSolution(120.0, true);

    setupBasicMocks(initialSolution, partialSolution, candidateSolution);
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(true);

    AlnsResult<TestSolution> result =
        alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    assertNotNull(result);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.forClass(OperatorSelectionFeedback.class);
    verify(mockOperatorSelector).update(feedbackCaptor.capture());

    assertEquals(SolutionStatus.ACCEPTED, feedbackCaptor.getValue().status());
    assertEquals(120.0, feedbackCaptor.getValue().candidateSolution().getObjectiveValue());
  }

  @Test
  void solve_ShouldSetRejectedStatus_WhenAcceptedCriterionReturnsFalse() {
    TestSolution initialSolution = new TestSolution(100.0, true);
    TestSolution partialSolution = new TestSolution(100.0, true);
    TestSolution candidateSolution = new TestSolution(120.0, true);

    setupBasicMocks(initialSolution, partialSolution, candidateSolution);
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(false);

    AlnsResult<TestSolution> result =
        alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    assertNotNull(result);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.forClass(OperatorSelectionFeedback.class);
    verify(mockOperatorSelector).update(feedbackCaptor.capture());

    assertEquals(SolutionStatus.REJECTED, feedbackCaptor.getValue().status());
  }

  @Test
  void solve_ShouldSetRejectedStatus_WhenCandidateIsInfeasible() {
    TestSolution initialSolution = new TestSolution(100.0, true);
    TestSolution partialSolution = new TestSolution(100.0, true);
    TestSolution infeasibleCandidateSolution = new TestSolution(40.0, false);

    setupBasicMocks(initialSolution, partialSolution, infeasibleCandidateSolution);

    AlnsResult<TestSolution> result =
        alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    assertNotNull(result);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.forClass(OperatorSelectionFeedback.class);
    verify(mockOperatorSelector).update(feedbackCaptor.capture());

    assertEquals(SolutionStatus.REJECTED, feedbackCaptor.getValue().status());

    verify(mockAcceptanceCriterion, atLeastOnce()).shouldAccept(any());
  }

  @ParameterizedTest
  @CsvSource({"-1, 0", "1, 0", "0, -1", "0, 1"})
  void validateOperatorSelection_ShouldThrowIllegalStateException_WhenIndexIsOutOfBounds(
      int destroyOperatorIndex, int repairOperatorIndex) {
    TestSolution initialSolution = new TestSolution(100.0, true);
    when(mockOperatorSelector.select(any()))
        .thenReturn(new OperatorSelection(destroyOperatorIndex, repairOperatorIndex));

    assertThrows(
        IllegalStateException.class,
        () -> alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion));
  }

  private void setupBasicMocks(
      TestSolution initialSolution, TestSolution partialSolution, TestSolution candidateSolution) {
    when(mockStoppingCriterion.shouldStop(any())).thenReturn(false).thenReturn(true);
    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));
    when(mockDestroyOperator.apply(any(), any())).thenReturn(partialSolution);
    when(mockRepairOperator.apply(any(), any())).thenReturn(candidateSolution);
  }
}
