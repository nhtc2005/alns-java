package io.github.nhtc2005.alns.acceptance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.github.nhtc2005.alns.util.TestSolution;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulatedAnnealingTest {

  @Mock private RandomGenerator mockRandomGenerator;
  @Mock private AcceptanceContext<TestSolution> mockContext;
  @Mock private TestSolution mockCurrentSolution;
  @Mock private TestSolution mockCandidateSolution;

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
  void constructor_ShouldThrowException_WhenStartTempLessThanEndTemp() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new SimulatedAnnealing<>(50.0, 100.0, 0.1));
    assertEquals(
        "startTemperature must be greater than or equal to endTemperature.",
        exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0.0", "-1.0", "NaN", "Infinity", "-Infinity"})
  void constructor_ShouldThrowException_WhenStepIsInvalid(double invalidStep) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new SimulatedAnnealing<>(100.0, 1.0, invalidStep));
    assertEquals("step must be finite and strictly positive.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"1.0", "1.5"})
  void constructor_ShouldThrowException_WhenExponentialStepIsGreaterOrEqualToOne(
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

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertEquals(100.0, sa.getStartTemperature(), 1e-6);
    assertEquals(1.0, sa.getEndTemperature(), 1e-6);
    assertEquals(0.5, sa.getStep(), 1e-6);
    assertEquals(SimulatedAnnealing.CoolingMethod.EXPONENTIAL, sa.getMethod());
    assertEquals(100.0, sa.getTemperature(), 1e-6);
    assertTrue(sa.shouldAccept(mockContext));
    assertEquals(50.0, sa.getTemperature(), 1e-6);
    assertEquals(100.0, sa.getStartTemperature(), 1e-6);
  }

  @ParameterizedTest
  @EnumSource(SimulatedAnnealing.CoolingMethod.class)
  void constructor_ShouldCreateValidInstance_WhenStartTempEqualsEndTemp(
      SimulatedAnnealing.CoolingMethod method) {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(1.0, 1.0, 0.5, method);

    assertEquals(1.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void shouldAccept_ShouldAlwaysAccept_WhenCandidateIsBetter() {
    SimulatedAnnealing<TestSolution> sa =
        new SimulatedAnnealing<>(100.0, 1.0, 0.95, SimulatedAnnealing.CoolingMethod.EXPONENTIAL);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(90.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.99);

    assertTrue(sa.shouldAccept(mockContext));
  }

  @ParameterizedTest
  @CsvSource({"0.2, true", "0.5, false"})
  void shouldAccept_ShouldUseAcceptanceProbability_WhenCandidateIsWorse(
      double randomValue, boolean expected) {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(10.0, 1.0, 0.5);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(110.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(randomValue);

    assertEquals(expected, sa.shouldAccept(mockContext));
    assertEquals(5.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void shouldAccept_ShouldAccept_WhenRandomValueEqualsProbability() {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(10.0, 1.0, 0.5);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(110.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(Math.exp(-1.0));

    assertTrue(sa.shouldAccept(mockContext));
  }

  @Test
  void shouldAccept_ShouldUseTemperatureBeforeCooling_WhenCandidateIsWorse() {
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(100.0, 1.0, 0.5);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(110.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.86);

    // exp(-10 / 100) > 0.86, but exp(-10 / 50) < 0.86.
    assertTrue(sa.shouldAccept(mockContext));
    assertEquals(50.0, sa.getTemperature(), 1e-6);
    assertFalse(sa.shouldAccept(mockContext));
    assertEquals(25.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void shouldAccept_ShouldCoolDownLinearly_WhenLinearMethodUsed() {
    double startTemp = 100.0;
    double step = 10.0;
    SimulatedAnnealing<TestSolution> sa =
        new SimulatedAnnealing<>(startTemp, 1.0, step, SimulatedAnnealing.CoolingMethod.LINEAR);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertTrue(sa.shouldAccept(mockContext));

    assertEquals(90.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void shouldAccept_ShouldCoolDownExponentially_WhenExponentialMethodUsed() {
    double startTemp = 100.0;
    double step = 0.9;
    SimulatedAnnealing<TestSolution> sa =
        new SimulatedAnnealing<>(
            startTemp, 1.0, step, SimulatedAnnealing.CoolingMethod.EXPONENTIAL);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertTrue(sa.shouldAccept(mockContext));

    assertEquals(90.0, sa.getTemperature(), 1e-6);
  }

  @ParameterizedTest
  @EnumSource(SimulatedAnnealing.CoolingMethod.class)
  void shouldAccept_ShouldNotCoolBelowEndTemperature(SimulatedAnnealing.CoolingMethod method) {
    double step = method == SimulatedAnnealing.CoolingMethod.LINEAR ? 8.0 : 0.1;
    SimulatedAnnealing<TestSolution> sa = new SimulatedAnnealing<>(10.0, 5.0, step, method);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    for (int iteration = 0; iteration < 3; iteration++) {
      assertTrue(sa.shouldAccept(mockContext));
      assertEquals(5.0, sa.getTemperature(), 1e-6);
    }
  }

  @Test
  void reset_ShouldResetTemperatureToStartTemperature() {
    SimulatedAnnealing<TestSolution> sa =
        new SimulatedAnnealing<>(100.0, 1.0, 10.0, SimulatedAnnealing.CoolingMethod.LINEAR);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    sa.shouldAccept(mockContext);
    assertEquals(90.0, sa.getTemperature(), 1e-6);

    sa.reset();
    assertEquals(100.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void autofit_ShouldUseExponentialCooling_WhenMethodIsNotSpecified() {
    SimulatedAnnealing<TestSolution> sa =
        SimulatedAnnealing.autofit(-4.0 * Math.log(0.5), 1.0, 0.5, 2);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertEquals(4.0, sa.getStartTemperature(), 1e-6);
    assertEquals(1.0, sa.getEndTemperature(), 1e-6);
    assertEquals(0.5, sa.getStep(), 1e-6);
    assertEquals(SimulatedAnnealing.CoolingMethod.EXPONENTIAL, sa.getMethod());
    assertEquals(4.0, sa.getTemperature(), 1e-6);
    assertTrue(sa.shouldAccept(mockContext));
    assertEquals(2.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void autofit_ShouldCreateValidInstance_WhenLinearMethodUsed() {
    SimulatedAnnealing<TestSolution> sa =
        SimulatedAnnealing.autofit(100.0, 0.1, 0.5, 1000, SimulatedAnnealing.CoolingMethod.LINEAR);

    assertNotNull(sa);
    assertEquals(SimulatedAnnealing.CoolingMethod.LINEAR, sa.getMethod());
    assertEquals(0.01342695, sa.getStep(), 1e-8);
    // startTemp = -(0.1 * 100) / ln(0.5) = -10 / -0.693147 = 14.42695
    assertEquals(14.42695, sa.getTemperature(), 1e-4);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertTrue(sa.shouldAccept(mockContext));
    assertEquals(14.413523, sa.getTemperature(), 1e-6);

    for (int iteration = 1; iteration < 1000; iteration++) {
      assertTrue(sa.shouldAccept(mockContext));
    }
    assertEquals(1.0, sa.getTemperature(), 1e-6);
    assertTrue(sa.shouldAccept(mockContext));
    assertEquals(1.0, sa.getTemperature(), 1e-6);
  }

  @Test
  void autofit_ShouldCreateValidInstance_WhenExponentialMethodUsed() {
    SimulatedAnnealing<TestSolution> sa =
        SimulatedAnnealing.autofit(
            100.0, 0.1, 0.5, 1000, SimulatedAnnealing.CoolingMethod.EXPONENTIAL);

    assertNotNull(sa);
    assertEquals(14.42695, sa.getTemperature(), 1e-4);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.5);

    assertTrue(sa.shouldAccept(mockContext));
    assertEquals(14.388495, sa.getTemperature(), 1e-6);

    for (int iteration = 1; iteration < 1000; iteration++) {
      assertTrue(sa.shouldAccept(mockContext));
    }
    assertEquals(1.0, sa.getTemperature(), 1e-6);
    assertTrue(sa.shouldAccept(mockContext));
    assertEquals(1.0, sa.getTemperature(), 1e-6);
  }

  @ParameterizedTest
  @CsvSource({"0.49, true", "0.51, false"})
  void autofit_ShouldUseInitialAcceptanceProbability_WhenCandidateIsWorse(
      double randomValue, boolean expected) {
    SimulatedAnnealing<TestSolution> sa = SimulatedAnnealing.autofit(100.0, 0.1, 0.5, 1000);

    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(110.0);
    when(mockContext.randomGenerator()).thenReturn(mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(randomValue);

    assertEquals(expected, sa.shouldAccept(mockContext));
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
  @CsvSource({"0.0", "-0.0", "NaN", "Infinity", "-Infinity"})
  void autofit_ShouldThrowException_WhenInitialObjectiveValueIsInvalid(double initialValue) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(initialValue, 0.1, 0.5, 1000));
    assertEquals(
        "initialObjectiveValue must be finite and non-zero.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0.0", "-0.1", "1.1", "NaN", "Infinity", "-Infinity"})
  void autofit_ShouldThrowException_WhenWorseIsInvalid(double worse) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(100.0, worse, 0.5, 1000));
    assertEquals("worse must be in the range (0.0, 1.0].", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0.0", "-0.1", "1.0", "1.5", "NaN", "Infinity", "-Infinity"})
  void autofit_ShouldThrowException_WhenAcceptProbabilityIsInvalid(double prob) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(100.0, 0.1, prob, 1000));
    assertEquals("acceptProbability must be in the range (0.0, 1.0).", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"0", "-10"})
  void autofit_ShouldThrowException_WhenNumIterationsIsNonPositive(long iterations) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(100.0, 0.1, 0.5, iterations));
    assertEquals("numIterations must be positive.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"1.0, 0.1, 0.5", "1.7976931348623157E308, 1.0, 0.5"})
  void autofit_ShouldThrowException_WhenCalculatedStartTemperatureIsInvalid(
      double initialValue, double worse, double prob) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(initialValue, worse, prob, 1000));
    assertEquals(
        "Autofit requires a finite startTemperature strictly greater than endTemperature.",
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(SimulatedAnnealing.CoolingMethod.class)
  void autofit_ShouldFitNegativeObjectiveAndCoolToConfiguredEnd(
      SimulatedAnnealing.CoolingMethod method) {
    SimulatedAnnealing<TestSolution> sa =
        SimulatedAnnealing.autofit(-0.1, 0.1, 0.5, 20, 0.001, method);
    double start = -0.01 / Math.log(0.5);
    assertEquals(start, sa.getStartTemperature(), 1e-15);
    assertEquals(0.001, sa.getEndTemperature());
    assertEquals(method, sa.getMethod());

    AcceptanceContext<TestSolution> context =
        new AcceptanceContext<>(
            0,
            0,
            new TestSolution(-0.1),
            new TestSolution(-0.1),
            new TestSolution(-0.09),
            mockRandomGenerator);
    when(mockRandomGenerator.nextDouble()).thenReturn(0.49, 0.51);
    assertTrue(sa.shouldAccept(context));
    sa.reset();
    assertFalse(sa.shouldAccept(context));
    for (int i = 1; i < 20; i++) {
      sa.shouldAccept(context);
    }
    assertEquals(0.001, sa.getTemperature(), 1e-15);
    sa.shouldAccept(context);
    assertEquals(0.001, sa.getTemperature(), 1e-15);
  }

  @Test
  void autofit_ShouldUseExponentialCooling_WhenOnlyEndTemperatureIsSpecified() {
    SimulatedAnnealing<TestSolution> sa =
        SimulatedAnnealing.autofit(-100.0, 0.1, 0.5, 10, 0.01);
    assertEquals(SimulatedAnnealing.CoolingMethod.EXPONENTIAL, sa.getMethod());
    assertEquals(0.01, sa.getEndTemperature());
  }

  @ParameterizedTest
  @CsvSource({"0.0", "-1.0", "NaN", "Infinity", "-Infinity"})
  void autofit_ShouldRejectInvalidEndTemperature(double endTemperature) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> SimulatedAnnealing.autofit(-100.0, 0.1, 0.5, 10, endTemperature));
    assertEquals("endTemperature must be finite and strictly positive.", exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(SimulatedAnnealing.CoolingMethod.class)
  void autofit_ShouldRejectStartAtOrBelowEnd(SimulatedAnnealing.CoolingMethod method) {
    double start = -10.0 / Math.log(0.5);
    for (double end : new double[] {start, start * 2}) {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> SimulatedAnnealing.autofit(-100.0, 0.1, 0.5, 10, end, method));
      assertEquals(
          "Autofit requires a finite startTemperature strictly greater than endTemperature.",
          exception.getMessage());
    }
  }
}
