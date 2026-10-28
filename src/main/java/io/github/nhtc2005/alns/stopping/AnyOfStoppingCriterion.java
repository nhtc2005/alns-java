package io.github.nhtc2005.alns.stopping;

import io.github.nhtc2005.alns.model.Solution;
import java.util.List;
import java.util.Objects;

public final class AnyOfStoppingCriterion<S extends Solution<S>> implements StoppingCriterion<S> {

  private final List<StoppingCriterion<S>> criteria;

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

  @Override
  public void reset() {
    for (StoppingCriterion<S> criterion : criteria) {
      criterion.reset();
    }
  }

  public List<StoppingCriterion<S>> getCriteria() {
    return criteria;
  }
}
