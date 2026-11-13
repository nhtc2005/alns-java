package io.github.nhtc2005.alns.memory;

import io.github.nhtc2005.alns.model.Solution;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tracks visits by storing extracted keys in a hash set.
 *
 * <p>Keys are retained until reset, so memory usage grows with the number of distinct keys. This
 * tracker is mutable and must not be shared by concurrent runs.
 *
 * @param <K> the key type
 * @param <S> the solution type
 */
public final class HashSetVisitedSolutionTracker<K, S extends Solution<S>>
    implements VisitedSolutionTracker<S> {

  private final SolutionKey<K, S> solutionKey;
  private final Set<K> visitedKeys = new HashSet<>();

  /**
   * Creates an empty tracker using the supplied key extractor.
   *
   * @param solutionKey the extractor of non-null, stable solution keys
   * @throws NullPointerException if {@code solutionKey} is null
   */
  public HashSetVisitedSolutionTracker(SolutionKey<K, S> solutionKey) {
    this.solutionKey = Objects.requireNonNull(solutionKey, "solutionKey must not be null.");
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException if the solution or its extracted key is null
   */
  @Override
  public boolean recordIfNew(S solution) {
    Objects.requireNonNull(solution, "solution must not be null.");
    K key =
        Objects.requireNonNull(
            solutionKey.keyOf(solution), "SolutionKey.keyOf(...) must not return null.");
    return visitedKeys.add(key);
  }

  /** Removes all recorded keys. */
  @Override
  public void reset() {
    visitedKeys.clear();
  }

  /**
   * Returns the number of distinct keys recorded since the last reset.
   *
   * @return the recorded key count
   */
  public int getVisitedSolutionCount() {
    return visitedKeys.size();
  }
}
