package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Selects a destroy and repair operator pair for each ALNS iteration.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface OperatorSelector<S extends Solution<S>> {

  /**
   * Selects operator indices without modifying the solutions in the context.
   *
   * @param context the non-null search context
   * @return a non-null selection with zero-based indices within the configured operator lists
   */
  OperatorSelection select(OperatorSelectionContext<S> context);

  /**
   * Receives the outcome after each iteration to update selection state.
   *
   * <p>The candidate solution must not be modified. The default implementation does nothing.
   *
   * @param feedback the non-null outcome and reward eligibility of the selected operator pair
   */
  default void update(OperatorSelectionFeedback<S> feedback) {}

  /**
   * Restores initial selection state before a solve begins.
   *
   * <p>The default implementation does nothing.
   */
  default void reset() {}

  /**
   * Validates the operator counts when the solver is built.
   *
   * <p>Selectors with fixed counts must check that both counts match their configuration. The
   * default implementation does nothing.
   *
   * @param destroyOperatorCount the positive number of configured destroy operators
   * @param repairOperatorCount the positive number of configured repair operators
   * @throws IllegalArgumentException if the counts are incompatible with this selector
   */
  default void validateOperatorCounts(int destroyOperatorCount, int repairOperatorCount) {}
}
