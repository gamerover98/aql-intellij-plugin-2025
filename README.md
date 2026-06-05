
![ArangoDB](img/arangodb.png)

<!-- Plugin description -->
**AQL [ArangoDB](https://www.arangodb.com/) Query Language support** for IntelliJ IDEA.

- Syntax highlighting
- Find usages
- Refactorings
- Quick docs
- Go to usages
- Execute queries
<!-- Plugin description end -->

## Important note of this fork

AQL IntelliJ Plugin seems to be abandoned, so the main goal of this project is to 
keep the plugin working with the latest versions of IntelliJ IDEA and ArangoDB.

**What are the next steps?**

- [x] The project uses Gradle 7.4 and Java 11 with the old IntelliJ SDK,
  so the first step is to update this project to the 
  [minimum required Gradle version 8.5 and Java 17](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-migration.html#minimum-gradle-and-java-versions)
  to be compatible with the 2023 version of IntelliJ IDEA.
- [x] Update Java to 21 to be compatible with the 2025 version of IntelliJ IDEA.

The plugin is now compatibile with the IntelliJ IDEA 2025 but,
even if it starts, it is not fully functional yet.

**Current plugin features:**

- [x] The plugin loads and starts in the IntelliJ IDEA 2025.
- [x] The plugin is able to connect to the ArangoDB server.
- [x] The grammar and syntax highlighting are working.
- [ ] The plugin is able to execute AQL queries and show the results in the UI.
    * Currently, the plugin is able to execute AQL queries, _but it does not show the results in the UI_.

**Next technical steps:**

- [ ] Update the grammarkit library to the latest version.
- [ ] Perhaps, rewrite the plugin in Kotlin.

**New features to be implemented:**

- [ ] Update to the last version of the ArangoDB Java driver.
- [ ] Add support for the latest ArangoDB features.
- [ ] Add support for the latest AQL features.
- [ ] Improve the UI and UX of the plugin.
    * Add a better way to manage connections to the ArangoDB server.
    * Add a "play" button on _.aql_ files to execute the selected query
      (like in the database explorer intellij plugin).
    * TODO ...
- [ ] Add a console to execute AQL queries and show the results in the UI
  (like in the database explorer intellij plugin).

# AQL (ArangoDB) language support plugin for Intellij *IDEA* ~~2021.x~~ 2025.x IDE

## Build

* On Unix: ```gradlew build -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2```
* On Windows (CMD): ```.\gradlew.bat build -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2```
* **Intellij run config**: ```Build```

Note: 
build might take a while because it downloads Intellij Community Edition (300+MB, not sure exactly).

## Run

* On Unix: ```gradlew runIde -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2```
* On Windows (CMD): ```.\gradlew.bat runIde -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2```
* **Intellij run config**: ```Run IDE```

## Find usages
 
![Find usages](img/find_usages.png)


## Docs
 
![Docs](img/quickdoc.png)

## Function arguments
 
![Function arguments](img/arguments.png)

## File editing
![File Editing](img/editor.png)

## Parameter language injection
 
![Query Injection](img/query.png)

## Licence
[Apache License Version 2.0](LICENSE.txt)

---

## Technical & Functional Analysis

### Overview

The **AQL (ArangoDB Query Language) IntelliJ Plugin** provides first-class IDE support for AQL
inside IntelliJ IDEA and other JetBrains IDEs. It integrates deeply with the IntelliJ Platform SDK
to deliver a developer experience comparable to built-in language support: syntax-aware editing,
intelligent code completion, live database navigation, and direct query execution.

This is an actively maintained fork of an originally abandoned plugin, modernized and upgraded to
target IntelliJ Platform 2025.1+.

---

### Current Features

#### 1. Language Support & Syntax Highlighting
- Full tokenization of AQL syntax: keywords (`FOR`, `LET`, `RETURN`, `FILTER`, `COLLECT`,
  `WINDOW`, `UPSERT`, `INSERT`, `UPDATE`, `REPLACE`, `REMOVE`), built-in functions
  (200+, across mathematical, string, date, geo, array, object, type-checking categories),
  variables, bind parameters (`@param`, `@@collection`), operators, strings, numbers, and comments.
- Customizable token colors via **Settings → Editor → Color Scheme → AQL**, including support for
  both light and dark themes.
- Case-insensitive keyword recognition (matching AQL's own semantics).

#### 2. Code Completion
Three complementary completion providers are registered:
- **Keyword Completion** — all AQL reserved words and built-in function names, with priority
  ranking and icons.
- **Database Completion** — dynamically lists collections, named views, and named graphs from the
  currently active ArangoDB connection. Completion items reflect the live database state.
- **Context Completion** — scope-aware suggestions based on the cursor position within the query
  (e.g., variables bound in `FOR` / `LET` clauses are available in subsequent expressions).

#### 3. Parameter Hints & Inline Inlay Hints
- **Ctrl+P / parameter info popup**: Displays the signature and named parameters of any AQL
  built-in function, backed by `AqlFunctionParameters.json` (31 KB catalog of 200+ functions).
- **Inline parameter name hints**: Inlay labels rendered at call sites within the editor, showing
  parameter names for built-in functions without requiring the popup.

#### 4. Find Usages & Reference Resolution
- **PSI reference resolution**: All variable definitions, bind parameters, and property accesses
  are wired into the IntelliJ reference system. "Go to Declaration", "Find Usages", and
  "Highlight Usages in File" work out of the box across `.aql` files in a project.
- Seven distinct reference types are supported: function names, keywords, property IDs, property
  lookups (`.field`), bind parameters (`@param`), placeholders (`${var}`), and system properties
  (`_key`, `_rev`, `_id`, `_from`, `_to`).

#### 5. Rename Refactoring
- Variables, bind parameters, and named functions can be renamed project-wide via
  **Shift+F6**, with all usages updated atomically.

#### 6. Go to Symbol
- **Ctrl+Alt+Shift+N** (Navigate → Symbol) supports searching for AQL named elements across
  all `.aql` files in the project.

#### 7. Structure View
- The **Structure** panel (Alt+7) displays a hierarchical view of the active `.aql` file,
  filtering out low-level tokens and showing meaningful constructs such as variable bindings,
  `RETURN` expressions, and function calls.

#### 8. Quick Documentation
- **Ctrl+Q** on any AQL keyword or built-in function renders inline HTML documentation sourced
  from the `/docs/` resource bundle. Documentation is cached (20-minute TTL) for performance.

#### 9. Connection Management (Tool Window)
- A dedicated **ArangoDB** tool window (right-side panel) allows managing multiple server
  connections simultaneously.
- Each server entry expands into its databases, which in turn expand into collections, named
  graphs, and named views.
- Connection details (host, port, user, SSL) are stored via `DataWindowState`
  (`PersistentStateComponent`) so they survive IDE restarts.
- Passwords are stored securely in the **IDE Password Store** (OS keychain or encrypted file).
- Toolbar actions: **Add Server**, **Set Active Database**, **Refresh Schema**,
  **Expand/Collapse All**.

#### 10. Query Execution & Explain
- **Execute AQL Query** (available via editor right-click → Run) executes the currently open
  `.aql` file against the active ArangoDB database via the official ArangoDB Java Driver (v6.14.0).
- **Explain AQL Query** returns the optimizer execution plan without running the query.
- Queries can carry bind parameters; a dedicated **Parameter Dialog** allows entering named
  parameter values before execution.

#### 11. Live Templates
- A set of AQL-specific live templates (code snippets) is provided, accessible via the standard
  IntelliJ **Live Templates** mechanism (Ctrl+J). Templates are scoped to `.aql` files and
  AQL injection contexts.

#### 12. Language Injection
- AQL can be injected into string literals in Java, Kotlin, and other host languages via the
  **IntelliLang** plugin (optional dependency). Once injected, all AQL language features
  (highlighting, completion, hints) apply within the host file.

#### 13. Code Style & Formatting
- Per-language code style settings for AQL are available under
  **Settings → Editor → Code Style → AQL**.

#### 14. New File Action
- **File → New → AQL File** creates a new `.aql` file from a bundled template, pre-populated
  with a basic `FOR` / `RETURN` skeleton.

---

### Architecture Overview

The plugin follows the standard IntelliJ Platform extension model. All registrations are
declared in `META-INF/plugin.xml` and implemented in Kotlin (migration from Java completed).

```
plugin.xml
  ↓ registers
Language + Lexer + Parser            ← grammars/aql.bnf (Grammar-Kit)
                                       grammars/AqlLexer.flex (JFlex)
                                       → auto-generated src/main/gen/
  ↓ produces
PSI Tree (AqlFile, AqlNamedElement)  ← grammar/custom/psi/ (mixins)
  ↓ consumed by
Editor Features
  Syntax highlighting                 ← syntax/AqlSyntaxHighlighter*.kt
  Code completion (3 providers)       ← editor/AqlCodeCompletionContributor.kt
  Parameter hints                     ← editor/AqlParameterInfoHandler.kt
  Reference resolution                ← lang/psi/ (9 reference types)
  Find usages / rename                ← lang/AqlFindUsagesProvider.kt
  Documentation                       ← lang/AqlDocumentationProvider.kt
  Structure view                      ← lang/AqlPsiStructureViewFactory.kt

DB Integration
  AqlDatabaseService (interface)      ← db/ (project-scoped service)
  AqlDatabaseServiceImpl              ← ArangoDB Java Driver 6.14.0
  AqlDataService                      ← actions/ (orchestrates queries & events)
  DataWindowState                     ← ui/ (PersistentStateComponent for server configs)

UI
  ServerToolWindow                    ← toolWindow/ (JTree + IntelliJ UI DSL)
  AqlServerDialog                     ← ui/dialogs/ (connection management)
  AqlParameterDialog                  ← ui/dialogs/ (bind parameter input)
  AqlConsoleWindow                    ← ui/windows/ (query result view, WIP)

Event Bus
  ActionBusEvent topics               ← actions/ (MessageBus for UI ↔ service decoupling)
  Topics: QueryResult, QueryTreeChange, MetaResult, GraphResult, EmptyLog, RefreshScheme,
          SetActiveDatabase
```

**Key architectural decisions:**
- **Single service entry point**: `AqlDatabaseService` is the only place that touches the
  ArangoDB driver. All completion, execution, and UI code accesses the DB exclusively through
  this project-scoped service.
- **PSI Mixin pattern**: Generated Grammar-Kit PSI classes are augmented via Kotlin mixin classes
  (`grammar/custom/psi/impl/`) rather than modifying generated code. This isolates behavior from
  the generated layer.
- **MessageBus decoupling**: UI components communicate with data services exclusively through
  IntelliJ's `MessageBus` (event bus topics), keeping the tool window and query actions loosely
  coupled.
- **Persistent state**: Server connection configurations are stored in `ArangoDB_DataSource.xml`
  (project-level `.idea/` storage) via `@State` / `@Storage` annotations.
- **Auto-generated grammar**: The lexer and parser are entirely auto-generated. Any AQL language
  changes require editing `aql.bnf` or `AqlLexer.flex` and regenerating via the Grammar-Kit and
  JFlex IDE actions; never edit `src/main/gen/` directly.

---

### Future Roadmap

#### 1. Complete Query Result Visualization
The query execution infrastructure is functional, but the result display panel (`AqlConsoleWindow`)
is unfinished — results are executed but not rendered in a user-friendly way. The highest-impact
near-term improvement is a proper result panel with:
- Tabular display of JSON document results with column auto-detection.
- JSON tree viewer for nested objects.
- Export to CSV / JSON file.
- Pagination for large result sets.

#### 2. AI-Assisted Query Generation & Explanation
Integrate with the **JetBrains AI Assistant** extension point (or Anthropic/OpenAI APIs) to
provide:
- Natural-language-to-AQL query generation ("Find all orders placed in the last 30 days").
- Automatic explanation of complex AQL queries in plain English.
- AI-powered query optimization suggestions based on the active schema.

#### 3. Advanced Schema Visualization
Currently the tool window displays a flat tree of collections, graphs, and views. A richer
visualization would include:
- An interactive graph diagram showing edge collection relationships between document collections.
- Index browser (list existing indexes per collection, with type and fields).
- Collection statistics (document count, disk size) fetched from the `/_api/collection/{name}/count`
  endpoint.

#### 4. Multi-Database & Cluster Awareness
- Support connecting to multiple databases simultaneously and switching the "active" target
  per editor tab rather than globally per project.
- Display ArangoDB Cluster topology (coordinators, DB servers) in the tool window for users
  running cluster deployments.
- Surface cluster health metrics and shard distribution information.

#### 5. Query History, Bookmarks & Snippets
- Persistent query history: automatically save every executed query with timestamp, database
  target, and execution time.
- Bookmark and name frequently used queries for quick access from the tool window.
- Share snippet collections as project-level files (`.aqlsnippets`) for team workflows.

---

### Dependencies & Compatibility

| Component | Version |
|---|---|
| IntelliJ Platform | 2025.1+ (since-build 251) |
| Kotlin | 2.1.21 |
| Java | 21 |
| ArangoDB Java Driver | 6.14.0 |
| Jackson (JSON) | 2.13.0 |
| ArangoDB Server | 3.x (tested up to 3.12) |

---

### Building & Development

```bash
# Build (downloads IntelliJ Community ~300 MB on first run)
gradlew.bat build -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2

# Run plugin in sandboxed IDE instance
gradlew.bat runIde

# Run tests
gradlew.bat test

# Verify plugin compatibility
gradlew.bat verifyPlugin
```

The AQL parser and lexer are auto-generated. After modifying `aql.bnf` or `AqlLexer.flex`,
regenerate via the Grammar-Kit (right-click → Generate Parser Code) and JFlex
(right-click → Run JFlex Generator) IDE actions respectively.
