package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores objective histories, iteration runtimes, and outcome counts from one ALNS search.
 *
 * <p>Enabled histories and runtimes contain an entry for the initial solution followed by one entry
 * per completed iteration, so index {@code k} corresponds to iteration {@code k}; the initial
 * runtime is zero. Data getters throw {@link IllegalStateException} when collection is disabled.
 * Outcomes are counted independently of reward eligibility, and each operator in a selected pair
 * receives the pair's outcome. Retaining full histories uses memory proportional to iteration
 * count.
 */
public final class AlnsStatistics {

  private static final AlnsStatistics DISABLED = new AlnsStatistics(false, List.of(), List.of());

  private final boolean enabled;
  private final List<String> destroyOperatorNames;
  private final List<String> repairOperatorNames;
  private final List<Double> currentObjectiveValues = new ArrayList<>();
  private final List<Double> bestObjectiveValues = new ArrayList<>();
  private final List<Long> iterationRuntimesNanos = new ArrayList<>();
  private final long[] statusCounts = new long[SolutionStatus.values().length];
  private final long[][] destroyOperatorCounts;
  private final long[][] repairOperatorCounts;

  AlnsStatistics(int destroyOperatorCount, int repairOperatorCount) {
    this(
        Alns.defaultOperatorNames("destroy", destroyOperatorCount),
        Alns.defaultOperatorNames("repair", repairOperatorCount));
  }

  AlnsStatistics(List<String> destroyOperatorNames, List<String> repairOperatorNames) {
    this(true, destroyOperatorNames, repairOperatorNames);
  }

  private AlnsStatistics(
      boolean enabled, List<String> destroyOperatorNames, List<String> repairOperatorNames) {
    this.enabled = enabled;
    this.destroyOperatorNames = List.copyOf(destroyOperatorNames);
    this.repairOperatorNames = List.copyOf(repairOperatorNames);
    this.destroyOperatorCounts =
        new long[destroyOperatorNames.size()][SolutionStatus.values().length];
    this.repairOperatorCounts =
        new long[repairOperatorNames.size()][SolutionStatus.values().length];
  }

  static AlnsStatistics disabled() {
    return DISABLED;
  }

  void collectInitial(double currentObjectiveValue, double bestObjectiveValue) {
    currentObjectiveValues.add(currentObjectiveValue);
    bestObjectiveValues.add(bestObjectiveValue);
    iterationRuntimesNanos.add(0L);
  }

  void collectIteration(
      OperatorSelection operatorSelection,
      SolutionStatus status,
      double currentObjectiveValue,
      double bestObjectiveValue,
      long iterationRuntimeNanos) {
    int statusIndex = status.ordinal();
    long[] destroyCounts = destroyOperatorCounts[operatorSelection.destroyOperatorIndex()];
    long[] repairCounts = repairOperatorCounts[operatorSelection.repairOperatorIndex()];
    currentObjectiveValues.add(currentObjectiveValue);
    bestObjectiveValues.add(bestObjectiveValue);
    iterationRuntimesNanos.add(iterationRuntimeNanos);
    destroyCounts[statusIndex]++;
    repairCounts[statusIndex]++;
    statusCounts[statusIndex]++;
  }

  /**
   * Returns whether statistics collection was enabled.
   *
   * @return whether statistics collection was enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Returns the destroy operator names; position {@code i} names destroy operator {@code i}.
   *
   * @return an unmodifiable list of destroy operator names
   * @throws IllegalStateException if statistics collection is disabled
   */
  public List<String> getDestroyOperatorNames() {
    ensureEnabled();
    return destroyOperatorNames;
  }

  /**
   * Returns the repair operator names; position {@code i} names repair operator {@code i}.
   *
   * @return an unmodifiable list of repair operator names
   * @throws IllegalStateException if statistics collection is disabled
   */
  public List<String> getRepairOperatorNames() {
    ensureEnabled();
    return repairOperatorNames;
  }

  /**
   * Returns an unmodifiable snapshot of current objectives, including the initial value.
   *
   * @return an unmodifiable snapshot of current objectives, including the initial value
   * @throws IllegalStateException if statistics collection is disabled
   */
  public List<Double> getCurrentObjectiveValues() {
    ensureEnabled();
    return List.copyOf(currentObjectiveValues);
  }

  /**
   * Returns an unmodifiable snapshot of best objectives, including the initial value.
   *
   * @return an unmodifiable snapshot of best objectives, including the initial value
   * @throws IllegalStateException if statistics collection is disabled
   */
  public List<Double> getBestObjectiveValues() {
    ensureEnabled();
    return List.copyOf(bestObjectiveValues);
  }

  /**
   * Returns an unmodifiable snapshot of iteration runtimes in nanoseconds, including a zero entry
   * for the initial solution.
   *
   * <p>Timing includes selection, operators, local search, acceptance, tracking, and selector
   * feedback, but excludes stopping checks, statistics collection, and iteration callbacks.
   *
   * @return an unmodifiable snapshot of iteration runtimes in nanoseconds
   * @throws IllegalStateException if statistics collection is disabled
   */
  public List<Long> getIterationRuntimesNanos() {
    ensureEnabled();
    return List.copyOf(iterationRuntimesNanos);
  }

  /**
   * Returns how often a destroy operator participated in the specified outcome.
   *
   * @param operatorIndex the zero-based operator index
   * @param status the outcome to count
   * @return the number of iterations with that operator and outcome
   * @throws IllegalStateException if statistics collection is disabled
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws NullPointerException if {@code status} is null
   */
  public long getDestroyOperatorCount(int operatorIndex, SolutionStatus status) {
    ensureEnabled();
    Objects.checkIndex(operatorIndex, destroyOperatorCounts.length);
    Objects.requireNonNull(status, "status must not be null.");
    return destroyOperatorCounts[operatorIndex][status.ordinal()];
  }

  /**
   * Returns how often a repair operator participated in the specified outcome.
   *
   * @param operatorIndex the zero-based operator index
   * @param status the outcome to count
   * @return the number of iterations with that operator and outcome
   * @throws IllegalStateException if statistics collection is disabled
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws NullPointerException if {@code status} is null
   */
  public long getRepairOperatorCount(int operatorIndex, SolutionStatus status) {
    ensureEnabled();
    Objects.checkIndex(operatorIndex, repairOperatorCounts.length);
    Objects.requireNonNull(status, "status must not be null.");
    return repairOperatorCounts[operatorIndex][status.ordinal()];
  }

  /**
   * Returns the total number of iterations with the specified outcome.
   *
   * @param status the outcome to count
   * @return the outcome count
   * @throws IllegalStateException if statistics collection is disabled
   * @throws NullPointerException if {@code status} is null
   */
  public long getStatusCount(SolutionStatus status) {
    ensureEnabled();
    Objects.requireNonNull(status, "status must not be null.");
    return statusCounts[status.ordinal()];
  }

  private void ensureEnabled() {
    if (!enabled) {
      throw new IllegalStateException(
          "Statistics collection is disabled; enable it with collectStatistics(true).");
    }
  }
}
