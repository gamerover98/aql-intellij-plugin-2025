
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
