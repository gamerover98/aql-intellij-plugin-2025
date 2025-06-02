import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import net.researchgate.release.ReleaseExtension

plugins {
    java
    idea
    id("org.jetbrains.intellij") version "0.7.3"
    id("org.jetbrains.grammarkit") version "2021.1.3"
    id("net.researchgate.release") version "2.8.1"
}

repositories {
    mavenCentral()
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://www.jetbrains.com/intellij-repository/snapshots")
    maven(url = "https://plugins.gradle.org/m2/")
    maven(url = "https://www.jitpack.io")
}

group = "com.arangodb"

java.sourceCompatibility = JavaVersion.VERSION_11

val ideaVersionExternDefinition: String? = System.getenv("IDEA_VERSION")
val ideaVersion: String by extra {
    ideaVersionExternDefinition ?: (findProperty("ideaVersion") as? String ?: "2021.1")
}

val isEAP = ideaVersion.contains("LATEST-EAP-SNAPSHOT") || ideaVersion.take(4).contains(".")
val artifactVersion = "$version-${if (isEAP) "EAP" else ideaVersion}"

println("ideaVersion: $ideaVersion")
println("artifactVersion: $artifactVersion")

dependencies {
    testRuntimeOnly("junit:junit:4.13.2")
    implementation("com.arangodb:arangodb-java-driver:6.14.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
}

tasks.withType<PatchPluginXmlTask> {
    version(artifactVersion)
    if (isEAP || (ideaVersion == (findProperty("lastReleaseVersion") as? String ?: ""))) {
        untilBuild("")
    }
}

intellij {
    version = ideaVersion
    println("Intellij version: $version")

    pluginName = findProperty("pluginName") as? String ?: "AQL"
    updateSinceUntilBuild = true
    sameSinceUntilBuild = false
    sandboxDirectory = "${rootProject.projectDir}/idea-sandbox/idea-$ideaVersion"

    setPlugins("java")
}

configure<ReleaseExtension> {
    revertOnFail = true
    failOnCommitNeeded = true
    failOnPublishNeeded = true
    failOnUpdateNeeded = true
    failOnSnapshotDependencies = true
    /* pushReleaseVersionBranch = false*/
    failOnUnversionedFiles = true

    buildTasks = listOf("build")

    tagTemplate = "$name-$version"
    preCommitText = "[Gradle release]"
    preTagCommitMessage = "Gradle Pre tag commit: "
    tagCommitMessage = "Gradle Creating tag: "
    newVersionCommitMessage = " New version commit: "
    
    //git {
    //    requireBranch = "master"
    //    pushToRemote = "origin"
    //}
}
