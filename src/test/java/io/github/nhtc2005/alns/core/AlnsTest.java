package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.acceptance.AcceptanceCriterion;
import io.github.nhtc2005.alns.acceptance.HillClimbing;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.operator.DestroyOperator;
import io.github.nhtc2005.alns.operator.RepairOperator;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import io.github.nhtc2005.alns.selection.OperatorSelectionContext;
import io.github.nhtc2005.alns.selection.OperatorSelectionFeedback;
import io.github.nhtc2005.alns.selection.OperatorSelector;
import io.github.nhtc2005.alns.selection.RandomOperatorSelector;
import io.github.nhtc2005.alns.selection.RouletteWheelOperatorSelector;
import io.github.nhtc2005.alns.selection.SegmentedRouletteWheelOperatorSelector;
import io.github.nhtc2005.alns.stopping.AnyOfStoppingCriterion;
import io.github.nhtc2005.alns.stopping.MaxIterations;
import io.github.nhtc2005.alns.stopping.NoImprovement;
import io.github.nhtc2005.alns.stopping.StoppingCriterion;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
    clearInvocations(mockOperatorSelector);
  }

  @Test
  void builder_ShouldThrowException_WhenFieldsAreMissing() {
    Alns.Builder<TestSolution> builder = Alns.builder();

    NullPointerException randomGeneratorException =
        assertThrows(NullPointerException.class, builder::build);

    assertEquals("A randomGenerator is required.", randomGeneratorException.getMessage());

    builder.randomGenerator(mockRandomGenerator);
    IllegalStateException destroyException =
        assertThrows(IllegalStateException.class, builder::build);

    assertEquals("At least one destroy operator is required.", destroyException.getMessage());

    builder.addDestroyOperator(mockDestroyOperator);
    IllegalStateException repairException =
        assertThrows(IllegalStateException.class, builder::build);

    assertEquals("At least one repair operator is required.", repairException.getMessage());
  }

  @Test
  void builder_ShouldUseDefaultSelector_WhenSelectorNotSet() {
    TestSolution initialSolution = new TestSolution(100.0);
    TestSolution partialSolution = new TestSolution(100.0);
    TestSolution candidateSolution = new TestSolution(50.0);
    Alns<TestSolution> alnsWithDefaultSelector =
        Alns.<TestSolution>builder()
            .randomGenerator(mockRandomGenerator)
            .addDestroyOperator(mockDestroyOperator)
            .addRepairOperator(mockRepairOperator)
            .build();

    when(mockStoppingCriterion.shouldStop(any())).thenReturn(false).thenReturn(true);
    when(mockDestroyOperator.apply(any(), any())).thenReturn(partialSolution);
    when(mockRepairOperator.apply(any(), any())).thenReturn(candidateSolution);
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(true);

    AlnsResult<TestSolution> result =
        alnsWithDefaultSelector.solve(
            initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    assertEquals(50.0, result.bestSolution().getObjectiveValue());
    assertEquals(1, result.totalIterations());
    verify(mockRandomGenerator, times(2)).nextInt(1);
    verify(mockDestroyOperator).apply(any(), same(mockRandomGenerator));
    verify(mockRepairOperator).apply(same(partialSolution), same(mockRandomGenerator));
    verifyNoInteractions(mockOperatorSelector);
  }

  @Test
  void builder_ShouldThrowException_WhenOperatorIsNull() {
    Alns.Builder<TestSolution> builder = Alns.builder();

    NullPointerException destroyException =
        assertThrows(NullPointerException.class, () -> builder.addDestroyOperator(null));

    assertEquals("destroyOperator must not be null.", destroyException.getMessage());

    NullPointerException repairException =
        assertThrows(NullPointerException.class, () -> builder.addRepairOperator(null));

    assertEquals("repairOperator must not be null.", repairException.getMessage());
  }

  @Test
  void constructor_ShouldThrowException_WhenOperatorListIsEmpty() {
    List<DestroyOperator<TestSolution>> emptyDestroyOperators = List.of();
    List<RepairOperator<TestSolution>> validRepairOperators = List.of(mockRepairOperator);

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

    List<DestroyOperator<TestSolution>> validDestroyOperators = List.of(mockDestroyOperator);
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

  @ParameterizedTest
  @CsvSource({"1, 2", "3, 2", "2, 1", "2, 3"})
  void constructor_ShouldThrowException_WhenSelectorOperatorCountsMismatch(
      int destroys, int repairs) {
    String expectedMessage =
        "Selector operator counts must match the configured destroy and repair operator counts.";
    for (OperatorSelector<TestSolution> selector : operatorCountSelectors(destroys, repairs)) {
      IllegalArgumentException constructorException =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new Alns<>(
                      new Random(1),
                      List.of(mockDestroyOperator, mockDestroyOperator),
                      List.of(mockRepairOperator, mockRepairOperator),
                      selector));

      assertEquals(expectedMessage, constructorException.getMessage());

      IllegalArgumentException builderException =
          assertThrows(
              IllegalArgumentException.class,
              () -> operatorCountsBuilder().operatorSelector(selector).build());

      assertEquals(expectedMessage, builderException.getMessage());
    }
  }

  @Test
  void constructor_ShouldNotThrowException_WhenSelectorOperatorCountsMatch() {
    for (OperatorSelector<TestSolution> selector : operatorCountSelectors(2, 2)) {
      assertDoesNotThrow(
          () ->
              new Alns<>(
                  new Random(1),
                  List.of(mockDestroyOperator, mockDestroyOperator),
                  List.of(mockRepairOperator, mockRepairOperator),
                  selector));
      assertDoesNotThrow(() -> operatorCountsBuilder().operatorSelector(selector).build());
    }
  }

  @Test
  void constructor_ShouldPropagateValidationFailure_WhenCustomSelectorRejectsCounts() {
    OperatorSelector<TestSolution> selector =
        new OperatorSelector<>() {
          @Override
          public OperatorSelection select(OperatorSelectionContext<TestSolution> context) {
            return new OperatorSelection(0, 0);
          }

          @Override
          public void validateOperatorCounts(int destroyCount, int repairCount) {
            assertEquals(2, destroyCount);
            assertEquals(2, repairCount);
            throw new IllegalArgumentException("custom validation");
          }
        };
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> operatorCountsBuilder().operatorSelector(selector).build());

    assertEquals("custom validation", exception.getMessage());
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

  @ParameterizedTest
  @CsvSource({
    "50.0, true, true, BEST, 50.0",
    "50.0, true, false, BEST, 50.0",
    "120.0, true, true, ACCEPTED, 100.0",
    "120.0, true, false, REJECTED, 100.0",
    "40.0, false, true, REJECTED, 100.0",
    "40.0, false, false, REJECTED, 100.0"
  })
  void solve_ShouldClassifyCandidateAndUpdateBest_WhenCandidateIsEvaluated(
      double candidateObjective,
      boolean feasible,
      boolean accepted,
      SolutionStatus expectedStatus,
      double expectedBestObjective) {
    TestSolution initialSolution = new TestSolution(100.0);
    TestSolution candidateSolution = new TestSolution(candidateObjective, feasible);
    setupBasicMocks(new TestSolution(100.0), candidateSolution);
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(accepted);

    AlnsResult<TestSolution> result =
        alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion);

    assertEquals(expectedBestObjective, result.bestSolution().getObjectiveValue());
    assertEquals(1, result.totalIterations());
    verify(mockOperatorSelector).reset();
    verify(mockAcceptanceCriterion).reset();
    verify(mockStoppingCriterion).reset();
    verify(mockAcceptanceCriterion).shouldAccept(any());

    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.captor();
    verify(mockOperatorSelector).update(feedbackCaptor.capture());
    assertEquals(expectedStatus, feedbackCaptor.getValue().status());
    assertSame(candidateSolution, feedbackCaptor.getValue().candidateSolution());
    assertEquals(
        candidateObjective, feedbackCaptor.getValue().candidateSolution().getObjectiveValue());
  }

  @Test
  void solve_ShouldSetBetterStatus_WhenCandidateIsBetterThanCurrent() {
    TestSolution initialSolution = new TestSolution(100.0);
    TestSolution partialSolution = new TestSolution(100.0);
    TestSolution worseCandidateSolution = new TestSolution(120.0);
    TestSolution betterCandidateSolution = new TestSolution(110.0);

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

    assertEquals(100.0, result.bestSolution().getObjectiveValue());
    assertEquals(2, result.totalIterations());

    ArgumentCaptor<OperatorSelectionFeedback<TestSolution>> feedbackCaptor =
        ArgumentCaptor.captor();
    verify(mockOperatorSelector, times(2)).update(feedbackCaptor.capture());
    verify(mockAcceptanceCriterion, times(2)).shouldAccept(any());

    List<OperatorSelectionFeedback<TestSolution>> feedbacks = feedbackCaptor.getAllValues();
    assertEquals(SolutionStatus.ACCEPTED, feedbacks.get(0).status());
    assertEquals(SolutionStatus.BETTER, feedbacks.get(1).status());
  }

  @ParameterizedTest
  @CsvSource({"-1, 0", "1, 0", "0, -1", "0, 1"})
  void solve_ShouldThrowException_WhenOperatorIndexIsOutOfBounds(
      int destroyOperatorIndex, int repairOperatorIndex) {
    TestSolution initialSolution = new TestSolution(100.0);
    when(mockOperatorSelector.select(any()))
        .thenReturn(new OperatorSelection(destroyOperatorIndex, repairOperatorIndex));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> alns.solve(initialSolution, mockAcceptanceCriterion, mockStoppingCriterion));
    assertEquals(
        "operatorSelector returned an out-of-range operator index.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void solve_ShouldThrowExceptionBeforeReset_WhenInitialObjectiveIsNonFinite(double objective) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                alns.solve(
                    new TestSolution(objective), mockAcceptanceCriterion, mockStoppingCriterion));

    assertEquals("initialSolution objective must be finite.", exception.getMessage());
    verifyNoInteractions(
        mockOperatorSelector,
        mockAcceptanceCriterion,
        mockStoppingCriterion,
        mockDestroyOperator,
        mockRepairOperator);
  }

  @ParameterizedTest
  @CsvSource({
    "NaN, true",
    "Infinity, true",
    "-Infinity, true",
    "NaN, false",
    "Infinity, false",
    "-Infinity, false"
  })
  void solve_ShouldThrowExceptionBeforeAcceptance_WhenCandidateObjectiveIsNonFinite(
      double objective, boolean feasible) {
    setupBasicMocks(new TestSolution(100.0, false), new TestSolution(objective, feasible));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                alns.solve(
                    new TestSolution(100.0), mockAcceptanceCriterion, mockStoppingCriterion));

    assertEquals("candidateSolution objective must be finite.", exception.getMessage());
    verify(mockAcceptanceCriterion, never()).shouldAccept(any());
    verify(mockOperatorSelector, never()).update(any());
  }

  @ParameterizedTest
  @ValueSource(doubles = {-Double.MAX_VALUE, -1.0, -0.0, 0.0, 1.0, Double.MAX_VALUE})
  void solve_ShouldAllowInitialAndCandidateObjectives_WhenObjectivesAreFinite(double objective) {
    setupBasicMocks(new TestSolution(Double.NaN, false), new TestSolution(objective));
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(true);

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(objective), mockAcceptanceCriterion, mockStoppingCriterion);

    assertEquals(objective, result.bestSolution().getObjectiveValue());
    assertEquals(1, result.totalIterations());
    verify(mockAcceptanceCriterion).shouldAccept(any());
  }

  @Test
  void solve_ShouldResetSelector_WhenSameSelectorIsReused() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(1, 1, new double[] {7, 3, 2, 0}, 0.5, 2);
    Alns<TestSolution> reusedAlns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) -> new TestSolution(solution.getObjectiveValue() + 1))
            .operatorSelector(selector)
            .build();

    for (int run = 0; run < 2; run++) {
      AlnsResult<TestSolution> result =
          reusedAlns.solve(new TestSolution(100.0), context -> true, new MaxIterations<>(3));

      assertEquals(3, result.totalIterations());
      assertArrayEquals(new double[] {2.5}, selector.getDestroyOperatorWeights());
      assertArrayEquals(new double[] {2.5}, selector.getRepairOperatorWeights());
      assertArrayEquals(new double[] {2}, selector.getDestroyOperatorSegmentScores());
      assertArrayEquals(new double[] {2}, selector.getRepairOperatorSegmentScores());
      assertEquals(1, selector.getIterationsInSegment());
    }
  }

  @Test
  void solve_ShouldResetStoppingCriterion_WhenSameCriterionIsReused() {
    Alns<TestSolution> reusedAlns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator((solution, randomGenerator) -> solution)
            .build();
    NoImprovement<TestSolution> criterion = new NoImprovement<>(3);

    for (int run = 0; run < 2; run++) {
      AlnsResult<TestSolution> result =
          reusedAlns.solve(new TestSolution(100.0), new HillClimbing<>(), criterion);

      assertEquals(3, result.totalIterations());
      assertEquals(100.0, result.bestSolution().getObjectiveValue());
    }
  }

  @ParameterizedTest
  @CsvSource({"2, 2, 2", "100, 4, 3"})
  void solve_ShouldResetComposedStoppingCriteria_WhenSameCriterionIsReused(
      long maxIterations, long expectedFirstIterations, long expectedSecondIterations) {
    Alns<TestSolution> reusedAlns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) ->
                    solution.getObjectiveValue() == 100.0 ? new TestSolution(90.0) : solution)
            .build();
    AnyOfStoppingCriterion<TestSolution> criterion =
        new AnyOfStoppingCriterion<TestSolution>(
            List.of(new MaxIterations<>(maxIterations), new NoImprovement<>(3)));

    AlnsResult<TestSolution> firstResult =
        reusedAlns.solve(new TestSolution(100.0), new HillClimbing<>(), criterion);
    AlnsResult<TestSolution> secondResult =
        reusedAlns.solve(new TestSolution(200.0), new HillClimbing<>(), criterion);

    assertEquals(expectedFirstIterations, firstResult.totalIterations());
    assertEquals(90.0, firstResult.bestSolution().getObjectiveValue());
    assertEquals(expectedSecondIterations, secondResult.totalIterations());
    assertEquals(200.0, secondResult.bestSolution().getObjectiveValue());
  }

  private void setupBasicMocks(TestSolution partialSolution, TestSolution candidateSolution) {
    when(mockStoppingCriterion.shouldStop(any())).thenReturn(false).thenReturn(true);
    when(mockOperatorSelector.select(any())).thenReturn(new OperatorSelection(0, 0));
    when(mockDestroyOperator.apply(any(), any())).thenReturn(partialSolution);
    when(mockRepairOperator.apply(any(), any())).thenReturn(candidateSolution);
  }

  private Alns.Builder<TestSolution> operatorCountsBuilder() {
    return Alns.<TestSolution>builder()
        .randomGenerator(new Random(1))
        .addDestroyOperator(mockDestroyOperator)
        .addDestroyOperator(mockDestroyOperator)
        .addRepairOperator(mockRepairOperator)
        .addRepairOperator(mockRepairOperator);
  }

  private List<OperatorSelector<TestSolution>> operatorCountSelectors(int destroys, int repairs) {
    return List.of(
        new RandomOperatorSelector<>(destroys, repairs),
        new RouletteWheelOperatorSelector<>(destroys, repairs, new double[] {7, 3, 1, 0}, 0.5),
        new SegmentedRouletteWheelOperatorSelector<>(
            destroys, repairs, new double[] {7, 3, 1, 0}, 0.5, 10));
  }
}
