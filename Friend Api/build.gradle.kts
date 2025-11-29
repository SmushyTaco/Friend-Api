import net.darkhax.curseforgegradle.TaskPublishCurseForge
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.minotaur)
    alias(libs.plugins.curseForgeGradle)
    alias(libs.plugins.dotenv)
    alias(libs.plugins.dokka)
    alias(libs.plugins.yumiGradleLicenser)
    alias(libs.plugins.nmcp)
    `maven-publish`
    signing
}
val archivesBaseName = providers.gradleProperty("archives_base_name")
val modVersion = providers.gradleProperty("mod_version")
val mavenGroup = providers.gradleProperty("maven_group")
val projectDescription = providers.gradleProperty("description")

val publishingUrl = providers.gradleProperty("url")

val licenseName = providers.gradleProperty("license_name")
val licenseUrl = providers.gradleProperty("license_url")
val licenseDistribution = providers.gradleProperty("license_distribution")

val developerId = providers.gradleProperty("developer_id")
val developerName = providers.gradleProperty("developer_name")
val developerEmail = providers.gradleProperty("developer_email")

val publishingStrategy = providers.gradleProperty("publishing_strategy")

val javaVersion = libs.versions.java.map { it.toInt() }

base.archivesName = archivesBaseName
version = modVersion.get()
group = mavenGroup.get()
description = projectDescription.get()
val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)
    implementation(libs.okhttp)
    shade(libs.okhttp)
}
java {
    toolchain {
        languageVersion = javaVersion.map { JavaLanguageVersion.of(it) }
        vendor = JvmVendorSpec.ADOPTIUM
    }
    sourceCompatibility = JavaVersion.toVersion(javaVersion.get())
    targetCompatibility = JavaVersion.toVersion(javaVersion.get())
    withSourcesJar()
}
dokka {
    dokkaSourceSets.configureEach {
        reportUndocumented = true
    }
}
val licenseFile = run {
    val rootLicense = layout.projectDirectory.file("LICENSE")
    val parentLicense = layout.projectDirectory.file("../LICENSE")
    when {
        rootLicense.asFile.exists() -> {
            logger.lifecycle("Using LICENSE from project root: {}", rootLicense.asFile)
            rootLicense
        }
        parentLicense.asFile.exists() -> {
            logger.lifecycle("Using LICENSE from parent directory: {}", parentLicense.asFile)
            parentLicense
        }
        else -> {
            logger.warn("No LICENSE file found in project or parent directory.")
            null
        }
    }
}
tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.get().toString()
        targetCompatibility = javaVersion.get().toString()
        if (javaVersion.get() > 8) options.release = javaVersion
    }
    named<UpdateDaemonJvm>("updateDaemonJvm") {
        languageVersion = libs.versions.gradleJava.map { JavaLanguageVersion.of(it.toInt()) }
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<Javadoc>().configureEach { options.encoding = "UTF-8" }
    withType<Test>().configureEach { defaultCharacterEncoding = "UTF-8" }
    register<Jar>("dokkaJar") {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        dependsOn(dokkaGenerateHtml)
        archiveClassifier = "javadoc"
        from(layout.buildDirectory.dir("dokka/html"))
    }
    named("build") { dependsOn(named("dokkaJar")) }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            extraWarnings = true
            jvmTarget = javaVersion.map { JvmTarget.valueOf("JVM_${if (it == 8) "1_8" else it}") }
        }
    }
    withType<Jar>().configureEach {
        licenseFile?.let {
            from(it) {
                rename { original -> "${original}_${archiveBaseName.get()}" }
            }
        }
    }
    shadowJar {
        archiveClassifier = "dev"
        configurations = listOf(shade)
        val projectPackage = "${mavenGroup.get().lowercase()}.${archiveBaseName.get().lowercase().replace('-', '_')}.shaded"
        relocate("okhttp3", "$projectPackage.okhttp3")
        relocate("okio", "$projectPackage.okio")
        exclude("kotlin/**", "org/intellij/lang/annotations/**", "org/jetbrains/annotations/**")
        minimize()
    }
    remapJar {
        dependsOn(shadowJar)
        inputFile = shadowJar.get().archiveFile
    }
    processResources {
        val resourceMap = mapOf(
            "version" to modVersion.get(),
            "fabricloader" to libs.versions.loader.get(),
            "fabric_api" to libs.versions.fabric.api.get(),
            "fabric_language_kotlin" to libs.versions.fabric.language.kotlin.get(),
            "minecraft" to libs.versions.minecraft.get(),
            "java" to libs.versions.java.get()
        )
        inputs.properties(resourceMap)
        filesMatching("fabric.mod.json") { expand(resourceMap) }
        filesMatching("**/*.mixins.json") { expand(resourceMap.filterKeys { it == "java" }) }
    }
    register<TaskPublishCurseForge>("publishCurseForge") {
        group = "publishing"
        disableVersionDetection()
        apiToken = env.fetch("CURSEFORGE_TOKEN", "")
        val file = upload(880050, remapJar)
        file.displayName = "[${libs.versions.minecraft.get()}] Friend Api"
        file.addEnvironment("Client")
        file.changelog = ""
        file.releaseType = "release"
        file.addModLoader("Fabric")
        file.addGameVersion(libs.versions.minecraft.get())
    }
}
modrinth {
    token = env.fetch("MODRINTH_TOKEN", "")
    projectId = "friend-api"
    uploadFile.set(tasks.remapJar)
    gameVersions.add(libs.versions.minecraft)
    versionName = libs.versions.minecraft.map { "[$it] Friend Api" }
    dependencies { required.project("fabric-api", "fabric-language-kotlin") }
}
license {
    rule(file("./HEADER"))
    include("**/*.kt")
    exclude("**/*.properties")
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = mavenGroup.get()
            artifactId = archivesBaseName.get()
            version = modVersion.get()
            artifact(tasks.named("dokkaJar"))
            pom {
                name = archivesBaseName
                description = projectDescription
                url = publishingUrl

                licenses {
                    license {
                        name = licenseName
                        url = licenseUrl
                        distribution = licenseDistribution
                    }
                }
                developers {
                    developer {
                        id = developerId
                        name = developerName
                        email = developerEmail
                    }
                }
                scm {
                    url = publishingUrl
                    connection = publishingUrl.map { "scm:git:$it.git" }
                    developerConnection = publishingUrl.map { "scm:git:$it.git" }
                }
            }
        }
    }
}
signing {
    val keyFile = layout.projectDirectory.file("./private-key.asc")
    if (keyFile.asFile.exists()) {
        isRequired = true
        useInMemoryPgpKeys(
            providers.fileContents(keyFile).asText.get(),
            env.fetch("PASSPHRASE", "")
        )
        sign(publishing.publications)
    }
}
nmcp {
    publishAllPublicationsToCentralPortal {
        username = env.fetch("USERNAME_TOKEN", "")
        password = env.fetch("PASSWORD_TOKEN", "")
        publishingType = publishingStrategy
    }
}