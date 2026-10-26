package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

public final class RandomOperatorSelector<S extends Solution<S>> implements OperatorSelector<S> {

  private final int destroyOperatorCount;
  private final int repairOperatorCount;

  public RandomOperatorSelector(int destroyOperatorCount, int repairOperatorCount) {
    if (destroyOperatorCount <= 0 || repairOperatorCount <= 0) {
      throw new IllegalArgumentException("Operator counts must be positive.");
    }

    this.destroyOperatorCount = destroyOperatorCount;
    this.repairOperatorCount = repairOperatorCount;
  }

  @Override
  public OperatorSelection select(OperatorSelectionContext<S> context) {
    RandomGenerator randomGenerator = context.randomGenerator();
    return new OperatorSelection(
        randomGenerator.nextInt(destroyOperatorCount),
        randomGenerator.nextInt(repairOperatorCount));
  }
}
