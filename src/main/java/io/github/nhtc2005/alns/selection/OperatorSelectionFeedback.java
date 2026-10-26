package io.github.nhtc2005.alns.selection;

import io.github.nhtc2005.alns.model.Solution;
import io.github.nhtc2005.alns.model.SolutionStatus;

public record OperatorSelectionFeedback<S extends Solution<S>>(
    OperatorSelection operatorSelection, S candidateSolution, SolutionStatus status) {}
