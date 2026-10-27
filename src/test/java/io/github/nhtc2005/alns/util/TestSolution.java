package io.github.nhtc2005.alns.util;

import io.github.nhtc2005.alns.model.Solution;

public final class TestSolution implements Solution<TestSolution> {

  private final double objectiveValue;
  private final boolean isFeasible;

  public TestSolution(double objectiveValue, boolean isFeasible) {
    this.objectiveValue = objectiveValue;
    this.isFeasible = isFeasible;
  }

  public TestSolution(double objectiveValue) {
    this(objectiveValue, true);
  }

  public static TestSolution feasible(double objectiveValue) {
    return new TestSolution(objectiveValue, true);
  }

  public static TestSolution infeasible(double objectiveValue) {
    return new TestSolution(objectiveValue, false);
  }

  @Override
  public TestSolution copy() {
    return new TestSolution(objectiveValue, isFeasible);
  }

  @Override
  public double getObjectiveValue() {
    return objectiveValue;
  }

  @Override
  public boolean isFeasible() {
    return isFeasible;
  }

  @Override
  public int compareTo(TestSolution o) {
    return Double.compare(this.objectiveValue, o.objectiveValue);
  }
}
