
![ArangoDB](img/arangodb.png)

## Important note of this fork

AQL IntelliJ Plugin seems to be abandoned, so the main goal of this project is to 
keep the plugin working with the latest versions of IntelliJ IDEA and ArangoDB.

What are the next steps?

Actually, this plugin uses Gradle 7.4 and Java 11 with the old IntelliJ SDK, 
so the first step is to update this project to the [minimum required Gradle
version 8.5 and Java 17](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-migration.html#minimum-gradle-and-java-versions).

#  AQL (ArangoDB) language support plugin for Intellij *IDEA* 2021.x IDE

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
