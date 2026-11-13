package io.github.nhtc2005.alns.model;

/**
 * Represents a problem-specific solution for ALNS.
 *
 * <p>The framework minimizes the objective: \(s_1\) is better than \(s_2\) when \(f(s_1) \lt
 * f(s_2)\). For maximization, return \(f(s) = -g(s)\).
 *
 * <p>Objective and feasibility queries must not modify the solution and must reflect its current
 * state.
 *
 * @param <S> the concrete solution type
 */
public interface Solution<S extends Solution<S>> {

  /**
   * Returns a copy preserving this solution's state, objective value, and feasibility.
   *
   * <p>Mutable solution data must be independent of the original. Immutable data may be shared;
   * fully immutable solutions may return {@code this}.
   *
   * @return a non-null copy of this solution
   */
  S copy();

  /**
   * Returns the objective value to minimize.
   *
   * @return the objective value; must be finite for initial solutions and repaired candidates
   */
  double getObjectiveValue();

  /**
   * Returns whether this solution satisfies the problem's feasibility constraints.
   *
   * @return {@code true} if all feasibility constraints are satisfied; {@code false} otherwise
   */
  boolean isFeasible();
}
