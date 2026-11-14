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
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs Adaptive Large Neighborhood Search with configurable operators and search policies.
 *
 * <p>The objective is minimized. Use {@link #builder()} or a constructor to configure the solver,
 * then supply acceptance and stopping criteria to {@link #solve}. Instances share their configured
 * components and RNG across calls and must not be used for concurrent searches.
 *
 * @param <S> the solution type
 */
public final class Alns<S extends Solution<S>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Alns.class);

  private final RandomGenerator randomGenerator;
  private final List<DestroyOperator<S>> destroyOperators;
  private final List<RepairOperator<S>> repairOperators;
  private final OperatorSelector<S> operatorSelector;
  private final boolean collectStatistics;
  private final VisitedSolutionTracker<S> visitedSolutionTracker;
  private final LocalSearch<S> localSearch;
  private final IterationCallback<S> iterationCallback;

  /**
   * Creates a solver with statistics and visit tracking disabled.
   *
   * <p>Operator lists are copied; operator and component instances are shared.
   *
   * @param randomGenerator the non-null random generator used throughout the search
   * @param destroyOperators the non-empty list of non-null destroy operators
   * @param repairOperators the non-empty list of non-null repair operators
   * @param operatorSelector the non-null selector compatible with the operator counts
   * @throws NullPointerException if a required argument or operator is null
   * @throws IllegalArgumentException if an operator list is empty or the selector rejects its
   *     counts
   */
  public Alns(
      RandomGenerator randomGenerator,
      List<DestroyOperator<S>> destroyOperators,
      List<RepairOperator<S>> repairOperators,
      OperatorSelector<S> operatorSelector) {
    this(randomGenerator, destroyOperators, repairOperators, operatorSelector, false);
  }

  /**
   * Creates a solver with optional statistics and visit tracking disabled.
   *
   * <p>Operator lists are copied; operator and component instances are shared.
   *
   * @param randomGenerator the non-null random generator used throughout the search
   * @param destroyOperators the non-empty list of non-null destroy operators
   * @param repairOperators the non-empty list of non-null repair operators
   * @param operatorSelector the non-null selector compatible with the operator counts
   * @param collectStatistics whether to retain histories and outcome counts
   * @throws NullPointerException if a required argument or operator is null
   * @throws IllegalArgumentException if an operator list is empty or the selector rejects its
   *     counts
   */
  public Alns(
      RandomGenerator randomGenerator,
      List<DestroyOperator<S>> destroyOperators,
      List<RepairOperator<S>> repairOperators,
      OperatorSelector<S> operatorSelector,
      boolean collectStatistics) {
    this(
        randomGenerator,
        destroyOperators,
        repairOperators,
        operatorSelector,
        collectStatistics,
        VisitedSolutionTracker.disabled());
  }

  /**
   * Creates a solver with the supplied search components.
   *
   * <p>Operator lists are copied; operator and component instances are shared.
   *
   * @param randomGenerator the non-null random generator used throughout the search
   * @param destroyOperators the non-empty list of non-null destroy operators
   * @param repairOperators the non-empty list of non-null repair operators
   * @param operatorSelector the non-null selector compatible with the operator counts
   * @param collectStatistics whether to retain histories and outcome counts
   * @param visitedSolutionTracker the non-null tracker used for reward eligibility
   * @throws NullPointerException if a required argument or operator is null
   * @throws IllegalArgumentException if an operator list is empty or the selector rejects its
   *     counts
   */
  public Alns(
      RandomGenerator randomGenerator,
      List<DestroyOperator<S>> destroyOperators,
      List<RepairOperator<S>> repairOperators,
      OperatorSelector<S> operatorSelector,
      boolean collectStatistics,
      VisitedSolutionTracker<S> visitedSolutionTracker) {
    this(
        randomGenerator,
        destroyOperators,
        repairOperators,
        operatorSelector,
        collectStatistics,
        visitedSolutionTracker,
        null,
        null);
  }

  private Alns(
      RandomGenerator randomGenerator,
      List<DestroyOperator<S>> destroyOperators,
      List<RepairOperator<S>> repairOperators,
      OperatorSelector<S> operatorSelector,
      boolean collectStatistics,
      VisitedSolutionTracker<S> visitedSolutionTracker,
      LocalSearch<S> localSearch,
      IterationCallback<S> iterationCallback) {
    this.randomGenerator =
        Objects.requireNonNull(randomGenerator, "randomGenerator must not be null.");
    this.destroyOperators = copyNonEmpty(destroyOperators, "destroyOperators");
    this.repairOperators = copyNonEmpty(repairOperators, "repairOperators");
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
   * bests are accepted regardless of the acceptance decision.
   *
   * <p>If configured, local search runs directly on feasible repaired solutions before evaluation.
   * Its result is checked again for feasibility and used by acceptance, tracking, feedback, and
   * statistics. The iteration runtime includes local search. Local search is not reset between
   * runs.
   *
   * <p>The tracker records the initial solution and accepted candidates. New bests remain reward
   * eligible; other accepted candidates require a new key and an objective different from the
   * current solution. Tracking does not prevent revisits. Exceptions from user components
   * propagate.
   *
   * <p>If configured, the iteration callback runs synchronously after feedback, statistics, and
   * incrementing the iteration count. It runs even when statistics are disabled and is not reset
   * between runs. Callback time counts toward total runtime, but not iteration statistics.
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
            ? new AlnsStatistics(destroyOperators.size(), repairOperators.size())
            : AlnsStatistics.disabled();
    if (collectStatistics) {
      statistics.collectInitial(
          currentSolution.getObjectiveValue(), bestSolution.getObjectiveValue());
    }

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          "Starting ALNS: initialObjective={}, destroyOperators={}, repairOperators={}",
          currentSolution.getObjectiveValue(),
          destroyOperators.size(),
          repairOperators.size());
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
              iteration + 1,
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
      iteration++;
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Iteration {}: destroyOperator={}, repairOperator={}, status={}, candidateObjective={}, currentObjective={}, bestObjective={}, rewardEligible={}",
            iteration,
            operatorSelection.destroyOperatorIndex(),
            operatorSelection.repairOperatorIndex(),
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
     * Appends a destroy operator; insertion order defines its index.
     *
     * @param destroyOperator the destroy operator to append
     * @return this builder
     * @throws NullPointerException if {@code destroyOperator} is null
     */
    public Builder<S> addDestroyOperator(DestroyOperator<S> destroyOperator) {
      destroyOperators.add(
          Objects.requireNonNull(destroyOperator, "destroyOperator must not be null."));
      return this;
    }

    /**
     * Appends a repair operator; insertion order defines its index.
     *
     * @param repairOperator the repair operator to append
     * @return this builder
     * @throws NullPointerException if {@code repairOperator} is null
     */
    public Builder<S> addRepairOperator(RepairOperator<S> repairOperator) {
      repairOperators.add(
          Objects.requireNonNull(repairOperator, "repairOperator must not be null."));
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
     * @throws IllegalArgumentException if the selector rejects the operator counts
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
          iterationCallback);
    }
  }
}
