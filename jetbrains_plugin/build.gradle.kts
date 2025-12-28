// Copyright 2009-2025 Weibo, Inc.
// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: APACHE2.0
// SPDX-License-Identifier: Apache-2.0

// Convenient for reading variables from gradle.properties
fun properties(key: String) = providers.gradleProperty(key)

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    id("org.jetbrains.intellij") version "1.13.3"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

apply(from = "genPlatform.gradle")

// Use Java/Kotlin toolchains instead of per-task source/target
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

// ------------------------------------------------------------
// The 'debugMode' setting controls how plugin resources are prepared during the build process.
// It supports the following three modes:
//
// 1. "idea" — Local development mode (used for debugging VSCode plugin integration)
//    - Copies theme resources from src/main/resources/themes to:
//        ../debug-resources/<vscodePlugin>/src/integrations/theme/default-themes/
//    - Automatically creates a .env file, which the Extension Host (Node.js side) reads at runtime.
//    - Enables the VSCode plugin to load resources from this directory for integration testing.
//    - Typically used when running IntelliJ with an Extension Host for live debugging and hot-reloading.
//
// 2. "release" — Production build mode (used to generate deployment artifacts)
//    - Requires platform.zip to exist, which can be retrieved via git-lfs or generated with genPlatform.gradle.
//    - This file includes the full runtime environment for VSCode plugins (e.g., node_modules, platform.txt).
//    - The zip is extracted to build/platform/, and its node_modules take precedence over other dependencies.
//    - Copies compiled extension_host outputs (dist, package.json, node_modules) and plugin resources.
//    - The result is a fully self-contained package ready for deployment across platforms.
//
// 3. "none" (default) — Lightweight mode (used for testing and CI)
//    - Does not rely on platform.zip or prepare VSCode runtime resources.
//    - Only copies the plugin's core assets such as themes.
//    - Useful for early-stage development, static analysis, unit tests, and continuous integration pipelines.
//
// How to configure:
//   - Set via gradle argument: -PdebugMode=idea / release / none
//     Example: ./gradlew prepareSandbox -PdebugMode=idea
//   - Defaults to "none" if not explicitly set.
// ------------------------------------------------------------
// Extra properties (Kotlin DSL style)
val ext = project.extensions.extraProperties
ext.set("debugMode", findProperty("debugMode") ?: "none")
ext.set("debugResource", project.projectDir.resolve("../debug-resources").absolutePath)
ext.set("vscodePlugin", findProperty("vscodePlugin") ?: "codex")

// Strongly-typed providers (avoid stringly-typed ext lookups during configuration)
val debugModeProp = providers.gradleProperty("debugMode").orElse("none")
val vscodePluginProp = providers.gradleProperty("vscodePlugin").orElse("codex")

