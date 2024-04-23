import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    id("fabric-loom")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}
base { archivesName.set(project.extra["archives_base_name"] as String) }
version = project.extra["mod_version"] as String
group = project.extra["maven_group"] as String
repositories {}
dependencies {
    minecraft("com.mojang", "minecraft", project.extra["minecraft_version"] as String)
    mappings("net.fabricmc", "yarn", project.extra["yarn_mappings"] as String, null, "v2")
    modImplementation("net.fabricmc", "fabric-loader", project.extra["loader_version"] as String)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", project.extra["fabric_version"] as String)
    modImplementation("net.fabricmc", "fabric-language-kotlin", project.extra["fabric_language_kotlin_version"] as String)
    shadow(implementation("com.squareup.okhttp3", "okhttp", project.extra["okhttp_version"] as String))
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = base.archivesName.get()
        }
    }
}
tasks {
    val javaVersion = JavaVersion.toVersion((project.extra["java_version"] as String).toInt())
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<Javadoc>().configureEach { options.encoding = "UTF-8" }
    withType<Test>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<KotlinCompile>().configureEach { kotlinOptions { jvmTarget = javaVersion.toString() } }
    jar {
        from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } }
        archiveClassifier.set("dev")
    }
    processResources {
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.extra["mod_version"] as String, "fabricloader" to project.extra["loader_version"] as String, "fabric_api" to project.extra["fabric_version"] as String, "fabric_language_kotlin" to project.extra["fabric_language_kotlin_version"] as String, "minecraft" to project.extra["minecraft_version"] as String, "java" to project.extra["java_version"] as String)) }
        filesMatching("*.mixins.json") { expand(mutableMapOf("java" to project.extra["java_version"] as String)) }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        withSourcesJar()
    }
    shadowJar {
        archiveClassifier.set("dev")
        configurations = listOf(project.configurations.shadow.get())
        relocate("okhttp3", "${project.group.toString().lowercase()}.${base.archivesName.get().lowercase().replace('-', '_')}.shaded.okhttp3")
    }
    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
    }
}