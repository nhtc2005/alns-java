package io.github.nhtc2005.alns.memory;

import io.github.nhtc2005.alns.model.Solution;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class HashSetVisitedSolutionTracker<K, S extends Solution<S>>
    implements VisitedSolutionTracker<S> {

  private final SolutionKey<K, S> solutionKey;
  private final Set<K> visitedKeys = new HashSet<>();

  public HashSetVisitedSolutionTracker(SolutionKey<K, S> solutionKey) {
    this.solutionKey = Objects.requireNonNull(solutionKey, "solutionKey must not be null.");
  }

  @Override
  public boolean recordIfNew(S solution) {
    Objects.requireNonNull(solution, "solution must not be null.");
    K key =
        Objects.requireNonNull(
            solutionKey.keyOf(solution), "SolutionKey.keyOf(...) must not return null.");
    return visitedKeys.add(key);
  }

  @Override
  public void reset() {
    visitedKeys.clear();
  }

  public int getVisitedSolutionCount() {
    return visitedKeys.size();
  }
}
