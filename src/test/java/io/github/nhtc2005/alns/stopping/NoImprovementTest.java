package io.github.nhtc2005.alns.stopping;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.acceptance.HillClimbing;
import io.github.nhtc2005.alns.core.Alns;
import io.github.nhtc2005.alns.core.AlnsResult;
import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoImprovementTest {

  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void constructor_ShouldThrowException_WhenMaxIterationsIsNonPositive(long maxIterations) {
    assertThrows(
        IllegalArgumentException.class, () -> new NoImprovement<TestSolution>(maxIterations));
  }

  @Test
  void shouldStop_ShouldCountCompletedIterations_WhenBestDoesNotImprove() {
    NoImprovement<TestSolution> criterion = new NoImprovement<>(3);

    assertEquals(3, criterion.getMaxIterations());
    assertEquals(0, criterion.getLastImprovementIteration());
    assertFalse(criterion.shouldStop(context(0, 100.0, 100.0)));
    assertFalse(criterion.shouldStop(context(2, 100.0, 100.0)));
    assertTrue(criterion.shouldStop(context(3, 100.0, 100.0)));
    assertTrue(criterion.shouldStop(context(4, 100.0, 100.0)));
  }

  @Test
  void shouldStop_ShouldNotCountAdditionalIterations_WhenCalledRepeatedlyForSameIteration() {
    NoImprovement<TestSolution> criterion = new NoImprovement<>(2);

    for (int call = 0; call < 4; call++) {
      assertFalse(criterion.shouldStop(context(0, 100.0, 100.0)));
    }
    assertTrue(criterion.shouldStop(context(2, 100.0, 100.0)));
  }

  @Test
  void shouldStop_ShouldRestartIterationLimit_WhenBestImproves() {
    NoImprovement<TestSolution> criterion = new NoImprovement<>(3);

    assertFalse(criterion.shouldStop(context(0, 100.0, 100.0)));
    assertEquals(0, criterion.getLastImprovementIteration());
    assertFalse(criterion.shouldStop(context(2, 90.0, 90.0)));
    assertEquals(2, criterion.getLastImprovementIteration());
    assertFalse(criterion.shouldStop(context(4, 90.0, 90.0)));
    assertTrue(criterion.shouldStop(context(5, 90.0, 90.0)));
    assertEquals(2, criterion.getLastImprovementIteration());
  }

  @Test
  void shouldStop_ShouldNotRestartIterationLimit_WhenOnlyCurrentImproves() {
    NoImprovement<TestSolution> criterion = new NoImprovement<>(2);

    assertFalse(criterion.shouldStop(context(0, 100.0, 120.0)));
    assertTrue(criterion.shouldStop(context(2, 100.0, 110.0)));
  }

  @Test
  void shouldStop_ShouldSnapshotBestObjective_WhenSolutionIsMutable() {
    NoImprovement<MutableSolution> criterion = new NoImprovement<>(2);
    MutableSolution bestSolution = new MutableSolution(10);

    assertFalse(
        criterion.shouldStop(new StoppingContext<>(0, 0, bestSolution, bestSolution, null)));
    bestSolution.objectiveValue = 9;
    assertFalse(
        criterion.shouldStop(new StoppingContext<>(2, 0, bestSolution, bestSolution, null)));
    assertTrue(criterion.shouldStop(new StoppingContext<>(4, 0, bestSolution, bestSolution, null)));
  }

  @Test
  void reset_ShouldClearPreviousRun_WhenCriterionIsReused() {
    NoImprovement<TestSolution> criterion = new NoImprovement<>(2);

    assertFalse(criterion.shouldStop(context(0, 100.0, 100.0)));
    assertFalse(criterion.shouldStop(context(1, 90.0, 90.0)));
    assertEquals(1, criterion.getLastImprovementIteration());
    assertTrue(criterion.shouldStop(context(3, 90.0, 90.0)));
    criterion.reset();
    assertEquals(0, criterion.getLastImprovementIteration());
    assertFalse(criterion.shouldStop(context(0, 120.0, 120.0)));
    assertFalse(criterion.shouldStop(context(1, 120.0, 120.0)));
    assertTrue(criterion.shouldStop(context(2, 120.0, 120.0)));
  }

  @Test
  void solve_ShouldResetCriterion_WhenSameCriterionIsReused() {
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator((solution, randomGenerator) -> solution)
            .build();
    NoImprovement<TestSolution> criterion = new NoImprovement<>(3);

    for (int run = 0; run < 2; run++) {
      AlnsResult<TestSolution> result =
          alns.solve(new TestSolution(100.0), new HillClimbing<>(), criterion);

      assertEquals(3, result.totalIterations());
      assertEquals(100.0, result.bestSolution().getObjectiveValue());
    }
  }

  private StoppingContext<TestSolution> context(long iteration, double best, double current) {
    return new StoppingContext<>(
        iteration, 0, new TestSolution(best), new TestSolution(current), null);
  }

  private static final class MutableSolution implements Solution<MutableSolution> {
    private int objectiveValue;

    private MutableSolution(int objectiveValue) {
      this.objectiveValue = objectiveValue;
    }

    @Override
    public MutableSolution copy() {
      return new MutableSolution(objectiveValue);
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
