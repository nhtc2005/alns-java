package io.github.nhtc2005.alns.core;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.operator.DestroyOperator;
import io.github.nhtc2005.alns.operator.RepairOperator;
import io.github.nhtc2005.alns.selection.OperatorSelection;
import io.github.nhtc2005.alns.selection.OperatorSelectionContext;
import io.github.nhtc2005.alns.selection.OperatorSelector;
import io.github.nhtc2005.alns.selection.RandomOperatorSelector;
import io.github.nhtc2005.alns.selection.RouletteWheelOperatorSelector;
import io.github.nhtc2005.alns.selection.SegmentedRouletteWheelOperatorSelector;
import io.github.nhtc2005.alns.util.TestSolution;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AlnsOperatorCountsTest {

  private final DestroyOperator<TestSolution> destroy = (solution, random) -> solution;
  private final RepairOperator<TestSolution> repair = (solution, random) -> solution;

  @ParameterizedTest
  @CsvSource({"1, 2", "3, 2", "2, 1", "2, 3"})
  void construction_ShouldRejectMismatchedCounts_ForAllBuiltInSelectors(int destroys, int repairs) {
    for (OperatorSelector<TestSolution> selector : selectors(destroys, repairs)) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new Alns<>(
                  new Random(1), List.of(destroy, destroy), List.of(repair, repair), selector));
      assertThrows(IllegalArgumentException.class, () -> builder().operatorSelector(selector).build());
    }
  }

  @Test
  void construction_ShouldAcceptMatchingCounts_ForAllBuiltInSelectors() {
    for (OperatorSelector<TestSolution> selector : selectors(2, 2)) {
      assertDoesNotThrow(
          () ->
              new Alns<>(
                  new Random(1), List.of(destroy, destroy), List.of(repair, repair), selector));
      assertDoesNotThrow(() -> builder().operatorSelector(selector).build());
    }
  }

  @Test
  void construction_ShouldSupportCustomLambdaSelector() {
    assertDoesNotThrow(
        () -> builder().operatorSelector(context -> new OperatorSelection(0, 0)).build());
  }

  @Test
  void construction_ShouldPassActualCountsToCustomSelector() {
    OperatorSelector<TestSolution> selector =
        new OperatorSelector<>() {
          @Override
          public OperatorSelection select(
              OperatorSelectionContext<TestSolution> context) {
            return new OperatorSelection(0, 0);
          }

          @Override
          public void validateOperatorCounts(int destroyCount, int repairCount) {
            assertEquals(2, destroyCount);
            assertEquals(2, repairCount);
            throw new IllegalArgumentException("custom validation");
          }
        };
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> builder().operatorSelector(selector).build());
    assertEquals("custom validation", exception.getMessage());
  }

  private Alns.Builder<TestSolution> builder() {
    return Alns.<TestSolution>builder()
        .randomGenerator(new Random(1))
        .addDestroyOperator(destroy)
        .addDestroyOperator(destroy)
        .addRepairOperator(repair)
        .addRepairOperator(repair);
  }

  private List<OperatorSelector<TestSolution>> selectors(int destroys, int repairs) {
    return List.of(
        new RandomOperatorSelector<>(destroys, repairs),
        new RouletteWheelOperatorSelector<>(destroys, repairs, new double[] {7, 3, 1, 0}, 0.5),
        new SegmentedRouletteWheelOperatorSelector<>(
            destroys, repairs, new double[] {7, 3, 1, 0}, 0.5, 10));
  }
}
