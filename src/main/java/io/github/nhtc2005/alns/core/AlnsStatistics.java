package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

  public boolean isEnabled() {
    return enabled;
  }

  public List<Double> getCurrentObjectiveValues() {
    return List.copyOf(currentObjectiveValues);
  }

  public List<Double> getBestObjectiveValues() {
    return List.copyOf(bestObjectiveValues);
  }

  public List<Long> getIterationRuntimesNanos() {
    return List.copyOf(iterationRuntimesNanos);
  }

  public long getDestroyOperatorCount(int operatorIndex, SolutionStatus status) {
    Objects.checkIndex(operatorIndex, destroyOperatorCounts.length);
    Objects.requireNonNull(status, "status must not be null.");
    return destroyOperatorCounts[operatorIndex][status.ordinal()];
  }

  public long getRepairOperatorCount(int operatorIndex, SolutionStatus status) {
    Objects.checkIndex(operatorIndex, repairOperatorCounts.length);
    Objects.requireNonNull(status, "status must not be null.");
    return repairOperatorCounts[operatorIndex][status.ordinal()];
  }

  public long getStatusCount(SolutionStatus status) {
    Objects.requireNonNull(status, "status must not be null.");
    return statusCounts[status.ordinal()];
  }
}
