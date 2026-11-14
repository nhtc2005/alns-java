# Contributing

Contributions to `alns` are welcome, including bug reports, documentation
improvements, and code changes.

## Development setup

Install Java 17 or later, then clone the repository:

```bash
git clone https://github.com/nhtc2005/alns-java.git
cd alns-java
```

Use the included Maven Wrapper; a separate Maven installation is not required.
On Windows, replace `./mvnw` with `.\mvnw.cmd` in the commands below.

## Code style

- Follow the existing project structure and coding conventions.
- Keep changes focused on the problem being addressed.
- Add or update concise English Javadoc for new or changed APIs.

Apply formatting before submitting changes:

```bash
./mvnw spotless:apply
```

## Tests and coverage

Add or update tests when changing behavior. Follow the naming pattern
`method_Should..._When...`, group related tests by functionality, and keep their
layout consistent with the existing tests.

Run the standard checks:

```bash
./mvnw clean verify
```

To exercise both enabled and disabled logging branches, run:

```bash
./mvnw clean verify '-Dorg.slf4j.simpleLogger.defaultLogLevel=off'
./mvnw verify '-Dorg.slf4j.simpleLogger.defaultLogLevel=trace'
```

## Documentation

Generate the API documentation:

```bash
./mvnw javadoc:javadoc
```

Open `target/reports/apidocs/index.html` in a browser and check any documentation
you changed. MathJax formulas require an internet connection to render.

## Issues and pull requests

For bug reports, include the Java and library versions, expected and actual
behavior, and a minimal reproducible example where possible.

For substantial changes, open an issue first to discuss the proposed approach.

When submitting a pull request:

- Explain the problem and how the change addresses it.
- Describe the checks you ran.
- Keep unrelated changes out of the pull request.

## Releases

CI checks pull requests and pushes to `main`, and uploads test and coverage reports
as workflow artifacts. Releases are published on GitHub Releases, not a Maven registry.

To check the release build locally:

```bash
./mvnw clean verify '-Dorg.slf4j.simpleLogger.defaultLogLevel=off'
./mvnw -Prelease verify javadoc:javadoc '-Dorg.slf4j.simpleLogger.defaultLogLevel=trace'
```

The release profile creates the library, sources, and Javadoc JARs in `target/`.

Before the first release, set **Settings > Pages > Build and deployment > Source**
to **GitHub Actions**. If the `github-pages` environment restricts deployment refs,
allow release tags matching `v*`.

After merging the release changes into `main`, create and push a tag matching the
version in `pom.xml`:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The release workflow validates the tag, runs the checks, publishes the three JARs
with generated release notes, and deploys Javadoc to GitHub Pages. It uses the
automatic `GITHUB_TOKEN`; no publishing token or signing key is required.
