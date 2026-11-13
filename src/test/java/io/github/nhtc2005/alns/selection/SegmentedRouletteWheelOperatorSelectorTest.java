package io.github.nhtc2005.alns.selection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.core.Alns;
import io.github.nhtc2005.alns.core.AlnsResult;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.stopping.MaxIterations;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentedRouletteWheelOperatorSelectorTest {

  @Mock private RandomGenerator mockRandomGenerator;

  @ParameterizedTest
  @CsvSource({"0, 2", "-1, 2", "2, 0", "2, -1"})
  void constructor_ShouldThrowException_WhenOperatorCountsAreInvalid(
      int destroyOperatorCount, int repairOperatorCount) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SegmentedRouletteWheelOperatorSelector<TestSolution>(
                destroyOperatorCount, repairOperatorCount, new double[] {7, 3, 1, 0}, 0.5, 2));
  }

  @Test
  void constructor_ShouldThrowException_WhenScoresAreNull() {
    assertThrows(
        NullPointerException.class,
        () -> new SegmentedRouletteWheelOperatorSelector<TestSolution>(2, 2, null, 0.5, 2));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 3, 5})
  void constructor_ShouldThrowException_WhenScoreCountIsNotFour(int scoreCount) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SegmentedRouletteWheelOperatorSelector<TestSolution>(
                2, 2, new double[scoreCount], 0.5, 2));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void constructor_ShouldThrowException_WhenScoreIsInvalid(double score) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SegmentedRouletteWheelOperatorSelector<TestSolution>(
                2, 2, new double[] {7, 3, 1, score}, 0.5, 2));
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void constructor_ShouldThrowException_WhenDecayIsInvalid(double decay) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SegmentedRouletteWheelOperatorSelector<TestSolution>(
                2, 2, new double[] {7, 3, 1, 0}, decay, 2));
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void constructor_ShouldThrowException_WhenSegmentLengthIsNonPositive(long segmentLength) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SegmentedRouletteWheelOperatorSelector<TestSolution>(
                2, 2, new double[] {7, 3, 1, 0}, 0.5, segmentLength));
  }

  @Test
  void select_ShouldUseUniformInitialWeights_WhenNoFeedbackHasBeenReceived() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(3, 2, new double[] {7, 3, 1, 0}, 0.5, 2);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.0, Math.nextDown(1.0));

    assertEquals(3, selector.getDestroyOperatorCount());
    assertEquals(2, selector.getRepairOperatorCount());
    assertEquals(0.5, selector.getDecay());
    assertEquals(2, selector.getSegmentLength());
    assertEquals(0, selector.getIterationsInSegment());
    assertEquals(new OperatorSelection(0, 1), selector.select(context()));
  }

  @Test
  void update_ShouldAccumulateScoresWithoutChangingWeights_WhenSegmentIsIncomplete() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 3);
    selector.update(feedback(0, 1, SolutionStatus.BEST));
    selector.update(feedback(1, 0, SolutionStatus.BETTER));

    assertEquals(2, selector.getIterationsInSegment());
    assertArrayEquals(new double[] {7, 3}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {3, 7}, selector.getRepairOperatorSegmentScores());
    assertArrayEquals(new double[] {1, 1}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1, 1}, selector.getRepairOperatorWeights());
  }

  @ParameterizedTest
  @CsvSource({"BETTER", "ACCEPTED"})
  void update_ShouldAdvanceSegmentWithoutAddingScore_WhenFeedbackIsNotRewardEligible(
      SolutionStatus status) {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 3);
    selector.update(feedback(1, 0, SolutionStatus.BEST));

    selector.update(
        new OperatorSelectionFeedback<>(
            new OperatorSelection(1, 0), new TestSolution(100.0), status, false));

    assertEquals(2, selector.getIterationsInSegment());
    assertArrayEquals(new double[] {0, 7}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {7, 0}, selector.getRepairOperatorSegmentScores());
    assertArrayEquals(new double[] {1, 1}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1, 1}, selector.getRepairOperatorWeights());

    selector.update(
        new OperatorSelectionFeedback<>(
            new OperatorSelection(0, 1), new TestSolution(100.0), status, false));

    assertEquals(0, selector.getIterationsInSegment());
    assertArrayEquals(new double[] {0, 0}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {0, 0}, selector.getRepairOperatorSegmentScores());
    assertArrayEquals(new double[] {0.5, 4}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {4, 0.5}, selector.getRepairOperatorWeights());
  }

  @Test
  void update_ShouldApplyConfiguredRejectedScore_WhenUsingLegacyFeedbackConstructor() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(2, 2, new double[] {7, 3, 1, 2}, 0.5, 1);

    selector.update(feedback(1, 0, SolutionStatus.REJECTED));

    assertArrayEquals(new double[] {0.5, 1.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1.5, 0.5}, selector.getRepairOperatorWeights());
  }

  @ParameterizedTest
  @CsvSource({"BEST, 7.5", "BETTER, 3.5", "ACCEPTED, 1.5", "REJECTED, 0.5"})
  void update_ShouldApplyTotalOutcomeScore_WhenSegmentCompletes(
      SolutionStatus status, double expectedWeight) {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 2);
    selector.update(feedback(0, 0, status));
    selector.update(feedback(0, 0, status));

    assertArrayEquals(new double[] {expectedWeight, 0.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {expectedWeight, 0.5}, selector.getRepairOperatorWeights());
    assertArrayEquals(new double[] {0, 0}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {0, 0}, selector.getRepairOperatorSegmentScores());
    assertEquals(0, selector.getIterationsInSegment());
  }

  @Test
  void update_ShouldTrackScoresSeparately_WhenDestroyAndRepairSelectionsDiffer() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 2);
    selector.update(feedback(1, 0, SolutionStatus.BEST));
    selector.update(feedback(1, 1, SolutionStatus.BETTER));

    assertArrayEquals(new double[] {0.5, 5.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {4, 2}, selector.getRepairOperatorWeights());
  }

  @Test
  void update_ShouldDiscardPreviousSegmentScores_WhenNextSegmentCompletes() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 2);
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(1, 1, SolutionStatus.BETTER));

    assertEquals(1, selector.getIterationsInSegment());
    assertArrayEquals(new double[] {0, 3}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {0, 3}, selector.getRepairOperatorSegmentScores());
    assertArrayEquals(new double[] {7.5, 0.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {7.5, 0.5}, selector.getRepairOperatorWeights());

    selector.update(feedback(1, 1, SolutionStatus.BETTER));

    assertArrayEquals(new double[] {3.75, 3.25}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {3.75, 3.25}, selector.getRepairOperatorWeights());
    assertEquals(0, selector.getIterationsInSegment());
  }

  @Test
  void select_ShouldNotAdvanceSegment_WhenCalledRepeatedlyWithoutFeedback() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 2);
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.75);

    assertEquals(new OperatorSelection(1, 1), selector.select(context(0)));
    assertEquals(new OperatorSelection(1, 1), selector.select(context(Long.MAX_VALUE)));
    assertEquals(1, selector.getIterationsInSegment());
    assertArrayEquals(new double[] {7, 0}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {7, 0}, selector.getRepairOperatorSegmentScores());
    assertArrayEquals(new double[] {1, 1}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1, 1}, selector.getRepairOperatorWeights());

    selector.update(feedback(0, 0, SolutionStatus.BEST));

    assertArrayEquals(new double[] {7.5, 0.5}, selector.getDestroyOperatorWeights());
    assertEquals(0, selector.getIterationsInSegment());
  }

  @Test
  void update_ShouldUpdateAllWeightsImmediately_WhenSegmentLengthIsOne() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 1);
    selector.update(feedback(1, 0, SolutionStatus.BEST));

    assertArrayEquals(new double[] {0.5, 4}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {4, 0.5}, selector.getRepairOperatorWeights());
    assertEquals(0, selector.getIterationsInSegment());
  }

  @ParameterizedTest
  @CsvSource({"0.0, 14.0, 0.0", "1.0, 1.0, 1.0"})
  void update_ShouldRespectDecayBoundary_WhenSegmentCompletes(
      double decay, double expectedSelectedWeight, double expectedUnusedWeight) {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(decay, 2);
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(0, 0, SolutionStatus.BEST));

    assertArrayEquals(
        new double[] {expectedSelectedWeight, expectedUnusedWeight},
        selector.getDestroyOperatorWeights());
    assertArrayEquals(
        new double[] {expectedSelectedWeight, expectedUnusedWeight},
        selector.getRepairOperatorWeights());
    assertArrayEquals(new double[] {0, 0}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {0, 0}, selector.getRepairOperatorSegmentScores());
    assertEquals(0, selector.getIterationsInSegment());
  }

  @ParameterizedTest
  @CsvSource({"-1, 0", "2, 0", "0, -1", "0, 2"})
  void update_ShouldThrowExceptionWithoutChangingState_WhenOperatorIndexIsInvalid(
      int destroyOperatorIndex, int repairOperatorIndex) {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 2);
    selector.update(feedback(1, 0, SolutionStatus.BETTER));

    assertThrows(
        IndexOutOfBoundsException.class,
        () ->
            selector.update(
                feedback(destroyOperatorIndex, repairOperatorIndex, SolutionStatus.BEST)));
    assertArrayEquals(new double[] {0, 3}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {3, 0}, selector.getRepairOperatorSegmentScores());
    assertArrayEquals(new double[] {1, 1}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1, 1}, selector.getRepairOperatorWeights());
    assertEquals(1, selector.getIterationsInSegment());

    selector.update(feedback(1, 0, SolutionStatus.ACCEPTED));

    assertArrayEquals(new double[] {0.5, 2.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {2.5, 0.5}, selector.getRepairOperatorWeights());
    assertEquals(0, selector.getIterationsInSegment());
  }

  @ParameterizedTest
  @CsvSource({"0, 1", "1, 0"})
  void update_ShouldThrowExceptionWithoutChangingState_WhenAccumulatedScoreOverflows(
      int destroyOperatorIndex, int repairOperatorIndex) {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(
            2, 2, new double[] {Double.MAX_VALUE, 3, 1, 0}, 0.5, 2);
    selector.update(feedback(0, 0, SolutionStatus.BEST));

    assertThrows(
        ArithmeticException.class,
        () ->
            selector.update(
                feedback(destroyOperatorIndex, repairOperatorIndex, SolutionStatus.BEST)));
    assertArrayEquals(
        new double[] {Double.MAX_VALUE, 0}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(
        new double[] {Double.MAX_VALUE, 0}, selector.getRepairOperatorSegmentScores());
    assertArrayEquals(new double[] {1, 1}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1, 1}, selector.getRepairOperatorWeights());
    assertEquals(1, selector.getIterationsInSegment());

    selector.update(feedback(1, 1, SolutionStatus.REJECTED));

    assertArrayEquals(
        new double[] {Double.MAX_VALUE / 2, 0.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(
        new double[] {Double.MAX_VALUE / 2, 0.5}, selector.getRepairOperatorWeights());
    assertEquals(0, selector.getIterationsInSegment());
  }

  @Test
  void update_ShouldPreserveConfiguredScores_WhenCallerModifiesArrays() {
    double[] scores = {7, 3, 1, 0};
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(2, 2, scores, 0.5, 2);
    scores[0] = 0;

    double[] configuredScores = selector.getScores();
    assertArrayEquals(new double[] {7, 3, 1, 0}, configuredScores);
    configuredScores[0] = 0;
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(0, 0, SolutionStatus.BEST));

    assertArrayEquals(new double[] {7.5, 0.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {7.5, 0.5}, selector.getRepairOperatorWeights());
  }

  @Test
  void select_ShouldUseInternalWeights_WhenCallerModifiesReturnedArrays() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 1);
    selector.update(feedback(1, 0, SolutionStatus.BEST));

    double[] destroyOperatorWeights = selector.getDestroyOperatorWeights();
    double[] repairOperatorWeights = selector.getRepairOperatorWeights();
    assertArrayEquals(new double[] {0.5, 4}, destroyOperatorWeights);
    assertArrayEquals(new double[] {4, 0.5}, repairOperatorWeights);
    destroyOperatorWeights[1] = 0;
    repairOperatorWeights[0] = 0;
    when(mockRandomGenerator.nextDouble()).thenReturn(0.75);

    assertEquals(new OperatorSelection(1, 0), selector.select(context()));
  }

  @Test
  void update_ShouldUseInternalSegmentScores_WhenCallerModifiesReturnedArrays() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 2);
    selector.update(feedback(1, 0, SolutionStatus.BEST));

    double[] destroyOperatorSegmentScores = selector.getDestroyOperatorSegmentScores();
    double[] repairOperatorSegmentScores = selector.getRepairOperatorSegmentScores();
    assertArrayEquals(new double[] {0, 7}, destroyOperatorSegmentScores);
    assertArrayEquals(new double[] {7, 0}, repairOperatorSegmentScores);
    destroyOperatorSegmentScores[1] = 0;
    repairOperatorSegmentScores[0] = 0;
    selector.update(feedback(1, 0, SolutionStatus.BETTER));

    assertArrayEquals(new double[] {0.5, 5.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {5.5, 0.5}, selector.getRepairOperatorWeights());
  }

  @Test
  void select_ShouldSkipZeroWeightOperators_WhenOtherWeightsArePositive() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(3, 3, new double[] {7, 3, 1, 0}, 0.0, 2);
    selector.update(feedback(1, 1, SolutionStatus.BEST));
    selector.update(feedback(1, 1, SolutionStatus.REJECTED));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.0, Math.nextDown(1.0));

    assertEquals(new OperatorSelection(1, 1), selector.select(context()));
  }

  @Test
  void select_ShouldFallBackToUniformSelection_WhenAllWeightsAreZero() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.0, 1);
    selector.update(feedback(0, 0, SolutionStatus.REJECTED));
    when(mockRandomGenerator.nextInt(2)).thenReturn(1, 0);

    assertEquals(new OperatorSelection(1, 0), selector.select(context()));
    verify(mockRandomGenerator, never()).nextDouble();
  }

  @Test
  void select_ShouldAvoidOverflow_WhenWeightsAreVeryLarge() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(
            2, 2, new double[] {Double.MAX_VALUE, 3, 1, 0}, 0.0, 2);
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(1, 1, SolutionStatus.BEST));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.25, 0.75);

    assertEquals(new OperatorSelection(0, 1), selector.select(context()));
  }

  @Test
  void select_ShouldReturnLastPositiveIndex_WhenRandomValueReachesTotalWeight() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(3, 3, new double[] {7, 3, 1, 0}, 0.0, 1);
    selector.update(feedback(1, 1, SolutionStatus.BEST));
    // Deliberately outside [0.0, 1.0) to exercise the defensive fallback.
    when(mockRandomGenerator.nextDouble()).thenReturn(1.0);

    assertEquals(new OperatorSelection(1, 1), selector.select(context()));
  }

  @Test
  void reset_ShouldClearWeightsAndPendingScores_WhenSelectorIsReused() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector = selector(0.5, 2);
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(1, 0, SolutionStatus.BEST));
    selector.reset();

    assertArrayEquals(new double[] {1, 1}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1, 1}, selector.getRepairOperatorWeights());
    assertArrayEquals(new double[] {0, 0}, selector.getDestroyOperatorSegmentScores());
    assertArrayEquals(new double[] {0, 0}, selector.getRepairOperatorSegmentScores());
    assertEquals(0, selector.getIterationsInSegment());

    selector.update(feedback(1, 1, SolutionStatus.BETTER));
    selector.update(feedback(1, 1, SolutionStatus.BETTER));

    assertArrayEquals(new double[] {0.5, 3.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {0.5, 3.5}, selector.getRepairOperatorWeights());
  }

  @Test
  void solve_ShouldResetSelector_WhenSameSelectorIsReused() {
    SegmentedRouletteWheelOperatorSelector<TestSolution> selector =
        new SegmentedRouletteWheelOperatorSelector<>(1, 1, new double[] {7, 3, 2, 0}, 0.5, 2);
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) -> new TestSolution(solution.getObjectiveValue() + 1))
            .operatorSelector(selector)
            .build();

    for (int run = 0; run < 2; run++) {
      AlnsResult<TestSolution> result =
          alns.solve(new TestSolution(100.0), context -> true, new MaxIterations<>(3));

      assertEquals(3, result.totalIterations());
      assertArrayEquals(new double[] {2.5}, selector.getDestroyOperatorWeights());
      assertArrayEquals(new double[] {2.5}, selector.getRepairOperatorWeights());
      assertArrayEquals(new double[] {2}, selector.getDestroyOperatorSegmentScores());
      assertArrayEquals(new double[] {2}, selector.getRepairOperatorSegmentScores());
      assertEquals(1, selector.getIterationsInSegment());
    }
  }

  private SegmentedRouletteWheelOperatorSelector<TestSolution> selector(
      double decay, long segmentLength) {
    return new SegmentedRouletteWheelOperatorSelector<>(
        2, 2, new double[] {7, 3, 1, 0}, decay, segmentLength);
  }

  private OperatorSelectionContext<TestSolution> context() {
    return context(0);
  }

  private OperatorSelectionContext<TestSolution> context(long iteration) {
    TestSolution initialSolution = new TestSolution(100.0);
    return new OperatorSelectionContext<>(
        iteration, 0, initialSolution, initialSolution, mockRandomGenerator);
  }

  private OperatorSelectionFeedback<TestSolution> feedback(
      int destroyOperatorIndex, int repairOperatorIndex, SolutionStatus status) {
    return new OperatorSelectionFeedback<>(
        new OperatorSelection(destroyOperatorIndex, repairOperatorIndex),
        new TestSolution(100.0),
        status);
  }
}
