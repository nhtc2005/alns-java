package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.acceptance.AcceptanceContext;
import io.github.nhtc2005.alns.acceptance.AcceptanceCriterion;
import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.operator.DestroyOperator;
import io.github.nhtc2005.alns.operator.RepairOperator;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import io.github.nhtc2005.alns.selection.OperatorSelectionContext;
import io.github.nhtc2005.alns.selection.OperatorSelectionFeedback;
import io.github.nhtc2005.alns.selection.OperatorSelector;
import io.github.nhtc2005.alns.selection.RandomOperatorSelector;
import io.github.nhtc2005.alns.stopping.StoppingContext;
import io.github.nhtc2005.alns.stopping.StoppingCriterion;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class Alns<S extends Solution<S>> {

  private final RandomGenerator randomGenerator;
  private final List<DestroyOperator<S>> destroyOperators;
  private final List<RepairOperator<S>> repairOperators;
  private final OperatorSelector<S> operatorSelector;
  private final boolean collectStatistics;

  public Alns(
      RandomGenerator randomGenerator,
      List<DestroyOperator<S>> destroyOperators,
      List<RepairOperator<S>> repairOperators,
      OperatorSelector<S> operatorSelector) {
    this(randomGenerator, destroyOperators, repairOperators, operatorSelector, false);
  }

  public Alns(
      RandomGenerator randomGenerator,
      List<DestroyOperator<S>> destroyOperators,
      List<RepairOperator<S>> repairOperators,
      OperatorSelector<S> operatorSelector,
      boolean collectStatistics) {
    this.randomGenerator =
        Objects.requireNonNull(randomGenerator, "randomGenerator must not be null.");
    this.destroyOperators = copyNonEmpty(destroyOperators, "destroyOperators");
    this.repairOperators = copyNonEmpty(repairOperators, "repairOperators");
    this.operatorSelector =
        Objects.requireNonNull(operatorSelector, "operatorSelector must not be null.");
    this.collectStatistics = collectStatistics;
  }

  public static <S extends Solution<S>> Builder<S> builder() {
    return new Builder<>();
  }

  public AlnsResult<S> solve(
      S initialSolution,
      AcceptanceCriterion<S> acceptanceCriterion,
      StoppingCriterion<S> stoppingCriterion) {
    Objects.requireNonNull(initialSolution, "initialSolution must not be null.");
    Objects.requireNonNull(acceptanceCriterion, "acceptanceCriterion must not be null.");
    Objects.requireNonNull(stoppingCriterion, "stoppingCriterion must not be null.");

    if (!initialSolution.isFeasible()) {
      throw new IllegalArgumentException("initialSolution must be feasible.");
    }

    operatorSelector.reset();
    acceptanceCriterion.reset();
    stoppingCriterion.reset();

    long startedAt = System.nanoTime();
    long iteration = 0;
    S currentSolution = copyOf(initialSolution, "initialSolution.copy() must not return null.");
    S bestSolution = copyOf(currentSolution, "Solution.copy() must not return null.");
    AlnsStatistics statistics =
        collectStatistics
            ? new AlnsStatistics(destroyOperators.size(), repairOperators.size())
            : AlnsStatistics.disabled();
    if (collectStatistics) {
      statistics.collectInitial(
          currentSolution.getObjectiveValue(), bestSolution.getObjectiveValue());
    }

    while (!stoppingCriterion.shouldStop(
        new StoppingContext<>(
            iteration,
            System.nanoTime() - startedAt,
            bestSolution,
            currentSolution,
            randomGenerator))) {
      long iterationStartedAt = System.nanoTime();
      long elapsedNanos = iterationStartedAt - startedAt;
      OperatorSelection operatorSelection =
          Objects.requireNonNull(
              operatorSelector.select(
                  new OperatorSelectionContext<>(
                      iteration, elapsedNanos, bestSolution, currentSolution, randomGenerator)),
              "operatorSelector.select(...) must not return null.");
      validateOperatorSelection(operatorSelection);

      S destroyedSolution =
          Objects.requireNonNull(
              destroyOperators
                  .get(operatorSelection.destroyOperatorIndex())
                  .apply(
                      copyOf(currentSolution, "Solution.copy() must not return null."),
                      randomGenerator),
              "DestroyOperator.apply(...) must not return null.");
      S candidateSolution =
          Objects.requireNonNull(
              repairOperators
                  .get(operatorSelection.repairOperatorIndex())
                  .apply(destroyedSolution, randomGenerator),
              "RepairOperator.apply(...) must not return null.");

      SolutionStatus status;
      boolean isFeasible = candidateSolution.isFeasible();
      boolean isAccepted =
          acceptanceCriterion.shouldAccept(
              new AcceptanceContext<>(
                  iteration,
                  elapsedNanos,
                  bestSolution,
                  currentSolution,
                  candidateSolution,
                  randomGenerator));

      if (isFeasible && isBetter(candidateSolution, bestSolution)) {
        currentSolution = candidateSolution;
        bestSolution = copyOf(currentSolution, "Solution.copy() must not return null.");
        status = SolutionStatus.BEST;
      } else if (isFeasible && isAccepted) {
        status =
            isBetter(candidateSolution, currentSolution)
                ? SolutionStatus.BETTER
                : SolutionStatus.ACCEPTED;
        currentSolution = candidateSolution;
      } else {
        status = SolutionStatus.REJECTED;
      }

      operatorSelector.update(
          new OperatorSelectionFeedback<>(operatorSelection, candidateSolution, status));
      if (collectStatistics) {
        long iterationRuntimeNanos = System.nanoTime() - iterationStartedAt;
        statistics.collectIteration(
            operatorSelection,
            status,
            currentSolution.getObjectiveValue(),
            bestSolution.getObjectiveValue(),
            iterationRuntimeNanos);
      }
      iteration++;
    }

    return new AlnsResult<>(bestSolution, iteration, System.nanoTime() - startedAt, statistics);
  }

  private static <T> List<T> copyNonEmpty(List<T> values, String name) {
    Objects.requireNonNull(values, name + " must not be null.");
    if (values.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty.");
    }
    return List.copyOf(values);
  }

  private static <S extends Solution<S>> S copyOf(S solution, String message) {
    return Objects.requireNonNull(solution.copy(), message);
  }

  private static <S extends Solution<S>> boolean isBetter(S candidateSolution, S currentSolution) {
    return candidateSolution.compareTo(currentSolution) < 0;
  }

  private void validateOperatorSelection(OperatorSelection operatorSelection) {
    if (operatorSelection.destroyOperatorIndex() < 0
        || operatorSelection.destroyOperatorIndex() >= destroyOperators.size()
        || operatorSelection.repairOperatorIndex() < 0
        || operatorSelection.repairOperatorIndex() >= repairOperators.size()) {
      throw new IllegalStateException("operatorSelector returned an out-of-range operator index.");
    }
  }

  public static final class Builder<S extends Solution<S>> {

    private RandomGenerator randomGenerator;
    private final List<DestroyOperator<S>> destroyOperators = new ArrayList<>();
    private final List<RepairOperator<S>> repairOperators = new ArrayList<>();
    private OperatorSelector<S> operatorSelector;
    private boolean collectStatistics;

    private Builder() {}

    public Builder<S> randomGenerator(RandomGenerator randomGenerator) {
      this.randomGenerator =
          Objects.requireNonNull(randomGenerator, "randomGenerator must not be null.");
      return this;
    }

    public Builder<S> addDestroyOperator(DestroyOperator<S> destroyOperator) {
      destroyOperators.add(
          Objects.requireNonNull(destroyOperator, "destroyOperator must not be null."));
      return this;
    }

    public Builder<S> addRepairOperator(RepairOperator<S> repairOperator) {
      repairOperators.add(
          Objects.requireNonNull(repairOperator, "repairOperator must not be null."));
      return this;
    }

    public Builder<S> operatorSelector(OperatorSelector<S> operatorSelector) {
      this.operatorSelector =
          Objects.requireNonNull(operatorSelector, "operatorSelector must not be null.");
      return this;
    }

    public Builder<S> collectStatistics(boolean collectStatistics) {
      this.collectStatistics = collectStatistics;
      return this;
    }

    public Alns<S> build() {
      RandomGenerator configuredRandomGenerator =
          Objects.requireNonNull(randomGenerator, "A randomGenerator is required.");
      if (destroyOperators.isEmpty()) {
        throw new IllegalStateException("At least one destroy operator is required.");
      }
      if (repairOperators.isEmpty()) {
        throw new IllegalStateException("At least one repair operator is required.");
      }

      OperatorSelector<S> configuredOperatorSelector = operatorSelector;
      if (configuredOperatorSelector == null) {
        configuredOperatorSelector =
            new RandomOperatorSelector<>(destroyOperators.size(), repairOperators.size());
      }

      return new Alns<>(
          configuredRandomGenerator,
          destroyOperators,
          repairOperators,
          configuredOperatorSelector,
          collectStatistics);
    }
  }
}
