# ALNS

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

Release JARs are distributed through [GitHub Releases](https://github.com/nhtc2005/alns-java/releases).
Download `alns-0.1.0.jar` from the `v0.1.0` release once it is published and add it
to your application's classpath. Sources and Javadoc JARs are provided separately.

The library requires `org.slf4j:slf4j-api:2.0.20` on the classpath. Dependencies are
not bundled in the release JAR.

## API documentation

`alns` provides the ALNS algorithm, along with acceptance criteria, operator
selection policies, and stopping criteria. To solve your own problem, provide:

- A solution type implementing `Solution<S>`.
- A feasible initial solution.
- One or more destroy and repair operators tailored to your problem.

API documentation for the latest published release will be available on
[GitHub Pages](https://nhtc2005.github.io/alns-java/) after the first successful
deployment. To generate it locally, see [Documentation](CONTRIBUTING.md#documentation).

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
