package io.github.nhtc2005.alns.core;

import io.github.nhtc2005.alns.acceptance.AcceptanceContext;
import io.github.nhtc2005.alns.acceptance.AcceptanceCriterion;
import io.github.nhtc2005.alns.memory.VisitedSolutionTracker;
import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.operator.DestroyOperator;
import io.github.nhtc2005.alns.operator.LocalSearch;
import io.github.nhtc2005.alns.operator.RepairOperator;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import io.github.nhtc2005.alns.selection.OperatorSelectionContext;
import io.github.nhtc2005.alns.selection.OperatorSelectionFeedback;
import io.github.nhtc2005.alns.selection.OperatorSelector;
import io.github.nhtc2005.alns.selection.RandomOperatorSelector;
import io.github.nhtc2005.alns.stopping.StoppingContext;
import io.github.nhtc2005.alns.stopping.StoppingCriterion;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs Adaptive Large Neighborhood Search with configurable operators and search policies.
 *
 * <p>The objective is minimized. Use {@link #builder()} to configure the solver, then supply
 * acceptance and stopping criteria to {@link #solve}. Instances share their configured components
 * and RNG across calls and must not be used for concurrent searches.
 *
 * @param <S> the solution type
 */
public final class Alns<S extends Solution<S>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Alns.class);

  private final RandomGenerator randomGenerator;
  private final List<DestroyOperator<S>> destroyOperators;
  private final List<RepairOperator<S>> repairOperators;
  private final List<String> destroyOperatorNames;
  private final List<String> repairOperatorNames;
  private final OperatorSelector<S> operatorSelector;
  private final boolean collectStatistics;
  private final VisitedSolutionTracker<S> visitedSolutionTracker;
  private final LocalSearch<S> localSearch;
  private final IterationCallback<S> iterationCallback;

  private Alns(
      RandomGenerator randomGenerator,
      List<DestroyOperator<S>> destroyOperators,
      List<RepairOperator<S>> repairOperators,
      OperatorSelector<S> operatorSelector,
      boolean collectStatistics,
      VisitedSolutionTracker<S> visitedSolutionTracker,
      LocalSearch<S> localSearch,
      IterationCallback<S> iterationCallback,
      List<String> destroyOperatorNames,
      List<String> repairOperatorNames) {
    this.randomGenerator =
        Objects.requireNonNull(randomGenerator, "randomGenerator must not be null.");
    this.destroyOperators = copyNonEmpty(destroyOperators, "destroyOperators");
    this.repairOperators = copyNonEmpty(repairOperators, "repairOperators");
    this.destroyOperatorNames = copyOperatorNames(destroyOperatorNames, "destroy");
    this.repairOperatorNames = copyOperatorNames(repairOperatorNames, "repair");
    this.operatorSelector =
        Objects.requireNonNull(operatorSelector, "operatorSelector must not be null.");
    this.collectStatistics = collectStatistics;
    this.visitedSolutionTracker =
        Objects.requireNonNull(visitedSolutionTracker, "visitedSolutionTracker must not be null.");
    this.localSearch = localSearch;
    this.iterationCallback = iterationCallback;
    this.operatorSelector.validateOperatorCounts(
        this.destroyOperators.size(), this.repairOperators.size());
  }

  /**
   * Returns a builder with statistics, visit tracking, local search, and callbacks disabled.
   *
   * @param <S> the solution type
   * @return a new builder
   */
  public static <S extends Solution<S>> Builder<S> builder() {
    return new Builder<>();
  }

  /**
   * Searches from a feasible initial solution until the stopping criterion requests termination.
   *
   * <p>Resets the selector, criteria, and tracker before each run, but does not reset the RNG or
   * operators. The initial solution is copied, and statistics are created separately for each run.
   * Acceptance is evaluated every iteration; infeasible candidates are rejected, while feasible new
   * bests are accepted regardless of the acceptance decision. Iterations are numbered from one;
   * iteration zero denotes the initial solution.
   *
   * <p>If configured, local search runs directly on feasible repaired solutions before evaluation.
   * Its result is checked again for feasibility and used by acceptance, tracking, feedback, and
   * statistics. The iteration runtime includes local search. Local search is not reset between
   * runs.
   *
   * <p>Reward eligibility uses strict comparisons. New bests are always eligible; other accepted
   * candidates are eligible only when their objective differs from the current solution and their
   * key is new to the tracker. Ineligible feedback receives the rejected-outcome score. The tracker
   * records the initial solution and accepted candidates; revisited candidates can still become
   * current. Exceptions from user components propagate.
   *
   * <p>If configured, the iteration callback runs synchronously after feedback and statistics. It
   * runs even when statistics are disabled and is not reset between runs. Callback time counts
   * toward total runtime, but not iteration statistics.
   *
   * @param initialSolution the non-null feasible initial solution with a finite objective
   * @param acceptanceCriterion the non-null acceptance policy
   * @param stoppingCriterion the non-null termination policy, checked before each iteration
   * @return the best solution, completed iteration count, runtime, and statistics
   * @throws NullPointerException if an argument, solution copy, operator result, or selection is
   *     null
   * @throws IllegalArgumentException if the initial solution is infeasible or the initial or a
   *     feasible candidate objective is not finite
   * @throws IllegalStateException if the selector returns an out-of-range operator index
   */
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
    validateObjective(initialSolution, "initialSolution");

    operatorSelector.reset();
    acceptanceCriterion.reset();
    stoppingCriterion.reset();
    visitedSolutionTracker.reset();

    long startedAt = System.nanoTime();
    long iteration = 0;
    S currentSolution = copyOf(initialSolution, "initialSolution.copy() must not return null.");
    S bestSolution = copyOf(currentSolution, "Solution.copy() must not return null.");
    visitedSolutionTracker.recordIfNew(currentSolution);
    AlnsStatistics statistics =
        collectStatistics
            ? new AlnsStatistics(destroyOperatorNames, repairOperatorNames)
            : AlnsStatistics.disabled();
    if (collectStatistics) {
      statistics.collectInitial(
          currentSolution.getObjectiveValue(), bestSolution.getObjectiveValue());
    }

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "Starting ALNS: initialObjective={}, destroyOperators={}, repairOperators={}",
          currentSolution.getObjectiveValue(),
          destroyOperatorNames,
          repairOperatorNames);
    }

    while (!stoppingCriterion.shouldStop(
        new StoppingContext<>(
            iteration,
            System.nanoTime() - startedAt,
            bestSolution,
            currentSolution,
            randomGenerator))) {
      iteration++;
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

      boolean isFeasible = candidateSolution.isFeasible();
      if (isFeasible) {
        validateObjective(candidateSolution, "candidateSolution");
      }
      if (localSearch != null && isFeasible) {
        candidateSolution =
            Objects.requireNonNull(
                localSearch.apply(candidateSolution, randomGenerator),
                "LocalSearch.apply(...) must not return null.");
        isFeasible = candidateSolution.isFeasible();
        if (isFeasible) {
          validateObjective(candidateSolution, "candidateSolution");
        }
      }

      SolutionStatus status;
      boolean isRewardEligible = true;
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
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "New best at iteration {}: objective {} -> {}",
              iteration,
              bestSolution.getObjectiveValue(),
              candidateSolution.getObjectiveValue());
        }
        currentSolution = candidateSolution;
        bestSolution = copyOf(currentSolution, "Solution.copy() must not return null.");
        status = SolutionStatus.BEST;
        visitedSolutionTracker.recordIfNew(candidateSolution);
      } else if (isFeasible && isAccepted) {
        double candidateObjective = candidateSolution.getObjectiveValue();
        double currentObjective = currentSolution.getObjectiveValue();
        status =
            candidateObjective < currentObjective ? SolutionStatus.BETTER : SolutionStatus.ACCEPTED;
        boolean isNew = visitedSolutionTracker.recordIfNew(candidateSolution);
        isRewardEligible = isNew && candidateObjective != currentObjective;
        currentSolution = candidateSolution;
      } else {
        status = SolutionStatus.REJECTED;
      }

      operatorSelector.update(
          new OperatorSelectionFeedback<>(
              operatorSelection, candidateSolution, status, isRewardEligible));
      if (collectStatistics) {
        long iterationRuntimeNanos = System.nanoTime() - iterationStartedAt;
        statistics.collectIteration(
            operatorSelection,
            status,
            currentSolution.getObjectiveValue(),
            bestSolution.getObjectiveValue(),
            iterationRuntimeNanos);
      }
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Iteration {}: destroyOperator={}, repairOperator={}, status={}, candidateObjective={}, currentObjective={}, bestObjective={}, rewardEligible={}",
            iteration,
            destroyOperatorNames.get(operatorSelection.destroyOperatorIndex()),
            repairOperatorNames.get(operatorSelection.repairOperatorIndex()),
            status,
            candidateSolution.getObjectiveValue(),
            currentSolution.getObjectiveValue(),
            bestSolution.getObjectiveValue(),
            isRewardEligible);
      }
      if (iterationCallback != null) {
        iterationCallback.onIteration(
            new IterationContext<>(
                iteration,
                System.nanoTime() - startedAt,
                status,
                isRewardEligible,
                operatorSelection,
                candidateSolution,
                currentSolution,
                bestSolution));
      }
    }

    long elapsedNanos = System.nanoTime() - startedAt;
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "Finished ALNS: bestObjective={}, iterations={}, elapsedNanos={}",
          bestSolution.getObjectiveValue(),
          iteration,
          elapsedNanos);
    }
    return new AlnsResult<>(bestSolution, iteration, elapsedNanos, statistics);
  }

  private static void validateObjective(Solution<?> solution, String name) {
    if (!Double.isFinite(solution.getObjectiveValue())) {
      throw new IllegalArgumentException(name + " objective must be finite.");
    }
  }

  private static <T> List<T> copyNonEmpty(List<T> values, String name) {
    Objects.requireNonNull(values, name + " must not be null.");
    if (values.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty.");
    }
    return List.copyOf(values);
  }

  static List<String> defaultOperatorNames(String kind, int count) {
    List<String> names = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      names.add(defaultOperatorName(kind, index));
    }
    return List.copyOf(names);
  }

  private static String defaultOperatorName(String kind, int index) {
    return kind + "-" + index;
  }

  private static List<String> copyOperatorNames(List<String> names, String kind) {
    Set<String> uniqueNames = new HashSet<>();
    for (String name : names) {
      if (name.isBlank()) {
        throw new IllegalArgumentException(kind + " operator names must not be blank.");
      }
      if (!uniqueNames.add(name)) {
        throw new IllegalArgumentException("Duplicate " + kind + " operator name: '" + name + "'.");
      }
    }
    return List.copyOf(names);
  }

  private static <S extends Solution<S>> S copyOf(S solution, String message) {
    return Objects.requireNonNull(solution.copy(), message);
  }

  private static <S extends Solution<S>> boolean isBetter(S candidateSolution, S currentSolution) {
    return candidateSolution.getObjectiveValue() < currentSolution.getObjectiveValue();
  }

  private void validateOperatorSelection(OperatorSelection operatorSelection) {
    if (operatorSelection.destroyOperatorIndex() < 0
        || operatorSelection.destroyOperatorIndex() >= destroyOperators.size()
        || operatorSelection.repairOperatorIndex() < 0
        || operatorSelection.repairOperatorIndex() >= repairOperators.size()) {
      throw new IllegalStateException("operatorSelector returned an out-of-range operator index.");
    }
  }

  /**
   * Configures a solver, using uniform random selection unless a selector is supplied.
   *
   * <p>A random generator and at least one operator of each kind are required. Built solvers share
   * configured component instances; building does not clone their state.
   *
   * @param <S> the solution type
   */
  public static final class Builder<S extends Solution<S>> {

    private RandomGenerator randomGenerator;
    private final List<DestroyOperator<S>> destroyOperators = new ArrayList<>();
    private final List<RepairOperator<S>> repairOperators = new ArrayList<>();
    private final List<String> destroyOperatorNames = new ArrayList<>();
    private final List<String> repairOperatorNames = new ArrayList<>();
    private OperatorSelector<S> operatorSelector;
    private boolean collectStatistics;
    private VisitedSolutionTracker<S> visitedSolutionTracker = VisitedSolutionTracker.disabled();
    private LocalSearch<S> localSearch;
    private IterationCallback<S> iterationCallback;

    private Builder() {}

    /**
     * Sets the required source of randomness.
     *
     * @param randomGenerator the random generator used by the search
     * @return this builder
     * @throws NullPointerException if {@code randomGenerator} is null
     */
    public Builder<S> randomGenerator(RandomGenerator randomGenerator) {
      this.randomGenerator =
          Objects.requireNonNull(randomGenerator, "randomGenerator must not be null.");
      return this;
    }

    /**
     * Appends a destroy operator named {@code destroy-<index>}; insertion order defines its index.
     *
     * @param destroyOperator the destroy operator to append
     * @return this builder
     * @throws NullPointerException if {@code destroyOperator} is null
     */
    public Builder<S> addDestroyOperator(DestroyOperator<S> destroyOperator) {
      return addDestroyOperator(
          defaultOperatorName("destroy", destroyOperators.size()), destroyOperator);
    }

    /**
     * Appends a named destroy operator; insertion order defines its index.
     *
     * <p>Names are used in logs and statistics and must be non-blank and unique among destroy
     * operators.
     *
     * @param name the operator name
     * @param destroyOperator the destroy operator to append
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code destroyOperator} is null
     */
    public Builder<S> addDestroyOperator(String name, DestroyOperator<S> destroyOperator) {
      Objects.requireNonNull(name, "destroyOperatorName must not be null.");
      destroyOperators.add(
          Objects.requireNonNull(destroyOperator, "destroyOperator must not be null."));
      destroyOperatorNames.add(name);
      return this;
    }

    /**
     * Appends a repair operator named {@code repair-<index>}; insertion order defines its index.
     *
     * @param repairOperator the repair operator to append
     * @return this builder
     * @throws NullPointerException if {@code repairOperator} is null
     */
    public Builder<S> addRepairOperator(RepairOperator<S> repairOperator) {
      return addRepairOperator(
          defaultOperatorName("repair", repairOperators.size()), repairOperator);
    }

    /**
     * Appends a named repair operator; insertion order defines its index.
     *
     * <p>Names are used in logs and statistics and must be non-blank and unique among repair
     * operators.
     *
     * @param name the operator name
     * @param repairOperator the repair operator to append
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code repairOperator} is null
     */
    public Builder<S> addRepairOperator(String name, RepairOperator<S> repairOperator) {
      Objects.requireNonNull(name, "repairOperatorName must not be null.");
      repairOperators.add(
          Objects.requireNonNull(repairOperator, "repairOperator must not be null."));
      repairOperatorNames.add(name);
      return this;
    }

    /**
     * Sets the operator selector, replacing the default uniform random selector.
     *
     * @param operatorSelector the selector compatible with the configured operator counts
     * @return this builder
     * @throws NullPointerException if {@code operatorSelector} is null
     */
    public Builder<S> operatorSelector(OperatorSelector<S> operatorSelector) {
      this.operatorSelector =
          Objects.requireNonNull(operatorSelector, "operatorSelector must not be null.");
      return this;
    }

    /**
     * Enables or disables statistics collection.
     *
     * @param collectStatistics whether to retain full histories and outcome counts; false by
     *     default
     * @return this builder
     */
    public Builder<S> collectStatistics(boolean collectStatistics) {
      this.collectStatistics = collectStatistics;
      return this;
    }

    /**
     * Sets the tracker used to identify accepted solution revisits.
     *
     * @param visitedSolutionTracker the tracker for reward eligibility; disabled by default
     * @return this builder
     * @throws NullPointerException if {@code visitedSolutionTracker} is null
     */
    public Builder<S> visitedSolutionTracker(VisitedSolutionTracker<S> visitedSolutionTracker) {
      this.visitedSolutionTracker =
          Objects.requireNonNull(
              visitedSolutionTracker, "visitedSolutionTracker must not be null.");
      return this;
    }

    /**
     * Sets optional local search for feasible repaired solutions; disabled by default.
     *
     * @param localSearch the local search to apply before candidate evaluation
     * @return this builder
     * @throws NullPointerException if {@code localSearch} is null
     */
    public Builder<S> localSearch(LocalSearch<S> localSearch) {
      this.localSearch = Objects.requireNonNull(localSearch, "localSearch must not be null.");
      return this;
    }

    /**
     * Sets the callback invoked after each completed iteration; disabled by default.
     *
     * @param callback the synchronous callback, shared across runs
     * @return this builder
     * @throws NullPointerException if {@code callback} is null
     */
    public Builder<S> onIteration(IterationCallback<S> callback) {
      this.iterationCallback = Objects.requireNonNull(callback, "callback must not be null.");
      return this;
    }

    /**
     * Creates a solver and validates the selector's operator counts.
     *
     * @return a solver with copied operator lists and shared component instances
     * @throws NullPointerException if no random generator was configured
     * @throws IllegalStateException if either operator list is empty
     * @throws IllegalArgumentException if an operator name is blank or duplicated within its group,
     *     or the selector rejects the operator counts
     */
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
          collectStatistics,
          visitedSolutionTracker,
          localSearch,
          iterationCallback,
          destroyOperatorNames,
          repairOperatorNames);
    }
  }
}
