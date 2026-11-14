package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.model.SolutionStatus;

/**
 * Reports a candidate's outcome to the operator selector.
 *
 * <p>Reward eligibility affects scoring, not acceptance or status. The candidate is a shared
 * reference and must not be modified by the selector.
 *
 * @param <S> the solution type
 * @param operatorSelection the operator pair that produced the candidate
 * @param candidateSolution the candidate after repair and optional local search, including when
 *     rejected
 * @param status the outcome assigned by the solver
 * @param isRewardEligible whether the configured outcome score may be applied; if false, the
 *     built-in adaptive selectors use zero score but still advance their update state
 */
public record OperatorSelectionFeedback<S extends Solution<S>>(
    OperatorSelection operatorSelection,
    S candidateSolution,
    SolutionStatus status,
    boolean isRewardEligible) {

  /**
   * Creates feedback eligible for the configured outcome score.
   *
   * @param operatorSelection the selected operator pair
   * @param candidateSolution the repaired candidate
   * @param status the outcome assigned by the solver
   */
  public OperatorSelectionFeedback(
      OperatorSelection operatorSelection, S candidateSolution, SolutionStatus status) {
    this(operatorSelection, candidateSolution, status, true);
  }
}
