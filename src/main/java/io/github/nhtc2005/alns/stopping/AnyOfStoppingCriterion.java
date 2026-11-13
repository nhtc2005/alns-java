package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.util.List;
import java.util.Objects;

/**
 * Stops when any contained criterion requests termination.
 *
 * <p>Criteria are checked in list order, stopping at the first {@code true} result. Skipped
 * criteria do not receive that check, but all criteria are reset before each solve.
 *
 * @param <S> the solution type
 */
public final class AnyOfStoppingCriterion<S extends Solution<S>> implements StoppingCriterion<S> {

  private final List<StoppingCriterion<S>> criteria;

  /**
   * Creates a criterion from a defensive copy of the supplied list.
   *
   * @param criteria the non-empty list of non-null criteria, in evaluation order
   * @throws NullPointerException if the list or any element is null
   * @throws IllegalArgumentException if the list is empty
   */
  public AnyOfStoppingCriterion(List<? extends StoppingCriterion<S>> criteria) {
    Objects.requireNonNull(criteria, "criteria must not be null.");
    if (criteria.isEmpty()) {
      throw new IllegalArgumentException("criteria must not be empty.");
    }
    this.criteria = List.copyOf(criteria);
  }

  @Override
  public boolean shouldStop(StoppingContext<S> context) {
    for (StoppingCriterion<S> criterion : criteria) {
      if (criterion.shouldStop(context)) {
        return true;
      }
    }
    return false;
  }

  /** Resets every contained criterion, including those skipped by short-circuit evaluation. */
  @Override
  public void reset() {
    for (StoppingCriterion<S> criterion : criteria) {
      criterion.reset();
    }
  }

  /**
   * Returns the unmodifiable criteria list in evaluation order; criterion instances are shared.
   *
   * @return the unmodifiable criteria list in evaluation order; criterion instances are shared
   */
  public List<StoppingCriterion<S>> getCriteria() {
    return criteria;
  }
}
