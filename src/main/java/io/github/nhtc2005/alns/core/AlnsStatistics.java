package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores objective histories, iteration runtimes, and outcome counts from one ALNS search.
 *
 * <p>Enabled histories contain the initial objectives followed by one entry per completed
 * iteration; runtimes contain only iteration entries. Disabled statistics have empty histories and
 * zero totals. Outcomes are counted independently of reward eligibility, and each operator in a
 * selected pair receives the pair's outcome. Retaining full histories uses memory proportional to
 * iteration count.
 */
public final class AlnsStatistics {

  private static final AlnsStatistics DISABLED = new AlnsStatistics(false, 0, 0);

  private final boolean enabled;
  private final List<Double> currentObjectiveValues = new ArrayList<>();
  private final List<Double> bestObjectiveValues = new ArrayList<>();
  private final List<Long> iterationRuntimesNanos = new ArrayList<>();
  private final long[] statusCounts = new long[SolutionStatus.values().length];
  private final long[][] destroyOperatorCounts;
  private final long[][] repairOperatorCounts;

  AlnsStatistics(int destroyOperatorCount, int repairOperatorCount) {
    this(true, destroyOperatorCount, repairOperatorCount);
  }

  private AlnsStatistics(boolean enabled, int destroyOperatorCount, int repairOperatorCount) {
    this.enabled = enabled;
    this.destroyOperatorCounts = new long[destroyOperatorCount][SolutionStatus.values().length];
    this.repairOperatorCounts = new long[repairOperatorCount][SolutionStatus.values().length];
  }

  static AlnsStatistics disabled() {
    return DISABLED;
  }

  void collectInitial(double currentObjectiveValue, double bestObjectiveValue) {
    currentObjectiveValues.add(currentObjectiveValue);
    bestObjectiveValues.add(bestObjectiveValue);
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
   * Returns an unmodifiable snapshot of current objectives, including the initial value.
   *
   * @return an unmodifiable snapshot of current objectives, including the initial value
   */
  public List<Double> getCurrentObjectiveValues() {
    return List.copyOf(currentObjectiveValues);
  }

  /**
   * Returns an unmodifiable snapshot of best objectives, including the initial value.
   *
   * @return an unmodifiable snapshot of best objectives, including the initial value
   */
  public List<Double> getBestObjectiveValues() {
    return List.copyOf(bestObjectiveValues);
  }

  /**
   * Returns an unmodifiable snapshot of iteration runtimes in nanoseconds.
   *
   * <p>Timing includes selection, operators, local search, acceptance, tracking, and selector
   * feedback, but excludes stopping checks, statistics collection, and iteration callbacks.
   *
   * @return an unmodifiable snapshot of iteration runtimes in nanoseconds
   */
  public List<Long> getIterationRuntimesNanos() {
    return List.copyOf(iterationRuntimesNanos);
  }

  /**
   * Returns how often a destroy operator participated in the specified outcome.
   *
   * @param operatorIndex the zero-based operator index
   * @param status the outcome to count
   * @return the number of iterations with that operator and outcome
   * @throws IndexOutOfBoundsException if the index is invalid, including when statistics are
   *     disabled
   * @throws NullPointerException if {@code status} is null
   */
  public long getDestroyOperatorCount(int operatorIndex, SolutionStatus status) {
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
   * @throws IndexOutOfBoundsException if the index is invalid, including when statistics are
   *     disabled
   * @throws NullPointerException if {@code status} is null
   */
  public long getRepairOperatorCount(int operatorIndex, SolutionStatus status) {
    Objects.checkIndex(operatorIndex, repairOperatorCounts.length);
    Objects.requireNonNull(status, "status must not be null.");
    return repairOperatorCounts[operatorIndex][status.ordinal()];
  }

  /**
   * Returns the total number of iterations with the specified outcome.
   *
   * @param status the outcome to count
   * @return the outcome count, or zero when collection is disabled
   * @throws NullPointerException if {@code status} is null
   */
  public long getStatusCount(SolutionStatus status) {
    Objects.requireNonNull(status, "status must not be null.");
    return statusCounts[status.ordinal()];
  }
}
