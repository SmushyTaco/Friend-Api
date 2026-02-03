# Friend Api
[![Maven Central](https://img.shields.io/maven-central/v/com.smushytaco/friend-api.svg?label=maven%20central)](https://central.sonatype.com/artifact/com.smushytaco/friend-api)
[![Dokka Docs](https://img.shields.io/badge/docs-dokka-brightgreen.svg)](https://smushytaco.github.io/Friend-Api)

CurseForge: https://www.curseforge.com/minecraft/mc-mods/friend-api/

Modrinth: https://modrinth.com/mod/friend-api/
## Usage
To use the API you can include it in your project like so:

build.gradle.kts:
```kotlin
repositories {
    mavenCentral()
}
dependencies {
    modImplementation(libs.friendApi)
}
```
libs.versions.toml:
```toml
[versions]
# Check this on https://central.sonatype.com/artifact/com.smushytaco/friend-api/
friendApi = "1.0.16"

[libraries]
friendApi = { group = "com.smushytaco", name = "friend-api", version.ref = "friendApi" }
```
