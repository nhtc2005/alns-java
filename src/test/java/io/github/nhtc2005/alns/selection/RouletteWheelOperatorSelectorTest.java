package io.github.nhtc2005.alns.selection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouletteWheelOperatorSelectorTest {

  @Mock private RandomGenerator mockRandomGenerator;

  @ParameterizedTest
  @CsvSource({"0, 2", "-1, 2", "2, 0", "2, -1"})
  void constructor_ShouldThrowException_WhenOperatorCountsAreInvalid(
      int destroyOperatorCount, int repairOperatorCount) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new RouletteWheelOperatorSelector<TestSolution>(
                    destroyOperatorCount, repairOperatorCount, new double[] {7, 3, 1, 0}, 0.5));

    assertEquals("Operator counts must be positive.", exception.getMessage());
  }

  @Test
  void constructor_ShouldThrowException_WhenScoresAreNull() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> new RouletteWheelOperatorSelector<TestSolution>(2, 2, null, 0.5));

    assertEquals("scores must not be null.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 3, 5})
  void constructor_ShouldThrowException_WhenScoreCountIsNotFour(int scoreCount) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new RouletteWheelOperatorSelector<TestSolution>(2, 2, new double[scoreCount], 0.5));

    assertEquals("scores must contain exactly four values.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void constructor_ShouldThrowException_WhenScoreIsInvalid(double score) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new RouletteWheelOperatorSelector<TestSolution>(
                    2, 2, new double[] {7, 3, 1, score}, 0.5));

    assertEquals("scores must be finite and non-negative.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void constructor_ShouldThrowException_WhenDecayIsInvalid(double decay) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new RouletteWheelOperatorSelector<TestSolution>(
                    2, 2, new double[] {7, 3, 1, 0}, decay));

    assertEquals("decay must be finite and in the range [0.0, 1.0].", exception.getMessage());
  }

  @Test
  void constructor_ShouldInitializeWeights_WhenResetIsOverridden() {
    RouletteWheelOperatorSelector<TestSolution> selector =
        assertDoesNotThrow(
            () ->
                new RouletteWheelOperatorSelector<TestSolution>(
                    2, 2, new double[] {7, 3, 1, 0}, 0.5) {
                  @Override
                  public void reset() {
                    throw new IllegalStateException("reset must not be called by the constructor.");
                  }
                });
    when(mockRandomGenerator.nextDouble()).thenReturn(0.75);

    assertEquals(new OperatorSelection(1, 1), selector.select(context()));
  }

  @Test
  void getScores_ShouldPreserveConfiguredScores_WhenCallerModifiesInputOrReturnedArray() {
    double[] scores = {7, 3, 1, 0};
    RouletteWheelOperatorSelector<TestSolution> selector =
        new RouletteWheelOperatorSelector<>(2, 2, scores, 0.0);
    scores[0] = 0;
    assertArrayEquals(new double[] {7, 3, 1, 0}, selector.getScores());
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    assertArrayEquals(new double[] {7, 1}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {7, 1}, selector.getRepairOperatorWeights());

    double[] returnedScores = selector.getScores();
    returnedScores[0] = 0;
    selector.update(feedback(1, 1, SolutionStatus.BEST));
    assertArrayEquals(new double[] {7, 7}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {7, 7}, selector.getRepairOperatorWeights());
  }

  @Test
  void select_ShouldUseUniformInitialWeights_WhenNoFeedbackHasBeenReceived() {
    RouletteWheelOperatorSelector<TestSolution> selector =
        new RouletteWheelOperatorSelector<>(3, 2, new double[] {7, 3, 1, 0}, 0.5);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.0, Math.nextDown(1.0));

    assertEquals(3, selector.getDestroyOperatorCount());
    assertEquals(2, selector.getRepairOperatorCount());
    assertEquals(0.5, selector.getDecay());
    assertEquals(new OperatorSelection(0, 1), selector.select(context()));
  }

  @ParameterizedTest
  @CsvSource({"0, 0.0, 1", "1, 0.999, 0"})
  void select_ShouldSkipZeroWeightOperators_WhenOtherWeightsArePositive(
      int zeroWeightIndex, double randomValue, int expectedIndex) {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(0.0);
    selector.update(feedback(zeroWeightIndex, zeroWeightIndex, SolutionStatus.REJECTED));
    when(mockRandomGenerator.nextDouble()).thenReturn(randomValue);

    assertEquals(new OperatorSelection(expectedIndex, expectedIndex), selector.select(context()));
  }

  @Test
  void select_ShouldFallBackToUniformSelection_WhenAllWeightsAreZero() {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(0.0);
    selector.update(feedback(0, 0, SolutionStatus.REJECTED));
    selector.update(feedback(1, 1, SolutionStatus.REJECTED));
    when(mockRandomGenerator.nextInt(2)).thenReturn(1, 0);

    assertEquals(new OperatorSelection(1, 0), selector.select(context()));
    verify(mockRandomGenerator, never()).nextDouble();
  }

  @Test
  void select_ShouldAvoidOverflow_WhenWeightsAreVeryLarge() {
    RouletteWheelOperatorSelector<TestSolution> selector =
        new RouletteWheelOperatorSelector<>(2, 2, new double[] {Double.MAX_VALUE, 3, 1, 0}, 0.0);
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    selector.update(feedback(1, 1, SolutionStatus.BEST));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.25, 0.75);

    assertEquals(new OperatorSelection(0, 1), selector.select(context()));
  }

  @Test
  void select_ShouldReturnLastPositiveIndex_WhenRandomValueReachesTotalWeight() {
    RouletteWheelOperatorSelector<TestSolution> selector =
        new RouletteWheelOperatorSelector<>(3, 3, new double[] {7, 3, 1, 0}, 0.0);
    selector.update(feedback(1, 1, SolutionStatus.BEST));
    selector.update(feedback(2, 2, SolutionStatus.REJECTED));
    // Deliberately outside [0.0, 1.0) to exercise the defensive fallback.
    when(mockRandomGenerator.nextDouble()).thenReturn(1.0);

    assertEquals(new OperatorSelection(1, 1), selector.select(context()));
  }

  @Test
  void select_ShouldUseInternalWeights_WhenCallerModifiesReturnedArrays() {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(0.5);
    selector.update(feedback(1, 0, SolutionStatus.BEST));

    double[] destroyWeights = selector.getDestroyOperatorWeights();
    double[] repairWeights = selector.getRepairOperatorWeights();
    assertArrayEquals(new double[] {1, 4}, destroyWeights);
    assertArrayEquals(new double[] {4, 1}, repairWeights);
    destroyWeights[1] = 0;
    repairWeights[0] = 0;
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertArrayEquals(new double[] {1, 4}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {4, 1}, selector.getRepairOperatorWeights());
    assertEquals(new OperatorSelection(1, 0), selector.select(context()));
  }

  @ParameterizedTest
  @CsvSource({
    "BEST, 0.79, 0", "BEST, 0.81, 1",
    "BETTER, 0.65, 0", "BETTER, 0.68, 1",
    "ACCEPTED, 0.49, 0", "ACCEPTED, 0.51, 1",
    "REJECTED, 0.32, 0", "REJECTED, 0.34, 1"
  })
  void update_ShouldApplyOutcomeScore_WhenFeedbackIsReceived(
      SolutionStatus status, double randomValue, int expectedIndex) {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(0.5);
    selector.update(feedback(0, 0, status));
    when(mockRandomGenerator.nextDouble()).thenReturn(randomValue);

    assertEquals(new OperatorSelection(expectedIndex, expectedIndex), selector.select(context()));
  }

  @Test
  void update_ShouldChangeOnlySelectedOperators_WhenDestroyAndRepairIndicesDiffer() {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(0.5);
    selector.update(feedback(1, 0, SolutionStatus.BEST));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.3);

    assertEquals(new OperatorSelection(1, 0), selector.select(context()));
  }

  @ParameterizedTest
  @EnumSource(
      value = SolutionStatus.class,
      names = {"BETTER", "ACCEPTED"})
  void update_ShouldApplyRejectedScoreToSelectedWeights_WhenFeedbackIsNotRewardEligible(
      SolutionStatus status) {
    RouletteWheelOperatorSelector<TestSolution> selector =
        new RouletteWheelOperatorSelector<>(2, 2, new double[] {7, 3, 1, 2}, 0.5);
    selector.update(feedback(1, 0, SolutionStatus.BEST));

    selector.update(
        new OperatorSelectionFeedback<>(
            new OperatorSelection(1, 0), new TestSolution(100.0), status, false));

    assertArrayEquals(new double[] {1, 3}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {3, 1}, selector.getRepairOperatorWeights());
  }

  @Test
  void update_ShouldApplyConfiguredRejectedScore_WhenRejectedScoreIsPositive() {
    RouletteWheelOperatorSelector<TestSolution> selector =
        new RouletteWheelOperatorSelector<>(2, 2, new double[] {7, 3, 1, 2}, 0.5);

    selector.update(feedback(1, 0, SolutionStatus.REJECTED));

    assertArrayEquals(new double[] {1, 1.5}, selector.getDestroyOperatorWeights());
    assertArrayEquals(new double[] {1.5, 1}, selector.getRepairOperatorWeights());
  }

  @Test
  void update_ShouldPreserveWeights_WhenDecayEqualsOne() {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(1.0);
    selector.update(feedback(0, 0, SolutionStatus.REJECTED));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.75);

    assertEquals(new OperatorSelection(1, 1), selector.select(context()));
  }

  @ParameterizedTest
  @CsvSource({"-1, 0", "2, 0", "0, -1", "0, 2"})
  void update_ShouldThrowExceptionWithoutChangingWeights_WhenOperatorIndexIsInvalid(
      int destroyOperatorIndex, int repairOperatorIndex) {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(0.0);

    assertThrows(
        IndexOutOfBoundsException.class,
        () ->
            selector.update(
                feedback(destroyOperatorIndex, repairOperatorIndex, SolutionStatus.BEST)));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.75);
    assertEquals(new OperatorSelection(1, 1), selector.select(context()));
  }

  @Test
  void reset_ShouldRestoreUniformWeights_WhenSelectorIsReused() {
    RouletteWheelOperatorSelector<TestSolution> selector = selector(0.0);
    selector.update(feedback(0, 0, SolutionStatus.BEST));
    when(mockRandomGenerator.nextDouble()).thenReturn(0.75);

    assertEquals(new OperatorSelection(0, 0), selector.select(context()));
    selector.reset();
    assertEquals(new OperatorSelection(1, 1), selector.select(context()));
  }

  private RouletteWheelOperatorSelector<TestSolution> selector(double decay) {
    return new RouletteWheelOperatorSelector<>(2, 2, new double[] {7, 3, 1, 0}, decay);
  }

  private OperatorSelectionContext<TestSolution> context() {
    TestSolution initialSolution = new TestSolution(100.0);
    return new OperatorSelectionContext<>(
        0, 0, initialSolution, initialSolution, mockRandomGenerator);
  }

  private OperatorSelectionFeedback<TestSolution> feedback(
      int destroyOperatorIndex, int repairOperatorIndex, SolutionStatus status) {
    return new OperatorSelectionFeedback<>(
        new OperatorSelection(destroyOperatorIndex, repairOperatorIndex),
        new TestSolution(100.0),
        status);
  }
}
