package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Objects;

public class SimulatedAnnealing<S extends Solution<S>> implements AcceptanceCriterion<S> {

  public enum CoolingMethod {
    LINEAR,
    EXPONENTIAL,
  }

  private final double startTemperature;
  private final double endTemperature;
  private final double step;
  private final CoolingMethod method;

  private double temperature;

  public SimulatedAnnealing(double startTemperature, double endTemperature, double step) {
    this(startTemperature, endTemperature, step, CoolingMethod.EXPONENTIAL);
  }

  public SimulatedAnnealing(
      double startTemperature, double endTemperature, double step, CoolingMethod method) {
    this.method = Objects.requireNonNull(method, "method must not be null.");

    if (!Double.isFinite(startTemperature)
        || startTemperature <= 0.0
        || !Double.isFinite(endTemperature)
        || endTemperature <= 0.0) {
      throw new IllegalArgumentException("Temperatures must be finite and strictly positive.");
    }

    if (startTemperature < endTemperature) {
      throw new IllegalArgumentException(
          "startTemperature must be greater than or equal to endTemperature.");
    }

    if (!Double.isFinite(step) || step <= 0.0) {
      throw new IllegalArgumentException("step must be finite and strictly positive.");
    }

    if (method == CoolingMethod.EXPONENTIAL && step >= 1.0) {
      throw new IllegalArgumentException("Exponential cooling requires step to be less than 1.0.");
    }

    this.startTemperature = startTemperature;
    this.endTemperature = endTemperature;
    this.step = step;
    this.temperature = startTemperature;
  }

  @Override
  public boolean shouldAccept(AcceptanceContext<S> context) {
    double currentObjectiveValue = context.currentSolution().getObjectiveValue();
    double candidateObjectiveValue = context.candidateSolution().getObjectiveValue();

    double probability = Math.exp((currentObjectiveValue - candidateObjectiveValue) / temperature);

    temperature =
        Math.max(
            endTemperature,
            method == CoolingMethod.LINEAR ? temperature - step : temperature * step);

    return probability >= context.randomGenerator().nextDouble();
  }

  @Override
  public void reset() {
    temperature = startTemperature;
  }

  public double getTemperature() {
    return temperature;
  }

  public static <S extends Solution<S>> SimulatedAnnealing<S> autofit(
      double initialObjectiveValue, double worse, double acceptProbability, long numIterations) {
    return autofit(
        initialObjectiveValue, worse, acceptProbability, numIterations, CoolingMethod.EXPONENTIAL);
  }

  public static <S extends Solution<S>> SimulatedAnnealing<S> autofit(
      double initialObjectiveValue,
      double worse,
      double acceptProbability,
      long numIterations,
      CoolingMethod method) {
    Objects.requireNonNull(method, "method must not be null.");

    if (!Double.isFinite(initialObjectiveValue) || initialObjectiveValue <= 0.0) {
      throw new IllegalArgumentException(
          "initialObjectiveValue must be finite and strictly positive.");
    }

    if (!Double.isFinite(worse) || worse <= 0.0 || worse > 1.0) {
      throw new IllegalArgumentException("worse must be in the range (0.0, 1.0].");
    }

    if (!Double.isFinite(acceptProbability)
        || acceptProbability <= 0.0
        || acceptProbability >= 1.0) {
      throw new IllegalArgumentException("acceptProbability must be in the range (0.0, 1.0).");
    }

    if (numIterations <= 0) {
      throw new IllegalArgumentException("numIterations must be positive.");
    }

    double startTemperature = -(worse * initialObjectiveValue) / Math.log(acceptProbability);

    if (!Double.isFinite(startTemperature) || startTemperature < 1.0) {
      throw new IllegalArgumentException(
          "Autofit requires a finite startTemperature greater than or equal to 1.0. "
              + "Use the constructor to configure a different endTemperature.");
    }

    double step =
        method == CoolingMethod.LINEAR
            ? (startTemperature - 1.0) / numIterations
            : Math.pow(1.0 / startTemperature, 1.0 / numIterations);

    return new SimulatedAnnealing<>(startTemperature, 1.0, step, method);
  }
}
