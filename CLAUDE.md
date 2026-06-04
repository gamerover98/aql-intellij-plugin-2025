# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IntelliJ IDEA plugin for ArangoDB Query Language (AQL) support. A maintained fork of an abandoned plugin, modernized for IntelliJ IDEA 2025.x. Provides syntax highlighting, code completion, ArangoDB connection management, and query execution.

## Build & Run Commands

```bash
# Build (downloads IntelliJ Community ~300MB on first run)
./gradlew build -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2

# Run plugin in a sandboxed IDE instance
./gradlew runIde

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.arangodb.intellij.aql.grammar.AqlParserTest"

# Verify plugin against IntelliJ compatibility
./gradlew verifyPlugin
```

On Windows use `gradlew.bat` instead of `./gradlew`.

**Key versions:** Java 21, IntelliJ Platform 2025.1 (IC), Kotlin 2.1.21, Gradle 8.5+.

## Parser/Lexer Generation

The AQL parser and lexer are **auto-generated** — do not edit files in `src/main/gen/` directly:

- `src/main/resources/grammars/aql.bnf` → Grammar-Kit generates parser Java classes → `src/main/gen/`
- `src/main/resources/grammars/AqlLexer.flex` → JFlex generates lexer → `src/main/gen/`

After modifying `.bnf` or `.flex` files, regenerate via the Grammar-Kit and JFlex IDE actions (right-click in the file).

Custom behavior is added via **PSI mixins** in `src/main/java/com/arangodb/intellij/aql/grammar/custom/psi/` rather than editing generated code.

## Architecture

The codebase is mixed Kotlin/Java (active migration to Kotlin underway). Language registration is mostly Kotlin; database integration and completion are mostly Java.

### Layer Stack

```
plugin.xml (META-INF)
    ↓ registers
Language + Parser + Lexer        ← grammars/, lang/AqlParserDefinition.kt
    ↓ produces
PSI tree (AqlFile, AqlElements)  ← grammar/custom/psi/, lang/psi/
    ↓ consumed by
Editor features:
  Syntax highlighting             ← syntax/AqlSyntaxHighlighter*.kt
  Code completion                 ← editor/AqlCodeCompletionContributor.java
  Find usages / references        ← lang/AqlFindUsagesProvider.kt, lang/psi/
  Documentation                   ← lang/AqlDocumentationProvider.kt
  Parameter hints                 ← editor/AqlParameterInfoHandler.java
DB integration:
  AqlDatabaseService              ← db/ (ArangoDB Java Driver 6.14.0)
  Query execution/explain         ← actions/
UI:
  Tool window (connections/DBs)   ← toolWindow/ServerToolWindow*.kt
  Dialogs (server, parameters)    ← ui/dialogs/
  Query console                   ← ui/windows/AqlConsoleWindow.java
```

### Key Architectural Points

- **`plugin.xml`** is the authoritative registry for all extensions, actions, services, and tool windows. Always check it when adding new features.
- **`AqlDatabaseService`** (`db/`) is the single entry point for all ArangoDB operations. It is a project-scoped service registered in `plugin.xml`.
- **`ArangoBundle.kt`** + `messages/ArangoBundle.properties` handle all UI strings — add new UI text there.
- **`AqlFunctionParameters.json`** (31KB in resources) drives function parameter hints and completion for built-in AQL functions.
- **`src/main/gen/`** is gitignored generated code. If it's missing, regenerate from the grammar files.

### Source Layout (non-obvious split)

- `src/main/kotlin/.../lang/` — language registration, PSI references, structure view
- `src/main/kotlin/.../syntax/` — syntax highlighting, commenting, code style
- `src/main/kotlin/.../toolWindow/` — right-side ArangoDB connections panel
- `src/main/java/.../editor/` — all code completion and parameter hints
- `src/main/java/.../db/` — ArangoDB driver integration
- `src/main/java/.../actions/` — query execute/explain actions
- `src/main/java/.../ui/` — dialogs, renderers, console window

## Testing

Tests use JUnit 4 with the IntelliJ platform test framework (`LightPlatformTestCase`, `ParsingTestCase`). Test data (`.aql` files and expected PSI dumps) lives in `src/test/resources/testData/`.

- Parser tests extend `ParsingTestCase` and compare PSI output against checked-in text files.
- Feature tests (find usages, commenter) use `LightPlatformCodeInsightTestCase`.

## Known Incomplete Areas

- Query execution results do not yet display in the UI (the execute action runs but the result view is unfinished).
- `AqlCreateRepositoryIntention` is temporarily disabled (see TODO in code).
- The plugin is partially migrated from Java to Kotlin — new code should be written in Kotlin.
