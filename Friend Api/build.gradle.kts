import com.google.gson.Gson
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
val archivesBaseName: Provider<String> = providers.gradleProperty("archives_base_name")
val modVersion: Provider<String> = providers.gradleProperty("mod_version")
val mavenGroup: Provider<String> = providers.gradleProperty("maven_group")
val projectDescription: Provider<String> = providers.gradleProperty("description")

val publishingUrl: Provider<String> = providers.gradleProperty("url")

val licenseName: Provider<String> = providers.gradleProperty("license_name")
val licenseUrl: Provider<String> = providers.gradleProperty("license_url")
val licenseDistribution: Provider<String> = providers.gradleProperty("license_distribution")

val developerId: Provider<String> = providers.gradleProperty("developer_id")
val developerName: Provider<String> = providers.gradleProperty("developer_name")
val developerEmail: Provider<String> = providers.gradleProperty("developer_email")

val publishingStrategy: Provider<String> = providers.gradleProperty("publishing_strategy")

val javaVersion: Provider<Int> = libs.versions.java.map { it.toInt() }

base.archivesName = archivesBaseName
version = modVersion.get()
group = mavenGroup.get()
description = projectDescription.get()

class AccountsJson(val accounts: List<Account>)
class Account(val profile: Profile, val ygg: YGG)
class YGG(val token: String)
class Profile(val name: String, val id: String)

val prismAccountsFile = providers.provider {
    val explicit = providers.gradleProperty("prism.accounts.file").orNull
    if (explicit != null) return@provider File(explicit)

    val home = System.getProperty("user.home")

    val candidates = buildList {
        System.getenv("APPDATA")?.let { add(File(it, "PrismLauncher/accounts.json")) }
        System.getenv("HOMEPATH")?.let { add(File(it, "scoop/persist/prismlauncher/accounts.json")) }
        val xdgDataHome = System.getenv("XDG_DATA_HOME")
        if (xdgDataHome != null) {
            add(File(xdgDataHome, "PrismLauncher/accounts.json"))
        } else {
            add(File(home, ".local/share/PrismLauncher/accounts.json"))
        }
        add(File(home, ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/accounts.json"))
        add(File(home, "Library/Application Support/PrismLauncher/accounts.json"))
    }
    candidates.firstOrNull(File::exists)
}

loom {
    runs {
        prismAccountsFile.orNull?.let { file ->
            val account: Provider<Account> = providers.fileContents(layout.file(providers.provider { file }))
                .asText
                .map { jsonStr ->
                    val accountNumber = (providers.gradleProperty("prism.accounts.number").orNull?.toInt() ?: 1) - 1
                    val accounts = Gson().fromJson(jsonStr, AccountsJson::class.java).accounts
                    accounts.getOrNull(accountNumber.coerceIn(0, accounts.size - 1))
                        ?: error("No PrismLauncher accounts found in ${file.absolutePath}")
                }
            register("clientAuth") {
                inherit(getByName("client"))
                configName = "Minecraft Client (Auth)"
                val acc = account.get()
                programArgs("--username", acc.profile.name, "--uuid", acc.profile.id, "--accessToken", acc.ygg.token)
            }
        }
    }
}

val shade: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    minecraft(libs.minecraft)
    implementation(libs.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)
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
    withType<UpdateDaemonJvm>().configureEach {
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
    named("build") { dependsOn(named("dokkaJar"), shadowJar) }
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
        archiveClassifier = ""
        configurations = listOf(shade)
        val projectPackage = "${mavenGroup.get().lowercase()}.${archiveBaseName.get().lowercase().replace('-', '_')}.shaded"
        relocate("okhttp3", "$projectPackage.okhttp3")
        relocate("okio", "$projectPackage.okio")
        exclude("kotlin/**", "org/intellij/lang/annotations/**", "org/jetbrains/annotations/**")
        minimize()
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
    }
    register<TaskPublishCurseForge>("publishCurseForge") {
        group = "publishing"
        disableVersionDetection()
        apiToken = env.fetch("CURSEFORGE_TOKEN", "")
        val file = upload(880050, jar)
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
    uploadFile.set(tasks.jar)
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