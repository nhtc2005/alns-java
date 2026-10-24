package io.github.nhtc2005.alns.model;

public enum SolutionStatus {
  BEST("best"),
  BETTER("better"),
  ACCEPTED("accepted"),
  REJECTED("rejected");

  private final String description;

  SolutionStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
