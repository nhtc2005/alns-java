package io.github.nhtc2005.alns.selection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.github.nhtc2005.alns.util.TestSolution;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RandomOperatorSelectorTest {

  @Mock private RandomGenerator mockRandomGenerator;

  @ParameterizedTest
  @CsvSource({"0, 5", "-1, 5", "5, 0", "5, -1", "0, 0"})
  void constructor_ShouldThrowException_WhenOperatorCountsAreInvalid(
      int destroyOperatorCount, int repairOperatorCount) {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new RandomOperatorSelector<>(destroyOperatorCount, repairOperatorCount));

    assertEquals("Operator counts must be positive.", exception.getMessage());
  }

  @Test
  void select_ShouldReturnValidOperatorSelection_WhenCalled() {
    int destroyOperatorCount = 4;
    int repairOperatorCount = 3;
    RandomOperatorSelector<TestSolution> selector =
        new RandomOperatorSelector<>(destroyOperatorCount, repairOperatorCount);

    OperatorSelectionContext<TestSolution> context =
        new OperatorSelectionContext<>(0, 0, null, null, mockRandomGenerator);
    when(mockRandomGenerator.nextInt(destroyOperatorCount)).thenReturn(2);
    when(mockRandomGenerator.nextInt(repairOperatorCount)).thenReturn(1);

    OperatorSelection selection = selector.select(context);

    assertEquals(destroyOperatorCount, selector.getDestroyOperatorCount());
    assertEquals(repairOperatorCount, selector.getRepairOperatorCount());
    assertNotNull(selection);
    assertEquals(2, selection.destroyOperatorIndex());
    assertEquals(1, selection.repairOperatorIndex());
  }
}
