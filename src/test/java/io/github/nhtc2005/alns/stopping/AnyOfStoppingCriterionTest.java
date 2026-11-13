package io.github.nhtc2005.alns.stopping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.nhtc2005.alns.util.TestSolution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    NullPointerException exception =
        assertThrows(
            NullPointerException.class, () -> new AnyOfStoppingCriterion<TestSolution>(null));

    assertEquals("criteria must not be null.", exception.getMessage());
  }

  @Test
  void constructor_ShouldThrowException_WhenCriteriaAreEmpty() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnyOfStoppingCriterion<TestSolution>(List.of()));

    assertEquals("criteria must not be empty.", exception.getMessage());
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

  @Test
  void shouldStop_ShouldSkipLaterCriteria_WhenEarlierCriterionMatches() {
    AnyOfStoppingCriterion<TestSolution> criterion =
        new AnyOfStoppingCriterion<>(List.of(mockFirstCriterion, mockSecondCriterion));
    when(mockFirstCriterion.shouldStop(mockContext)).thenReturn(true);

    assertTrue(criterion.shouldStop(mockContext));
    verify(mockFirstCriterion).shouldStop(mockContext);
    verifyNoMoreInteractions(mockFirstCriterion);
    verifyNoInteractions(mockSecondCriterion);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldStop_ShouldEvaluateAllCriteriaInOrder_WhenEarlierCriteriaDoNotMatch(
      boolean secondMatches) {
    AnyOfStoppingCriterion<TestSolution> criterion =
        new AnyOfStoppingCriterion<>(List.of(mockFirstCriterion, mockSecondCriterion));
    when(mockFirstCriterion.shouldStop(mockContext)).thenReturn(false);
    when(mockSecondCriterion.shouldStop(mockContext)).thenReturn(secondMatches);

    assertEquals(secondMatches, criterion.shouldStop(mockContext));
    InOrder order = inOrder(mockFirstCriterion, mockSecondCriterion);
    order.verify(mockFirstCriterion).shouldStop(mockContext);
    order.verify(mockSecondCriterion).shouldStop(mockContext);
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
}
