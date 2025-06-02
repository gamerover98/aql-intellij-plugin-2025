//import net.researchgate.release.ReleaseExtension

plugins {
    java
    idea
    id("org.jetbrains.intellij.platform") version "2.6.0"
    //id("org.jetbrains.intellij.platform.migration") version "2.6.0"
    id("org.jetbrains.grammarkit") version "2021.1.3"
    //id("net.researchgate.release") version "2.8.1"
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

group = "com.arangodb"

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(findProperty("intellijIdeaCommunityVersion") as String)
    }

    testRuntimeOnly("junit:junit:4.13.2")
    implementation("com.arangodb:arangodb-java-driver:6.14.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
}

intellijPlatform {
    pluginConfiguration {
        id = findProperty("pluginId") as String
        name = findProperty("pluginName") as String
        version = findProperty("pluginVersion") as String
        description = findProperty("pluginDescription") as String

        vendor {
            name = findProperty("pluginVendorName") as String
            email = findProperty("pluginVendorEmail") as String
            url = findProperty("pluginVendorUrl") as String
        }

        changeNotes = findProperty("changeNotes") as String

        ideaVersion {
            sinceBuild = findProperty("sinceBuild") as String
        }
    }
}

//configure<ReleaseExtension> {
//    revertOnFail = true
//    failOnCommitNeeded = true
//    failOnPublishNeeded = true
//    failOnUpdateNeeded = true
//    failOnSnapshotDependencies = true
//    /* pushReleaseVersionBranch = false*/
//    failOnUnversionedFiles = true
//
//    buildTasks = listOf("build")
//
//    tagTemplate = "$name-$version"
//    preCommitText = "[Gradle release]"
//    preTagCommitMessage = "Gradle Pre tag commit: "
//    tagCommitMessage = "Gradle Creating tag: "
//    newVersionCommitMessage = " New version commit: "
//
//    //git {
//    //    requireBranch = "master"
//    //    pushToRemote = "origin"
//    //}
//}
