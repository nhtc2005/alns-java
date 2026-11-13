package io.github.nhtc2005.alns.operator;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

/**
 * Transforms a solution during an ALNS iteration.
 *
 * @param <S> the solution type
 */
@FunctionalInterface
public interface SolutionOperator<S extends Solution<S>> {

  /**
   * Applies this operator using the supplied source of randomness.
   *
   * <p>The operator may modify the input solution and return it, or return a different solution.
   * Use the supplied generator for random choices to support reproducible runs.
   *
   * @param solution the non-null solution to transform
   * @param randomGenerator the non-null random generator used by the search
   * @return a non-null transformed solution
   */
  S apply(S solution, RandomGenerator randomGenerator);
}
