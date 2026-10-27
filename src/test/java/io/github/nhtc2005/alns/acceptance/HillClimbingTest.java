package io.github.nhtc2005.alns.acceptance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.github.nhtc2005.alns.util.TestSolution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HillClimbingTest {

  @Mock private AcceptanceContext<TestSolution> mockContext;
  @Mock private TestSolution mockCandidateSolution;
  @Mock private TestSolution mockCurrentSolution;

  @Test
  void shouldAccept_ShouldReturnTrue_WhenCandidateIsBetter() {
    HillClimbing<TestSolution> hillClimbing = new HillClimbing<>();

    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(80.0);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);

    assertTrue(hillClimbing.shouldAccept(mockContext));
  }

  @Test
  void shouldAccept_ShouldReturnFalse_WhenCandidateEqualsCurrent() {
    HillClimbing<TestSolution> hillClimbing = new HillClimbing<>();

    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(100.0);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);

    assertFalse(hillClimbing.shouldAccept(mockContext));
  }

  @Test
  void shouldAccept_ShouldReturnFalse_WhenCandidateIsWorse() {
    HillClimbing<TestSolution> hillClimbing = new HillClimbing<>();

    when(mockContext.candidateSolution()).thenReturn(mockCandidateSolution);
    when(mockContext.currentSolution()).thenReturn(mockCurrentSolution);
    when(mockCandidateSolution.getObjectiveValue()).thenReturn(120.0);
    when(mockCurrentSolution.getObjectiveValue()).thenReturn(100.0);

    assertFalse(hillClimbing.shouldAccept(mockContext));
  }
}
