package io.github.nhtc2005.alns.acceptance;

import io.github.nhtc2005.alns.model.Solution;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accepts candidates probabilistically using simulated annealing for minimization.
 *
 * <p>For objective difference \(\Delta = f(s_{\mathrm{candidate}}) - f(s_{\mathrm{current}})\) and
 * temperature \(T\), the acceptance probability is:
 *
 * <p>\[ P(\mathrm{accept}) = \exp\!\left(-\frac{\max(0, \Delta)}{T}\right). \]
 *
 * <p>Each evaluation uses the current temperature, then cools it, regardless of acceptance.
 * Temperature is bounded below by the configured end temperature; reaching it does not stop the
 * search. Instances maintain mutable temperature state and must not be shared by concurrent runs.
 *
 * @param <S> the solution type
 */
public class SimulatedAnnealing<S extends Solution<S>> implements AcceptanceCriterion<S> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedAnnealing.class);

  /** Defines how temperature changes after each acceptance evaluation. */
  public enum CoolingMethod {
    /** Subtracts a fixed step: \(T_{k+1} = \max(T_{\mathrm{end}}, T_k - d)\). */
    LINEAR,
    /** Multiplies by a fixed factor: \(T_{k+1} = \max(T_{\mathrm{end}}, T_k r)\). */
    EXPONENTIAL,
  }

  private final double startTemperature;
  private final double endTemperature;
  private final double step;
  private final CoolingMethod method;

  private double temperature;

  /**
   * Creates a criterion with exponential cooling.
   *
   * @param startTemperature the finite positive initial temperature; at least {@code
   *     endTemperature}
   * @param endTemperature the finite positive minimum temperature
   * @param step the cooling factor in {@code (0, 1)}
   * @throws IllegalArgumentException if a temperature or step is invalid
   */
  public SimulatedAnnealing(double startTemperature, double endTemperature, double step) {
    this(startTemperature, endTemperature, step, CoolingMethod.EXPONENTIAL);
  }

  /**
   * Creates a criterion with the specified cooling schedule.
   *
   * @param startTemperature the finite positive initial temperature; at least {@code
   *     endTemperature}
   * @param endTemperature the finite positive minimum temperature
   * @param step the finite positive decrement for linear cooling, or factor in {@code (0, 1)} for
   *     exponential cooling
   * @param method the cooling method
   * @throws IllegalArgumentException if a temperature or step is invalid
   * @throws NullPointerException if {@code method} is null
   */
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

  /**
   * {@inheritDoc}
   *
   * <p>Uses the temperature before cooling and advances the cooling schedule once per call.
   */
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

  /** Restores the temperature to its configured initial value. */
  @Override
  public void reset() {
    temperature = startTemperature;
  }

  /**
   * Returns the configured initial temperature.
   *
   * @return the configured initial temperature
   */
  public double getStartTemperature() {
    return startTemperature;
  }

  /**
   * Returns the configured minimum temperature.
   *
   * @return the configured minimum temperature
   */
  public double getEndTemperature() {
    return endTemperature;
  }

  /**
   * Returns the linear decrement or exponential cooling factor.
   *
   * @return the linear decrement or exponential cooling factor
   */
  public double getStep() {
    return step;
  }

  /**
   * Returns the configured cooling method.
   *
   * @return the configured cooling method
   */
  public CoolingMethod getMethod() {
    return method;
  }

  /**
   * Returns the temperature for the next acceptance evaluation.
   *
   * @return the temperature for the next acceptance evaluation
   */
  public double getTemperature() {
    return temperature;
  }

  /**
   * Fits exponential cooling with an end temperature of {@code 1.0}.
   *
   * <p>See {@link #autofit(double, double, double, long, double, CoolingMethod)} for the fitting
   * formulas and validation rules.
   *
   * @param <S> the solution type
   * @param initialObjectiveValue the finite non-zero initial objective; may be negative
   * @param worse the worsening fraction in {@code (0, 1]}; {@code 0.1} means 10% of the objective
   *     magnitude
   * @param acceptProbability the initial probability of accepting that worsening, in {@code (0, 1)}
   * @param numIterations the positive number of cooling updates
   * @return a criterion initialized with the fitted temperature and step
   * @throws IllegalArgumentException if inputs are invalid or the fitted temperature or step is
   *     invalid
   */
  public static <S extends Solution<S>> SimulatedAnnealing<S> autofit(
      double initialObjectiveValue, double worse, double acceptProbability, long numIterations) {
    return autofit(
        initialObjectiveValue, worse, acceptProbability, numIterations, CoolingMethod.EXPONENTIAL);
  }

  /**
   * Fits the specified cooling method with an end temperature of {@code 1.0}.
   *
   * <p>See {@link #autofit(double, double, double, long, double, CoolingMethod)} for the fitting
   * formulas and validation rules.
   *
   * @param <S> the solution type
   * @param initialObjectiveValue the finite non-zero initial objective; may be negative
   * @param worse the worsening fraction in {@code (0, 1]}; {@code 0.1} means 10% of the objective
   *     magnitude
   * @param acceptProbability the initial probability of accepting that worsening, in {@code (0, 1)}
   * @param numIterations the positive number of cooling updates
   * @param method the cooling method
   * @return a criterion initialized with the fitted temperature and step
   * @throws IllegalArgumentException if inputs are invalid or the fitted temperature or step is
   *     invalid
   * @throws NullPointerException if {@code method} is null
   */
  public static <S extends Solution<S>> SimulatedAnnealing<S> autofit(
      double initialObjectiveValue,
      double worse,
      double acceptProbability,
      long numIterations,
      CoolingMethod method) {
    return autofit(initialObjectiveValue, worse, acceptProbability, numIterations, 1.0, method);
  }

  /**
   * Fits exponential cooling with the specified end temperature.
   *
   * <p>See {@link #autofit(double, double, double, long, double, CoolingMethod)} for the fitting
   * formulas and validation rules.
   *
   * @param <S> the solution type
   * @param initialObjectiveValue the finite non-zero initial objective; may be negative
   * @param worse the worsening fraction in {@code (0, 1]}; {@code 0.1} means 10% of the objective
   *     magnitude
   * @param acceptProbability the initial probability of accepting that worsening, in {@code (0, 1)}
   * @param numIterations the positive number of cooling updates
   * @param endTemperature the finite positive minimum temperature
   * @return a criterion initialized with the fitted temperature and step
   * @throws IllegalArgumentException if inputs are invalid or the fitted temperature or step is
   *     invalid
   */
  public static <S extends Solution<S>> SimulatedAnnealing<S> autofit(
      double initialObjectiveValue,
      double worse,
      double acceptProbability,
      long numIterations,
      double endTemperature) {
    return autofit(
        initialObjectiveValue,
        worse,
        acceptProbability,
        numIterations,
        endTemperature,
        CoolingMethod.EXPONENTIAL);
  }

  /**
   * Fits a cooling schedule to an initial acceptance probability.
   *
   * <p>For initial objective \(f_0\), worsening fraction \(w\), and target probability \(p\):
   *
   * <p>\[ \Delta = w|f_0|, \qquad T_0 = -\frac{\Delta}{\ln p}. \]
   *
   * <p>The candidate objective used for fitting is \(f_0 + \Delta\), including when \(f_0\) is
   * negative. A zero initial objective cannot define a relative worsening and is rejected.
   *
   * <p>For \(N\) evaluations and minimum temperature \(T_e\), the fitted step is:
   *
   * <p>\[ d = \frac{T_0 - T_e}{N} \quad\text{(linear)}, \qquad r =
   * \left(\frac{T_e}{T_0}\right)^{1/N} \quad\text{(exponential)}. \]
   *
   * <p>The temperature reaches \(T_e\) after \(N\) updates, subject to floating-point rounding. The
   * fitted \(T_0\) must be strictly greater than \(T_e\).
   *
   * @param <S> the solution type
   * @param initialObjectiveValue the finite non-zero initial objective; may be negative
   * @param worse the worsening fraction in {@code (0, 1]}; {@code 0.1} means 10% of the objective
   *     magnitude
   * @param acceptProbability the initial probability of accepting that worsening, in {@code (0, 1)}
   * @param numIterations the positive number of cooling updates
   * @param endTemperature the finite positive minimum temperature
   * @param method the cooling method
   * @return a criterion initialized with the fitted temperature and step
   * @throws IllegalArgumentException if inputs are invalid or the fitted temperature or step is
   *     invalid
   * @throws NullPointerException if {@code method} is null
   */
  public static <S extends Solution<S>> SimulatedAnnealing<S> autofit(
      double initialObjectiveValue,
      double worse,
      double acceptProbability,
      long numIterations,
      double endTemperature,
      CoolingMethod method) {
    Objects.requireNonNull(method, "method must not be null.");

    if (!Double.isFinite(initialObjectiveValue) || initialObjectiveValue == 0.0) {
      throw new IllegalArgumentException("initialObjectiveValue must be finite and non-zero.");
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

    if (!Double.isFinite(endTemperature) || endTemperature <= 0.0) {
      throw new IllegalArgumentException("endTemperature must be finite and strictly positive.");
    }

    double startTemperature =
        -(worse * Math.abs(initialObjectiveValue)) / Math.log(acceptProbability);

    if (!Double.isFinite(startTemperature) || startTemperature <= endTemperature) {
      throw new IllegalArgumentException(
          "Autofit requires a finite startTemperature strictly greater than endTemperature.");
    }

    double step =
        method == CoolingMethod.LINEAR
            ? (startTemperature - endTemperature) / numIterations
            : Math.exp((Math.log(endTemperature) - Math.log(startTemperature)) / numIterations);

    SimulatedAnnealing<S> criterion =
        new SimulatedAnnealing<>(startTemperature, endTemperature, step, method);
    LOGGER.debug(
        "Fitted simulated annealing: startTemperature={}, endTemperature={}, step={}, method={}",
        startTemperature,
        endTemperature,
        step,
        method);
    return criterion;
  }
}
