package io.github.nhtc2005.alns.operator;

import io.github.nhtc2005.alns.model.Solution;
import java.util.random.RandomGenerator;

@FunctionalInterface
public interface SolutionOperator<S extends Solution<S>> {

  S apply(S solution, RandomGenerator randomGenerator);
}
