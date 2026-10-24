package io.github.nhtc2005.alns.model;

public interface Solution<S extends Solution<S>> {

  S copy();

  double getObjectiveValue();

  boolean isFeasible();
}
