# Friend Api
CurseForge: https://www.curseforge.com/minecraft/mc-mods/friend-api/

Modrinth: https://modrinth.com/mod/friend-api/
## Usage
To use the API you can include it in your project like so:

build.gradle.kts
```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    modImplementation("com.github.SmushyTaco", "Friend-Api", project.extra["friend_api_version"] as String)
}
```
gradle.properties
```properties
# Check this on https://github.com/SmushyTaco/Friend-Api/releases/latest/
friend_api_version = 1.0.8
```
