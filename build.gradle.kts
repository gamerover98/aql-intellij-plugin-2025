//import net.researchgate.release.ReleaseExtension

plugins {
    java
    idea
    kotlin("jvm") version libs.versions.kotlinJvm apply true
    id("org.jetbrains.intellij.platform") version libs.versions.intellijPlatform apply true
    id("org.jetbrains.grammarkit") version libs.versions.jetbrainsGrammarkit apply true
    //id("net.researchgate.release") version "2.8.1" <-- Should be updated to the latest version if used
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

group = "com.arangodb"

sourceSets.main {
    java.srcDirs(
        "src/main/java",
        "src/main/kotlin"
    )
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(property("intellijIdeaCommunityVersion"))
    }

    // Plugin dependencies
    implementation(libs.jacksonCore)
    implementation(libs.jacksonDatabind)
    implementation(libs.arangoDriver) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    // Test dependencies
    testRuntimeOnly(libs.junit)
}

intellijPlatform {
    pluginConfiguration {
        id = property("pluginId")
        name = property("pluginName")
        version = property("pluginVersion")
        description = property("pluginDescription")

        vendor {
            name = property("pluginVendorName")
            email = property("pluginVendorEmail")
            url = property("pluginVendorUrl")
        }

        changeNotes = property("changeNotes")

        ideaVersion {
            sinceBuild = property("sinceBuild")
        }
    }
}

fun property(name: String): String {
    return findProperty(name) as? String ?: error("Property '$name' is not set")
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
