
![ArangoDB](img/arangodb.png)

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
