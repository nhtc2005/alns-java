package io.github.nhtc2005.alns.model;

public interface Solution<S extends Solution<S>> extends Comparable<S> {

  S copy();

  double getObjectiveValue();

  boolean isFeasible();
}
