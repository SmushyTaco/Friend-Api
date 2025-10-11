# Friend Api
CurseForge: https://www.curseforge.com/minecraft/mc-mods/friend-api/

Modrinth: https://modrinth.com/mod/friend-api/
## Usage
To use the API you can include it in your project like so:

build.gradle.kts
```kotlin
val friendApiVersion = providers.gradleProperty("friend_api_version")
repositories {
    maven("https://jitpack.io")
}
dependencies {
    modImplementation("com.github.SmushyTaco:Friend-Api:${friendApiVersion.get()}")
}
```
gradle.properties
```properties
# Check this on https://github.com/SmushyTaco/Friend-Api/releases/latest/
friend_api_version = 1.0.13
```