fun Sync.prepareSandbox() {
    // Read once during configuration; values also wired as task inputs below
    val debugMode = debugModeProp.get()
    val vsCodePluginName = vscodePluginProp.get()

    // ---- Copy logging helpers ----
    val copyOps = mutableListOf<Pair<String, String>>() // (src, dest)

    fun resolvedDestPath(rawDest: String, destinationDir: File): File {
        return if (File(rawDest).isAbsolute) File(rawDest) else File(destinationDir, rawDest)
    }

    fun Sync.copyAndTrack(
        src: Any,
        dest: String,
        createDestIfMissing: Boolean = false,
        configure: CopySpec.() -> Unit = {}
    ) {
        if (createDestIfMissing) {
            val destFile = resolvedDestPath(dest, destinationDir)
            if (!destFile.exists()) {
                destFile.mkdirs()
                logger.lifecycle("[prepareSandbox] Created missing destination directory: ${destFile.absolutePath}")
            }
        }
        logger.lifecycle("[prepareSandbox] Scheduling copy: $src -> $dest")
        val destFile = resolvedDestPath(dest, destinationDir)
        if (File(dest).isAbsolute) {
            // Absolute dest: perform copy outside of this Sync's destinationDir
            doLast {
                logger.lifecycle("[prepareSandbox] Executing external copy: $src -> ${destFile.absolutePath}")
                project.copy {
                    from(src)
                    into(destFile)
                    configure.invoke(this)
                }
            }
        } else {
            // Relative dest: use this Sync's copy spec
            from(src) {
                into(dest)
                configure.invoke(this)
            }
        }
        copyOps.add(src.toString() to dest)
    }

    fun Sync.copyNodeModulesFiltered(srcDir: String, destDir: String, patterns: List<String>) {
        copyAndTrack(srcDir, destDir) {
            patterns.forEach { include(it) }
        }
    }

    doFirst {
        logger.lifecycle("[prepareSandbox] Starting with debugMode='${debugMode}'")
    }

    // Set duplicate strategy to include files, with later sources taking precedence
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    val sandboxDir = intellij.pluginName.get()

    // Themes
    val themesDir = "${project.projectDir.absolutePath}/src/main/resources/themes/"
    if (!File(themesDir).exists()) {
        throw IllegalStateException("missing themes dir")
    }

    // Common
    val vscodePluginDir = File("./plugins/${vsCodePluginName}")
    if (!vscodePluginDir.exists()) {
        throw IllegalStateException("missing plugin dir")
    }

    val depPatterns = mutableListOf<String>()
    val depfile = File("prodDep.txt")
    if (!depfile.exists()) {
        throw IllegalStateException("missing prodDep.txt")
    }
    depfile.readLines().let {
        it.forEach { line ->
            depPatterns.add(line.substringAfterLast("node_modules/") + "/**")
        }
    }

    if (debugMode == "idea") {
        val debugResourceDir = layout.projectDirectory.dir("../debug-resources")
        if (!debugResourceDir.asFile.exists()) {
            mkdir(debugResourceDir)
        }
        val debugDir = debugResourceDir.asFile.absolutePath
        copyAndTrack("${vscodePluginDir.path}/extension","${debugDir}/${vsCodePluginName}", createDestIfMissing = true)
        if (File("${debugDir}/${vsCodePluginName}/src").exists()) {
            copyAndTrack(themesDir, "${debugDir}/${vsCodePluginName}/src/integrations/theme/default-themes/")
        } else {
            copyAndTrack(themesDir, "${debugDir}/${vsCodePluginName}/integrations/theme/default-themes/")

        }
        copyAndTrack(themesDir, "${debugDir}/themes/", createDestIfMissing = true)
        copyAndTrack("../extension_host/dist", "${debugDir}/runtime/", createDestIfMissing = true)
        copyAndTrack("../extension_host/package.json", "${debugDir}/runtime/")
        copyNodeModulesFiltered("../extension_host/node_modules", "${debugDir}/node_modules/", depPatterns)
    } else {
        copyAndTrack("../extension_host/dist", "${sandboxDir}/runtime/")
        copyAndTrack("../extension_host/package.json", "${sandboxDir}/runtime/")
        copyNodeModulesFiltered("../extension_host/node_modules", "${sandboxDir}/node_modules/", depPatterns)
        copyAndTrack("${vscodePluginDir.path}/extension", "${sandboxDir}/${vsCodePluginName}")
        copyAndTrack("src/main/resources/themes/", "${sandboxDir}/${vsCodePluginName}/integrations/theme/default-themes/")
        copyAndTrack("src/main/resources/themes/", "${sandboxDir}/themes/")

        // The platform.zip file required for release mode is associated with the code in ../base/vscode, currently using version 1.100.0. If upgrading this code later
        // Need to modify the vscodeVersion value in gradle.properties, then execute the task named genPlatform, which will generate a new platform.zip file for submission
        // To support new architectures, modify according to the logic in genPlatform.gradle script
        if (debugMode == "release") {
            // Check if platform.zip file exists and is larger than 1MB, otherwise throw exception
            val platformZip = File("platform.zip")
            if (platformZip.exists() && platformZip.length() >= 1024 * 1024) {
                // Extract platform.zip to the platform subdirectory under the project build directory
                val platformDir = File("${project.buildDir}/platform")
                platformDir.mkdirs()
                logger.lifecycle("[prepareSandbox] Extracting platform.zip -> ${platformDir.absolutePath}")
                copy {
                    from(zipTree(platformZip))
                    into(platformDir)
                }
                copyOps.add(platformZip.absolutePath to platformDir.absolutePath)
            } else {
                throw IllegalStateException("platform.zip file does not exist or is smaller than 1MB. This file is supported through git lfs and needs to be obtained through git lfs")
            }

            copyAndTrack(File(project.buildDir, "platform/platform.txt"), "${sandboxDir}/")
            // Copy platform node_modules last to ensure it takes precedence over extension_host node_modules
            copyAndTrack(File(project.buildDir, "platform/node_modules"), "${sandboxDir}/node_modules")
        }

        doLast {
            File("${destinationDir}/${sandboxDir}/${vsCodePluginName}/.env").createNewFile()
        }
    }

    doLast {
        logger.lifecycle("[prepareSandbox] Completed with debugMode='${debugMode}'. Summary:")
        copyOps.forEach { (src, dest) ->
            val target = resolvedDestPath(dest, destinationDir)
            val ok = if (target.exists()) {
                if (target.isDirectory) target.list()?.isNotEmpty() == true else true
            } else false
            val status = if (ok) "SUCCESS" else "FAIL"
            logger.lifecycle("[prepareSandbox] ${status}: ${src} -> ${target.absolutePath}")
        }
    }
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    plugins.set(
        listOf(
            "com.intellij.java",
            // Add JCEF support
            "org.jetbrains.plugins.terminal"
        )
    )
}

tasks {

    // Create task for generating configuration files
    register("generateConfigProperties") {
        description = "Generate properties file containing plugin configuration"
        doLast {
            val configDir = File("$projectDir/src/main/resources/com/sina/weibo/agent/plugin/config")
            configDir.mkdirs()

            val configFile = File(configDir, "plugin.properties")
            configFile.writeText("debug.mode=${ext.get("debugMode")}")
            configFile.appendText("\n")
            configFile.appendText("debug.resource=${ext.get("debugResource")}")
            println("Configuration file generated: ${configFile.absolutePath}")
        }
    }

    prepareSandbox {
        // Wire task inputs for build cache/key stability
        inputs.property("build_mode", debugModeProp)
        inputs.property("vscode_plugin", vscodePluginProp)
        prepareSandbox()
    }

    // Generate configuration file before compilation
    withType<JavaCompile> {
        dependsOn("generateConfigProperties")
    }

    // Set the JVM compatibility versions
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn("generateConfigProperties")
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

// Configure ktlint
ktlint {
    version.set("0.50.0")
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    enableExperimentalRules.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// Configure detekt
detekt {
    toolVersion = "1.23.4"
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false

    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}