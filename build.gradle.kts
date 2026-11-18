plugins {
    id("java")
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.20.0"
}

group = "com.github.todd"
version = "1.0.1"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        pluginVerifier()
        zipSigner()
    }

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.todd.hammersortjson"
        name = "Hammer Sort"
        version = project.version.toString()
        vendor {
            name = "Todd"
        }
        description = """
            <h3>Hammer Sort</h3>
            <p>
                A universal structured sorting plugin for PhpStorm and IntelliJ Platform IDEs. It effortlessly organizes and alphabetizes keys, properties, and members across <b>JSON</b>, <b>TypeScript interfaces & type aliases</b>, <b>TypeScript enums</b>, <b>JavaScript object literals</b>, <b>PHP associative arrays</b>, <b>Python dictionaries</b>, and arbitrary selected lines of code.
            </p>
            <p>
                Maintaining clean, alphabetically sorted code makes diffs clearer, prevents merge conflicts, and makes large configuration files and type definitions much easier to scan. Unlike basic line sorters, Hammer Sort is syntax-aware: it preserves comments (JSDoc, block comments, and inline trailing comments), respects TypeScript modifiers (<code>readonly</code>, <code>public</code>, <code>?</code>), handles trailing commas/semicolons seamlessly, and supports both shallow and deep recursive sorting with a single keystroke.
            </p>
            <h4>Key Features:</h4>
            <ul>
                <li><b>TypeScript Interfaces & Types:</b> Sorts <code>interface Foo { ... }</code>, <code>type Foo = { ... }</code>, and <code>enum Foo { ... }</code> ignoring modifiers like <code>readonly</code> or <code>?</code>.</li>
                <li><b>JSON & JSON5/JSONC:</b> Automatic comma placement ensures the last item never has an invalid trailing comma syntax error.</li>
                <li><b>JavaScript Objects & Spreads:</b> Preserves spread properties (<code>...defaults</code> stays at top, <code>...overrides</code> stays at bottom).</li>
                <li><b>PHP Arrays & Python Dictionaries:</b> Full support for <code>[ 'key' => 'val' ]</code>, <code>array(...)</code>, and Python <code>dict</code>.</li>
                <li><b>Bare Selection Sorting:</b> Highlight any subset of property lines without braces to sort them directly.</li>
                <li><b>Comment Preservation:</b> Leading JSDoc/line comments and trailing inline comments remain attached to their fields.</li>
                <li><b>Shallow & Recursive:</b> Sort top-level keys or recursively sort nested structures.</li>
            </ul>
            <h4>Keyboard Shortcuts:</h4>
            <ul>
                <li><b>Sort Keys (Shallow):</b> <kbd>Shift+Option+S</kbd> (macOS) / <kbd>Shift+Alt+S</kbd> (Win/Linux)</li>
                <li><b>Sort Keys (Recursively):</b> <kbd>Shift+Option+Ctrl+S</kbd> (macOS) / <kbd>Shift+Alt+Ctrl+S</kbd> (Win/Linux)</li>
                <li><b>Menus:</b> Right-click editor context menu → <b>Hammer Sort</b>, or via <b>Edit</b> and <b>Code</b> menus</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
