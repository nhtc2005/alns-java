package io.github.nhtc2005.alns.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import io.github.nhtc2005.alns.util.TestSolution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HillClimbingTest {

  @ParameterizedTest
  @CsvSource({"80.0, true", "100.0, false", "120.0, false"})
  void shouldAccept_ShouldAcceptOnlyImprovingCandidates_WhenComparingObjectives(
      double candidateObjective, boolean expected) {
    HillClimbing<TestSolution> hillClimbing = new HillClimbing<>();
    TestSolution current = new TestSolution(100.0);
    AcceptanceContext<TestSolution> context =
        new AcceptanceContext<>(0, 0, current, current, new TestSolution(candidateObjective), null);

    assertEquals(expected, hillClimbing.shouldAccept(context));
  }
}
