package io.github.nhtc2005.alns.memory;

import io.github.nhtc2005.alns.model.Solution;

/**
 * Extracts a key identifying a solution's structure, independently of its objective value.
 *
 * @param <K> the key type
 * @param <S> the solution type
 */
@FunctionalInterface
public interface SolutionKey<K, S extends Solution<S>> {

  /**
   * Returns a key without modifying the solution.
   *
   * <p>Key equality must represent solution identity. The key's equality and hash code must remain
   * stable while stored; snapshot mutable solution data rather than returning a live reference.
   *
   * @param solution the non-null solution to identify
   * @return a non-null key with stable {@code equals} and {@code hashCode} behavior
   */
  K keyOf(S solution);
}
