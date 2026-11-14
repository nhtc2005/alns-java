package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.acceptance.AcceptanceCriterion;
import io.github.nhtc2005.alns.acceptance.SimulatedAnnealing;
import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.model.SolutionStatus;
import io.github.nhtc2005.alns.operator.DestroyOperator;
import io.github.nhtc2005.alns.operator.RepairOperator;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import io.github.nhtc2005.alns.selection.OperatorSelector;
import io.github.nhtc2005.alns.stopping.MaxIterations;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlnsStatisticsIntegrationTest {

  @Mock private DestroyOperator<TestSolution> mockDestroyOperator;
  @Mock private RepairOperator<TestSolution> mockRepairOperator;
  @Mock private OperatorSelector<TestSolution> mockOperatorSelector;
  @Mock private AcceptanceCriterion<TestSolution> mockAcceptanceCriterion;

  @ParameterizedTest
  @EnumSource(DisabledConfiguration.class)
  void solve_ShouldLeaveStatisticsDisabled_WhenCollectionIsNotEnabled(
      DisabledConfiguration configuration) {
    Alns.Builder<TestSolution> builder =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator((solution, randomGenerator) -> solution);
    if (configuration == DisabledConfiguration.BUILDER_EXPLICIT_FALSE) {
      builder.collectStatistics(true).collectStatistics(false);
    }
    Alns<TestSolution> alns = builder.build();

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), context -> true, new MaxIterations<>(2));

    assertDisabledStatistics(result);
  }

  @Test
  void solve_ShouldCollectInitialObjectiveValues_WhenStoppingBeforeFirstIteration() {
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator((solution, randomGenerator) -> solution)
            .collectStatistics(true)
            .build();

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), context -> true, context -> true);
    AlnsStatistics statistics = result.statistics();

    assertTrue(statistics.isEnabled());
    assertEquals(0, result.totalIterations());
    assertEquals(List.of(100.0), statistics.getCurrentObjectiveValues());
    assertEquals(List.of(100.0), statistics.getBestObjectiveValues());
    assertEquals(List.of(0L), statistics.getIterationRuntimesNanos());
    for (SolutionStatus status : SolutionStatus.values()) {
      assertEquals(0, statistics.getStatusCount(status));
      assertEquals(0, statistics.getDestroyOperatorCount(0, status));
      assertEquals(0, statistics.getRepairOperatorCount(0, status));
    }
  }

  @Test
  void solve_ShouldCollectHistoriesAndOperatorOutcomes_WhenCandidatesHaveDifferentResults() {
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator("random-removal", mockDestroyOperator)
            .addDestroyOperator(mockDestroyOperator)
            .addRepairOperator(mockRepairOperator)
            .addRepairOperator("greedy-insertion", mockRepairOperator)
            .addRepairOperator(mockRepairOperator)
            .operatorSelector(mockOperatorSelector)
            .collectStatistics(true)
            .build();
    when(mockOperatorSelector.select(any()))
        .thenReturn(
            new OperatorSelection(0, 2),
            new OperatorSelection(1, 0),
            new OperatorSelection(0, 1),
            new OperatorSelection(1, 2),
            new OperatorSelection(1, 1));
    when(mockDestroyOperator.apply(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(mockRepairOperator.apply(any(), any()))
        .thenReturn(
            new TestSolution(90.0),
            new TestSolution(120.0),
            new TestSolution(110.0),
            new TestSolution(130.0),
            new TestSolution(50.0, false));
    when(mockAcceptanceCriterion.shouldAccept(any())).thenReturn(false, true, true, false, true);

    AlnsResult<TestSolution> result =
        alns.solve(new TestSolution(100.0), mockAcceptanceCriterion, new MaxIterations<>(5));
    AlnsStatistics statistics = result.statistics();

    assertEquals(5, result.totalIterations());
    assertEquals(90.0, result.bestSolution().getObjectiveValue());
    assertEquals(
        List.of(100.0, 90.0, 120.0, 110.0, 110.0, 110.0), statistics.getCurrentObjectiveValues());
    assertEquals(List.of(100.0, 90.0, 90.0, 90.0, 90.0, 90.0), statistics.getBestObjectiveValues());
    assertEquals(1, statistics.getStatusCount(SolutionStatus.BEST));
    assertEquals(1, statistics.getStatusCount(SolutionStatus.BETTER));
    assertEquals(1, statistics.getStatusCount(SolutionStatus.ACCEPTED));
    assertEquals(2, statistics.getStatusCount(SolutionStatus.REJECTED));
    assertEquals(List.of("random-removal", "destroy-1"), statistics.getDestroyOperatorNames());
    assertEquals(
        List.of("repair-0", "greedy-insertion", "repair-2"), statistics.getRepairOperatorNames());

    long[][] expectedDestroyOperatorCounts = {{1, 1, 0, 0}, {0, 0, 1, 2}};
    long[][] expectedRepairOperatorCounts = {{0, 0, 1, 0}, {0, 1, 0, 1}, {1, 0, 0, 1}};
    for (int operatorIndex = 0;
        operatorIndex < expectedDestroyOperatorCounts.length;
        operatorIndex++) {
      for (SolutionStatus status : SolutionStatus.values()) {
        assertEquals(
            expectedDestroyOperatorCounts[operatorIndex][status.ordinal()],
            statistics.getDestroyOperatorCount(operatorIndex, status));
      }
    }
    for (int operatorIndex = 0;
        operatorIndex < expectedRepairOperatorCounts.length;
        operatorIndex++) {
      for (SolutionStatus status : SolutionStatus.values()) {
        assertEquals(
            expectedRepairOperatorCounts[operatorIndex][status.ordinal()],
            statistics.getRepairOperatorCount(operatorIndex, status));
      }
    }

    assertEquals(6, statistics.getIterationRuntimesNanos().size());
    assertEquals(0L, statistics.getIterationRuntimesNanos().get(0));
    assertTrue(statistics.getIterationRuntimesNanos().stream().allMatch(runtime -> runtime >= 0));
    long totalIterationRuntimeNanos =
        statistics.getIterationRuntimesNanos().stream().mapToLong(Long::longValue).sum();
    assertTrue(totalIterationRuntimeNanos <= result.elapsedNanos());
    verify(mockOperatorSelector, times(5)).update(any());
    verify(mockAcceptanceCriterion, times(5)).shouldAccept(any());
  }

  @Test
  void solve_ShouldKeepPreviousStatisticsUnchanged_WhenSameAlnsIsReused() {
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) -> new TestSolution(solution.getObjectiveValue() - 1))
            .collectStatistics(true)
            .build();

    AlnsResult<TestSolution> firstResult =
        alns.solve(new TestSolution(100.0), context -> true, new MaxIterations<>(2));
    AlnsStatistics firstStatistics = firstResult.statistics();
    List<Double> firstCurrentObjectiveValues = firstStatistics.getCurrentObjectiveValues();
    List<Long> firstIterationRuntimesNanos = firstStatistics.getIterationRuntimesNanos();

    AlnsResult<TestSolution> secondResult =
        alns.solve(new TestSolution(200.0), context -> true, new MaxIterations<>(3));
    AlnsStatistics secondStatistics = secondResult.statistics();

    assertNotSame(firstStatistics, secondStatistics);
    assertEquals(List.of(100.0, 99.0, 98.0), firstStatistics.getCurrentObjectiveValues());
    assertEquals(List.of(100.0, 99.0, 98.0), firstStatistics.getBestObjectiveValues());
    assertEquals(firstCurrentObjectiveValues, firstStatistics.getCurrentObjectiveValues());
    assertEquals(firstIterationRuntimesNanos, firstStatistics.getIterationRuntimesNanos());
    assertEquals(2, firstStatistics.getStatusCount(SolutionStatus.BEST));
    assertEquals(List.of(200.0, 199.0, 198.0, 197.0), secondStatistics.getCurrentObjectiveValues());
    assertEquals(List.of(200.0, 199.0, 198.0, 197.0), secondStatistics.getBestObjectiveValues());
    assertEquals(3, secondStatistics.getStatusCount(SolutionStatus.BEST));
    assertEquals(4, secondStatistics.getIterationRuntimesNanos().size());
  }

  @Test
  void solve_ShouldMinimizeObjective_WhenOtherSolutionDataHasDifferentOrdering() {
    Alns<RankedSolution> alns =
        Alns.<RankedSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) ->
                    solution.rank() == 10
                        ? new RankedSolution(9, 100.0)
                        : new RankedSolution(11, 1.0))
            .collectStatistics(true)
            .build();

    AlnsResult<RankedSolution> result =
        alns.solve(new RankedSolution(10, 10.0), context -> true, new MaxIterations<>(2));

    assertEquals(11, result.bestSolution().rank());
    assertEquals(List.of(10.0, 100.0, 1.0), result.statistics().getCurrentObjectiveValues());
    assertEquals(List.of(10.0, 10.0, 1.0), result.statistics().getBestObjectiveValues());
  }

  @Test
  void solve_ShouldPreserveSearchAndRandomSequence_WhenStatisticsAreEnabled() {
    Random randomGeneratorWithoutStatistics = new Random(123);
    Random randomGeneratorWithStatistics = new Random(123);
    List<Double> currentObjectiveValues = new ArrayList<>();
    Alns.Builder<TestSolution> builder =
        Alns.<TestSolution>builder()
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) -> {
                  currentObjectiveValues.add(solution.getObjectiveValue());
                  return new TestSolution(randomGenerator.nextInt(200) + 1);
                });
    Alns<TestSolution> alnsWithoutStatistics =
        builder.randomGenerator(randomGeneratorWithoutStatistics).build();
    Alns<TestSolution> alnsWithStatistics =
        builder.randomGenerator(randomGeneratorWithStatistics).collectStatistics(true).build();

    AlnsResult<TestSolution> resultWithoutStatistics =
        alnsWithoutStatistics.solve(
            new TestSolution(100.0),
            new SimulatedAnnealing<>(100.0, 1.0, 0.9),
            new MaxIterations<>(20));
    AlnsResult<TestSolution> resultWithStatistics =
        alnsWithStatistics.solve(
            new TestSolution(100.0),
            new SimulatedAnnealing<>(100.0, 1.0, 0.9),
            new MaxIterations<>(20));

    assertFalse(resultWithoutStatistics.statistics().isEnabled());
    assertTrue(resultWithStatistics.statistics().isEnabled());
    assertEquals(resultWithoutStatistics.totalIterations(), resultWithStatistics.totalIterations());
    assertEquals(
        resultWithoutStatistics.bestSolution().getObjectiveValue(),
        resultWithStatistics.bestSolution().getObjectiveValue());
    assertEquals(currentObjectiveValues.subList(0, 20), currentObjectiveValues.subList(20, 40));
    assertEquals(
        randomGeneratorWithoutStatistics.nextLong(), randomGeneratorWithStatistics.nextLong());
  }

  private void assertDisabledStatistics(AlnsResult<TestSolution> result) {
    AlnsStatistics statistics = result.statistics();
    assertEquals(2, result.totalIterations());
    assertFalse(statistics.isEnabled());
    assertThrows(IllegalStateException.class, statistics::getCurrentObjectiveValues);
    assertThrows(IllegalStateException.class, statistics::getBestObjectiveValues);
    assertThrows(IllegalStateException.class, statistics::getIterationRuntimesNanos);
    assertThrows(IllegalStateException.class, () -> statistics.getStatusCount(SolutionStatus.BEST));
  }

  private enum DisabledConfiguration {
    BUILDER_DEFAULT,
    BUILDER_EXPLICIT_FALSE
  }

  private record RankedSolution(int rank, double objectiveValue)
      implements Solution<RankedSolution> {

    @Override
    public RankedSolution copy() {
      return new RankedSolution(rank, objectiveValue);
    }

    @Override
    public double getObjectiveValue() {
      return objectiveValue;
    }

    @Override
    public boolean isFeasible() {
      return true;
    }
  }
}
