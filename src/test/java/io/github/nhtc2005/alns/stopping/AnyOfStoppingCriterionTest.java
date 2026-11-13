package io.github.nhtc2005.alns.stopping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.acceptance.HillClimbing;
import io.github.nhtc2005.alns.core.Alns;
import io.github.nhtc2005.alns.core.AlnsResult;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnyOfStoppingCriterionTest {

  @Mock private StoppingCriterion<TestSolution> mockFirstCriterion;
  @Mock private StoppingCriterion<TestSolution> mockSecondCriterion;
  @Mock private StoppingContext<TestSolution> mockContext;

  @Test
  void constructor_ShouldThrowException_WhenCriteriaAreNull() {
    assertThrows(NullPointerException.class, () -> new AnyOfStoppingCriterion<TestSolution>(null));
  }

  @Test
  void constructor_ShouldThrowException_WhenCriteriaAreEmpty() {
    assertThrows(
        IllegalArgumentException.class, () -> new AnyOfStoppingCriterion<TestSolution>(List.of()));
  }

  @Test
  void constructor_ShouldThrowException_WhenCriterionIsNull() {
    assertThrows(
        NullPointerException.class,
        () -> new AnyOfStoppingCriterion<TestSolution>(Arrays.asList(mockFirstCriterion, null)));
  }

  @Test
  void getCriteria_ShouldPreserveCriteria_WhenCallerModifiesInputOrReturnedList() {
    List<StoppingCriterion<TestSolution>> criteria =
        new ArrayList<>(List.of(mockFirstCriterion, mockSecondCriterion));
    AnyOfStoppingCriterion<TestSolution> criterion = new AnyOfStoppingCriterion<>(criteria);
    criteria.clear();
    when(mockSecondCriterion.shouldStop(mockContext)).thenReturn(true);

    assertEquals(List.of(mockFirstCriterion, mockSecondCriterion), criterion.getCriteria());
    assertTrue(criterion.shouldStop(mockContext));
    verify(mockFirstCriterion).shouldStop(mockContext);
    verify(mockSecondCriterion).shouldStop(mockContext);

    assertThrows(UnsupportedOperationException.class, () -> criterion.getCriteria().clear());
    assertEquals(List.of(mockFirstCriterion, mockSecondCriterion), criterion.getCriteria());
    assertTrue(criterion.shouldStop(mockContext));
  }

  @ParameterizedTest
  @CsvSource({"false, false", "true, false", "false, true"})
  void shouldStop_ShouldEvaluateInOrderAndShortCircuit_WhenCheckingCriteria(
      boolean firstMatches, boolean secondMatches) {
    AnyOfStoppingCriterion<TestSolution> criterion =
        new AnyOfStoppingCriterion<>(List.of(mockFirstCriterion, mockSecondCriterion));
    when(mockFirstCriterion.shouldStop(mockContext)).thenReturn(firstMatches);
    if (!firstMatches) {
      when(mockSecondCriterion.shouldStop(mockContext)).thenReturn(secondMatches);
    }

    assertEquals(firstMatches || secondMatches, criterion.shouldStop(mockContext));
    InOrder order = inOrder(mockFirstCriterion, mockSecondCriterion);
    order.verify(mockFirstCriterion).shouldStop(mockContext);
    if (firstMatches) {
      verifyNoInteractions(mockSecondCriterion);
    } else {
      order.verify(mockSecondCriterion).shouldStop(mockContext);
    }
    verifyNoMoreInteractions(mockFirstCriterion, mockSecondCriterion);
  }

  @Test
  void reset_ShouldResetAllCriteria_WhenLaterCriteriaWereSkipped() {
    AnyOfStoppingCriterion<TestSolution> criterion =
        new AnyOfStoppingCriterion<>(List.of(mockFirstCriterion, mockSecondCriterion));
    when(mockFirstCriterion.shouldStop(mockContext)).thenReturn(true);

    assertTrue(criterion.shouldStop(mockContext));
    criterion.reset();

    InOrder inOrder = inOrder(mockFirstCriterion, mockSecondCriterion);
    inOrder.verify(mockFirstCriterion).shouldStop(mockContext);
    inOrder.verify(mockFirstCriterion).reset();
    inOrder.verify(mockSecondCriterion).reset();
    verify(mockSecondCriterion, never()).shouldStop(mockContext);
  }

  @ParameterizedTest
  @CsvSource({"2, 2, 2", "100, 4, 3"})
  void solve_ShouldResetComposedCriteria_WhenSameCriterionIsReused(
      long maxIterations, long expectedFirstIterations, long expectedSecondIterations) {
    Alns<TestSolution> alns =
        Alns.<TestSolution>builder()
            .randomGenerator(new Random(1))
            .addDestroyOperator((solution, randomGenerator) -> solution)
            .addRepairOperator(
                (solution, randomGenerator) ->
                    solution.getObjectiveValue() == 100.0 ? new TestSolution(90.0) : solution)
            .build();
    AnyOfStoppingCriterion<TestSolution> criterion =
        new AnyOfStoppingCriterion<TestSolution>(
            List.of(new MaxIterations<>(maxIterations), new NoImprovement<>(3)));

    AlnsResult<TestSolution> firstResult =
        alns.solve(new TestSolution(100.0), new HillClimbing<>(), criterion);
    AlnsResult<TestSolution> secondResult =
        alns.solve(new TestSolution(200.0), new HillClimbing<>(), criterion);

    assertEquals(expectedFirstIterations, firstResult.totalIterations());
    assertEquals(90.0, firstResult.bestSolution().getObjectiveValue());
    assertEquals(expectedSecondIterations, secondResult.totalIterations());
    assertEquals(200.0, secondResult.bestSolution().getObjectiveValue());
  }
}
