# ALNS

[![CI](https://github.com/nhtc2005/alns-java/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/nhtc2005/alns-java/actions/workflows/ci.yml)
[![alns](https://img.shields.io/github/v/release/nhtc2005/alns-java?label=alns)](https://github.com/nhtc2005/alns-java/releases/latest)

`alns` is a general-purpose, documented and tested Java implementation of the
Adaptive Large Neighborhood Search (ALNS) metaheuristic for solving difficult
combinatorial optimization problems. Starting from an initial solution, ALNS
iterates until a stopping criterion is met. In each iteration, selected destroy
and repair operators transform the current solution into a candidate. The
candidate is evaluated using an acceptance criterion, and the operator selection
policy receives feedback based on the outcome.

## Requirements

- Java 17 or later.

## Installation

Download [alns-0.1.0.jar](https://github.com/nhtc2005/alns-java/releases/download/v0.1.0/alns-0.1.0.jar)
from [release v0.1.0](https://github.com/nhtc2005/alns-java/releases/tag/v0.1.0)
and add it to your application's classpath.
[Sources](https://github.com/nhtc2005/alns-java/releases/download/v0.1.0/alns-0.1.0-sources.jar)
and [Javadoc](https://github.com/nhtc2005/alns-java/releases/download/v0.1.0/alns-0.1.0-javadoc.jar)
JARs are also available.

The library requires `org.slf4j:slf4j-api:2.0.20` on the classpath. Dependencies are
not bundled in the release JAR.

## API documentation

Build a solver with `Alns.builder()` using your own `Solution<S>` type and
problem-specific destroy and repair operators. The builder lets you choose an
operator selection policy and optionally enable local search, visited-solution
tracking, statistics, and iteration callbacks. These optional features are
disabled by default; visit tracking controls operator reward eligibility without
preventing repeated solutions from being accepted.

Start the search with `solve(...)`, supplying a feasible initial solution and
your choice of acceptance and stopping criteria.

The [API documentation for version 0.1.0](https://nhtc2005.github.io/alns-java/)
is available on GitHub Pages. To generate it locally, see
[Documentation](CONTRIBUTING.md#documentation).

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) to get started.
For substantial changes, please open an issue first to discuss the proposed
approach.

## Getting help

For questions, bug reports, or feature requests, please
[open an issue](https://github.com/nhtc2005/alns-java/issues).
Include enough context and a minimal reproducible example where applicable.

## License

`alns` is available under the [MIT License](LICENSE).
