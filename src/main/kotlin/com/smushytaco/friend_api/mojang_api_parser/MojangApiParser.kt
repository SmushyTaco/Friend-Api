package com.smushytaco.friend_api.mojang_api_parser
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
object MojangApiParser {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private fun executeRequestForGets(request: Request): NameAndUUID? {
        return try {
            client.newCall(request).execute().use { response -> response.body?.string()?.let { json.decodeFromString(NameAndUUID.serializer(), it) } }
        } catch (e: Exception) { null }
    }
    private fun executeRequestForChecks(request: Request): Boolean {
        return try {
            client.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) { false }
    }
    fun getUuid(name: String): NameAndUUID? {
        val request = Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/$name/").build()
        return executeRequestForGets(request)
    }
    fun getUsername(uuid: UUID): NameAndUUID? {
        val request = Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/${uuid.toString().replace("-", "")}/").build()
        return executeRequestForGets(request)
    }
    @Suppress("UNUSED")
    fun isUsernameToUuidApiRunning(): Boolean {
        val request = Request.Builder().url("https://api.mojang.com/users/profiles/minecraft/SmushyTaco/").build()
        return executeRequestForChecks(request)
    }
    fun isUuidToProfileApiRunning(): Boolean {
        val request = Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/c6d2219bc8a54ccda8165328b2b32653/").build()
        return executeRequestForChecks(request)
    }
}