package io.github.nhtc2005.alns.acceptance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
class SimulatedAnnealingTest {

  @Mock private RandomGenerator mockRandomGenerator;

  @Test
  void constructor_ShouldThrowException_WhenMethodIsNull() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class, () -> new SimulatedAnnealing<>(100.0, 1.0, 0.1, null));

    assertEquals("method must not be null.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
    "0.0, 1.0, 0.1",
    "-10.0, 1.0, 0.1",
    "NaN, 1.0, 0.1",
    "Infinity, 1.0, 0.1",
    "-Infinity, 1.0, 0.1",
    "100.0, 0.0, 0.1",
    "100.0, -1.0, 0.1",
    "100.0, NaN, 0.1",
    "100.0, Infinity, 0.1",
    "100.0, -Infinity, 0.1"
  })
  void constructor_ShouldThrowException_WhenTemperaturesAreInvalid(
      double startTemperature, double endTemperature, double step) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SimulatedAnnealing<>(startTemperature, endTemperature, step));

    assertEquals("Temperatures must be finite and strictly positive.", exception.getMessage());
  }

  @Test
  void constructor_ShouldThrowException_WhenStartTemperatureIsLessThanEndTemperature() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new SimulatedAnnealing<>(50.0, 100.0, 0.1));

    assertEquals(
        "startTemperature must be greater than or equal to endTemperature.",
        exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void constructor_ShouldThrowException_WhenStepIsInvalid(double invalidStep) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SimulatedAnnealing<>(100.0, 1.0, invalidStep));

    assertEquals("step must be finite and strictly positive.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = {1.0, 1.5})
  void constructor_ShouldThrowException_WhenExponentialStepIsGreaterThanOrEqualToOne(
      double invalidStep) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new SimulatedAnnealing<>(
                    100.0, 1.0, invalidStep, SimulatedAnnealing.CoolingMethod.EXPONENTIAL));

    assertEquals("Exponential cooling requires step to be less than 1.0.", exception.getMessage());
  }

  @Test
  void constructor_ShouldUseExponentialCooling_WhenMethodIsNotSpecified() {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(100.0, 1.0, 0.5);

    assertEquals(100.0, sa.getStartTemperature());
    assertEquals(1.0, sa.getEndTemperature());
    assertEquals(0.5, sa.getStep());
    assertEquals(SimulatedAnnealing.CoolingMethod.EXPONENTIAL, sa.getMethod());
    assertEquals(100.0, sa.getTemperature());
  }

  @ParameterizedTest
  @EnumSource(SimulatedAnnealing.CoolingMethod.class)
  void constructor_ShouldCreateValidInstance_WhenStartTemperatureEqualsEndTemperature(
      SimulatedAnnealing.CoolingMethod method) {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(1.0, 1.0, 0.5, method);

    assertEquals(1.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void shouldAccept_ShouldAlwaysAccept_WhenCandidateIsBetter() {
    SimulatedAnnealing<TestSolution> sa =
        new SimulatedAnnealing<>(100.0, 1.0, 0.95, SimulatedAnnealing.CoolingMethod.EXPONENTIAL);

    AcceptanceContext<TestSolution> context = context(100.0, 90.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.99);

    assertTrue(sa.shouldAccept(context));
  }

  @ParameterizedTest
  @CsvSource({"0.2, true", "0.5, false"})
  void shouldAccept_ShouldUseAcceptanceProbability_WhenCandidateIsWorse(
      double randomValue, boolean expected) {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(10.0, 1.0, 0.5);

    AcceptanceContext<TestSolution> context = context(100.0, 110.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(randomValue);

    assertEquals(expected, sa.shouldAccept(context));
    assertEquals(5.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void shouldAccept_ShouldAcceptCandidate_WhenRandomValueEqualsProbability() {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(10.0, 1.0, 0.5);

    AcceptanceContext<TestSolution> context = context(100.0, 110.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(Math.exp(-1.0));

    assertTrue(sa.shouldAccept(context));
  }

  @Test
  void shouldAccept_ShouldUseTemperatureBeforeCooling_WhenCandidateIsWorse() {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(100.0, 1.0, 0.5);

    AcceptanceContext<TestSolution> context = context(100.0, 110.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.86);

    // exp(-10 / 100) > 0.86, but exp(-10 / 50) < 0.86.
    assertTrue(sa.shouldAccept(context));
    assertEquals(50.0, sa.getTemperature(), 1e-6);
    assertFalse(sa.shouldAccept(context));
    assertEquals(25.0, sa.getTemperature(), 1e-6);
  }

  @ParameterizedTest
  @CsvSource({"LINEAR, 10.0", "EXPONENTIAL, 0.9"})
  void shouldAccept_ShouldApplyCoolingSchedule_WhenCandidateIsEvaluated(
      SimulatedAnnealing.CoolingMethod method, double step) {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(100.0, 1.0, step, method);

    AcceptanceContext<TestSolution> context = context(100.0, 100.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertTrue(sa.shouldAccept(context));
    assertEquals(90.0, sa.getTemperature(), 1e-6);
  }

  @ParameterizedTest
  @CsvSource({"LINEAR, 8.0", "EXPONENTIAL, 0.1"})
  void shouldAccept_ShouldKeepEndTemperature_WhenCoolingWouldExceedLowerBound(
      SimulatedAnnealing.CoolingMethod method, double step) {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(10.0, 5.0, step, method);

    AcceptanceContext<TestSolution> context = context(100.0, 100.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    for (int iteration = 0; iteration < 3; iteration++) {
      assertTrue(sa.shouldAccept(context));
      assertEquals(5.0, sa.getTemperature(), 1e-6);
    }
  }

  @Test
  void reset_ShouldRestoreStartTemperature_WhenTemperatureHasDecreased() {
    SimulatedAnnealing<TestSolution> sa =
        new SimulatedAnnealing<>(100.0, 1.0, 10.0, SimulatedAnnealing.CoolingMethod.LINEAR);

    AcceptanceContext<TestSolution> context = context(100.0, 100.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    sa.shouldAccept(context);
    assertEquals(90.0, sa.getTemperature(), 1e-6);

    sa.reset();
    assertEquals(100.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void autofit_ShouldUseExponentialCoolingAndDefaultEndTemperature_WhenOptionalArgsAreOmitted() {
    double objective = -4.0 * Math.log(0.5);
    SimulatedAnnealing<TestSolution> sa = SimulatedAnnealing.autofit(objective, 1.0, 0.5, 2);
    AcceptanceContext<TestSolution> context = context(objective, objective);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertEquals(4.0, sa.getStartTemperature(), 1e-6);
    assertEquals(1.0, sa.getEndTemperature());
    assertEquals(0.5, sa.getStep(), 1e-6);
    assertEquals(SimulatedAnnealing.CoolingMethod.EXPONENTIAL, sa.getMethod());
    assertEquals(4.0, sa.getTemperature(), 1e-6);
    assertTrue(sa.shouldAccept(context));
    assertEquals(2.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void autofit_ShouldUseProvidedEndTemperature_WhenEndTemperatureIsSpecified() {
    double objective = -4.0 * Math.log(0.5);
    SimulatedAnnealing<TestSolution> sa = SimulatedAnnealing.autofit(objective, 1.0, 0.5, 2, 0.25);
    AcceptanceContext<TestSolution> context = context(objective, objective);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertEquals(4.0, sa.getStartTemperature(), 1e-6);
    assertEquals(0.25, sa.getEndTemperature());
    assertEquals(0.25, sa.getStep(), 1e-6);
    assertEquals(SimulatedAnnealing.CoolingMethod.EXPONENTIAL, sa.getMethod());
    assertEquals(4.0, sa.getTemperature(), 1e-6);
    assertTrue(sa.shouldAccept(context));
    assertEquals(1.0, sa.getTemperature(), 1e-6);
  }

  @ParameterizedTest
  @CsvSource({"LINEAR, 3.75, 12.25", "EXPONENTIAL, 0.5, 8.0"})
  void autofit_ShouldReachDefaultEndTemperature_WhenAllIterationsComplete(
      SimulatedAnnealing.CoolingMethod method, double expectedStep, double firstTemperature) {
    SimulatedAnnealing<TestSolution> sa =
        SimulatedAnnealing.autofit(-16.0 * Math.log(0.5), 1.0, 0.5, 4, method);
    AcceptanceContext<TestSolution> context = context(100.0, 100.0);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertEquals(method, sa.getMethod());
    assertEquals(16.0, sa.getStartTemperature(), 1e-6);
    assertEquals(1.0, sa.getEndTemperature());
    assertEquals(16.0, sa.getTemperature(), 1e-6);
    assertEquals(expectedStep, sa.getStep(), 1e-8);
    assertTrue(sa.shouldAccept(context));
    assertEquals(firstTemperature, sa.getTemperature(), 1e-6);
    for (int iteration = 1; iteration < 4; iteration++) {
      assertTrue(sa.shouldAccept(context));
    }
    assertEquals(1.0, sa.getTemperature(), 1e-6);
    assertTrue(sa.shouldAccept(context));
    assertEquals(1.0, sa.getTemperature(), 1e-6);
  }

  @ParameterizedTest
  @CsvSource({
    "LINEAR, 0.1, 0.11",
    "EXPONENTIAL, 0.1, 0.11",
    "LINEAR, -0.1, -0.09",
    "EXPONENTIAL, -0.1, -0.09"
  })
  void autofit_ShouldFitAcceptanceAndCooling_WhenObjectiveIsPositiveOrNegative(
      SimulatedAnnealing.CoolingMethod method, double objective, double candidateObjective) {
    SimulatedAnnealing<TestSolution> sa =
        SimulatedAnnealing.autofit(objective, 0.1, 0.5, 4, 0.001, method);
    double start = -0.01 / Math.log(0.5);
    AcceptanceContext<TestSolution> context = context(objective, candidateObjective);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.49, 0.51);

    assertEquals(start, sa.getStartTemperature(), 1e-15);
    assertEquals(0.001, sa.getEndTemperature());
    assertEquals(method, sa.getMethod());
    assertTrue(sa.shouldAccept(context));
    sa.reset();
    assertFalse(sa.shouldAccept(context));
    for (int i = 1; i < 4; i++) {
      sa.shouldAccept(context);
    }
    assertEquals(0.001, sa.getTemperature(), 1e-15);
    sa.shouldAccept(context);
    assertEquals(0.001, sa.getTemperature(), 1e-15);
  }

  @Test
  void autofit_ShouldThrowException_WhenMethodIsNull() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> SimulatedAnnealing.autofit(100.0, 0.1, 0.5, 1000, null));

    assertEquals("method must not be null.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {0.0, -0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void autofit_ShouldThrowException_WhenInitialObjectiveValueIsInvalid(double initialValue) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(initialValue, 0.1, 0.5, 1000));

    assertEquals("initialObjectiveValue must be finite and non-zero.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {0.0, -0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void autofit_ShouldThrowException_WhenWorseIsInvalid(double worse) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(100.0, worse, 0.5, 1000));

    assertEquals("worse must be in the range (0.0, 1.0].", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {
        0.0,
        -0.1,
        1.0,
        1.5,
        Double.NaN,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY
      })
  void autofit_ShouldThrowException_WhenAcceptProbabilityIsInvalid(double prob) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(100.0, 0.1, prob, 1000));

    assertEquals("acceptProbability must be in the range (0.0, 1.0).", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -10})
  void autofit_ShouldThrowException_WhenNumIterationsIsNonPositive(long iterations) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(100.0, 0.1, 0.5, iterations));

    assertEquals("numIterations must be positive.", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
  void autofit_ShouldThrowException_WhenEndTemperatureIsInvalid(double endTemperature) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(-100.0, 0.1, 0.5, 10, endTemperature));

    assertEquals("endTemperature must be finite and strictly positive.", exception.getMessage());
  }

  @Test
  void autofit_ShouldThrowException_WhenCalculatedStartTemperatureOverflows() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(Double.MAX_VALUE, 1.0, 0.5, 1000));

    assertEquals(
        "Autofit requires a finite startTemperature strictly greater than endTemperature.",
        exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"LINEAR, 1.0", "LINEAR, 2.0", "EXPONENTIAL, 1.0", "EXPONENTIAL, 2.0"})
  void autofit_ShouldThrowException_WhenStartTemperatureIsAtOrBelowEndTemperature(
      SimulatedAnnealing.CoolingMethod method, double endToStartRatio) {
    double endTemperature = -10.0 / Math.log(0.5) * endToStartRatio;

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(-100.0, 0.1, 0.5, 10, endTemperature, method));

    assertEquals(
        "Autofit requires a finite startTemperature strictly greater than endTemperature.",
        exception.getMessage());
  }

  private AcceptanceContext<TestSolution> context(
      double currentObjective, double candidateObjective) {
    TestSolution current = new TestSolution(currentObjective);
    return new AcceptanceContext<>(
        0, 0, current, current, new TestSolution(candidateObjective), mockRandomGenerator);
  }
}
