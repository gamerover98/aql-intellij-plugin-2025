
![ArangoDB](img/arangodb.png)

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

<!-- Plugin description -->
**AQL [ArangoDB](https://www.arangodb.com/) Query Language support** for IntelliJ IDEA.

- Syntax highlighting
- Find usages
- Refactorings
- Quick docs
- Go to usages
- Execute queries
<!-- Plugin description end -->
<!-- Plugin changeNotes -->
### Changelog

#### 1.0.8
- Add `WINDOW` keyword
- Add remaining v3.7 and v3.8 functions:
  - `IPV4_FROM_NUMBER`, `REPLACE_NTH`, `NGRAM_MATCH`, `NGRAM_SIMILARITY`
  - `NGRAM_POSITIONAL_SIMILARITY`, `DATE_UTCTOLOCAL`, `DATE_LOCALTOUTC`
  - `BIT_AND`, `BIT_OR`, `BIT_XOR`, `BIT_NEGATE`, `BIT_TEST`, `BIT_POPCOUNT`, `BIT_SHIFT_LEFT`
  - `BIT_SHIFT_RIGHT`, `BIT_CONSTRUCT`, `BIT_DECONSTRUCT`, `BIT_TO_STRING`, `BIT_FROM_STRING`
  - `GEO_IN_RANGE`, `DATE_TIMEZONE`, `DATE_TIMEZONES`
- Fix syntax highlighting (+ function type "relaxing")

#### 1.0.7
- SSL connection support, 3.8.x database support, bugfixes

#### 1.0.6

#### 1.0.5
- Bug fixes, add new analyzers, keywords and functions (grammar/autocomplete), add function parameter hints (CTRL+P), add live template support

#### 1.0.4
- Add 2019.2+ support, use password manager to store passwords

#### 1.0.3
- Bug fixes, create SpringData Repository interface intention

#### 1.0.2
- Bug fixes

#### 1.0.1
- Bug fixes (syntax highlighting, documentation)

#### 1.0.0
- Initial release (syntax highlighting, autocompletion, refactorings, spring data query language injection)
<!-- Plugin changeNotes end -->