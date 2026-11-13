package io.github.nhtc2005.alns.model;

/** Describes the outcome of evaluating a candidate solution in an ALNS iteration. */
public enum SolutionStatus {
  /** A feasible candidate strictly improves the best objective found so far. */
  BEST,
  /** An accepted feasible candidate strictly improves the current objective, but not the best. */
  BETTER,
  /** A feasible candidate is accepted without strictly improving the current objective. */
  ACCEPTED,
  /** A candidate is infeasible or is rejected by the acceptance criterion without becoming best. */
  REJECTED
}
