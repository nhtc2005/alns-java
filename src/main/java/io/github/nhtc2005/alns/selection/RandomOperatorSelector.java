package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

/**
 * Selects destroy and repair operators independently and uniformly using the search RNG.
 *
 * @param <S> the solution type
 */
public final class RandomOperatorSelector<S extends Solution<S>> implements OperatorSelector<S> {

  private final int destroyOperatorCount;
  private final int repairOperatorCount;

  /**
   * Creates a selector for fixed operator counts.
   *
   * @param destroyOperatorCount the positive number of destroy operators
   * @param repairOperatorCount the positive number of repair operators
   * @throws IllegalArgumentException if either operator count is not positive
   */
  public RandomOperatorSelector(int destroyOperatorCount, int repairOperatorCount) {
    if (destroyOperatorCount <= 0 || repairOperatorCount <= 0) {
      throw new IllegalArgumentException("Operator counts must be positive.");
    }

    this.destroyOperatorCount = destroyOperatorCount;
    this.repairOperatorCount = repairOperatorCount;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if either count differs from this selector's configured count
   */
  @Override
  public void validateOperatorCounts(int destroyOperatorCount, int repairOperatorCount) {
    if (destroyOperatorCount != getDestroyOperatorCount()
        || repairOperatorCount != getRepairOperatorCount()) {
      throw new IllegalArgumentException(
          "Selector operator counts must match the configured destroy and repair operator counts.");
    }
  }

  @Override
  public OperatorSelection select(OperatorSelectionContext<S> context) {
    RandomGenerator randomGenerator = context.randomGenerator();
    return new OperatorSelection(
        randomGenerator.nextInt(destroyOperatorCount),
        randomGenerator.nextInt(repairOperatorCount));
  }

  /**
   * Returns the configured number of destroy operators.
   *
   * @return the configured number of destroy operators
   */
  public int getDestroyOperatorCount() {
    return destroyOperatorCount;
  }

  /**
   * Returns the configured number of repair operators.
   *
   * @return the configured number of repair operators
   */
  public int getRepairOperatorCount() {
    return repairOperatorCount;
  }
}
