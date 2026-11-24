# Plan: Validate Generated Structurizr DSL

## Goal

Add parser-based tests so the generated Structurizr DSL is checked for syntax and model validity, beyond the renderer's current exact-string tests.

Structurizr documents a Java parser in the `structurizr-dsl` library. Tests can parse a workspace file with `StructurizrDslParser` and inspect the resulting workspace. See the [Structurizr DSL and Java guide](https://docs.structurizr.com/dsl/cookbook/dsl-and-code/).

## Changes

- Add `com.structurizr:structurizr-dsl` as a test-scope dependency, pinned to a stable version compatible with the project.
- In tests, write generated DSL to a temporary `.dsl` file and parse it with `StructurizrDslParser`.
- Parse the checked-in sample DSL and output from both Structurizr API routes.
- Assert the parsed workspace contains the expected system, containers, relationships, views, and—when present—deployment environment and dynamic views.
- Add representative cases for minimal models and escaped text. Keep exact-string tests to catch unintended output changes.
- Update the README to describe the DSL as parser-tested after validation is implemented. Keep parser execution out of production request handling.

## Acceptance Criteria

- `mvn verify` passes with parser validation for the sample and API outputs.
- Syntax or reference errors in generated DSL fail the test suite.
- Models without deployment nodes still parse successfully.
- Parser tests require no Structurizr server or CLI.
