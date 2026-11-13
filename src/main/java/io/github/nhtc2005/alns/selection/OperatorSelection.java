package io.github.nhtc2005.alns.selection;

/**
 * Identifies the destroy and repair operators selected for an iteration.
 *
 * @param destroyOperatorIndex the zero-based index in the solver's destroy operator list
 * @param repairOperatorIndex the zero-based index in the solver's repair operator list
 */
public record OperatorSelection(int destroyOperatorIndex, int repairOperatorIndex) {}
