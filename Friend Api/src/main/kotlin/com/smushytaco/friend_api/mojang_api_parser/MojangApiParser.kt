/*
 * Copyright 2025 Nikan Radan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smushytaco.friend_api.mojang_api_parser

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

/**
 * Utility object for interacting with Mojang's public profile API.
 *
 * Provides helper functions to:
 * - Resolve a Minecraft username → UUID
 * - Resolve a UUID → username/profile
 * - Check the availability of Mojang API endpoints
 *
 * All API calls are performed synchronously using OkHttp and return `null` or
 * `false` on failure instead of throwing.
 */
object MojangApiParser {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private fun executeRequestForGets(request: Request) = runCatching { client.newCall(request).execute().use { response -> json.decodeFromString(NameAndUUID.serializer(), response.body.string()) } }.getOrNull()
    private fun executeRequestForChecks(request: Request) = runCatching { client.newCall(request).execute().use { response -> response.isSuccessful } }.getOrElse { false }
    /**
     * Fetches the Mojang profile associated with the given Minecraft username.
     *
     * @param name the username to query
     * @return a [NameAndUUID] entry if the profile exists, or `null` if the
     *         user does not exist or the API request fails
     */
    fun getUuid(name: String) = executeRequestForGets(Request(url = "https://api.mojang.com/users/profiles/minecraft/$name/".toHttpUrl()))
    /**
     * Fetches the Mojang profile associated with the given UUID.
     *
     * This internally strips hyphens before performing the request, as
     * Mojang's profile endpoint expects a compact UUID format.
     *
     * @param uuid the UUID to query
     * @return a [NameAndUUID] entry if the profile exists, or `null` if the
     *         UUID is invalid or the API request fails
     */
    fun getUsername(uuid: UUID) = executeRequestForGets(Request(url = "https://sessionserver.mojang.com/session/minecraft/profile/${uuid.toString().replace("-", "")}/".toHttpUrl()))
    /**
     * Verifies whether Mojang's username → UUID API endpoint is reachable.
     *
     * Performs a simple request using a known username and returns `true`
     * if the server responds successfully.
     *
     * @return `true` if the endpoint is operational, otherwise `false`
     */
    @Suppress("UNUSED")
    fun isUsernameToUuidApiRunning() = executeRequestForChecks(Request(url = "https://api.mojang.com/users/profiles/minecraft/SmushyTaco/".toHttpUrl()))
    /**
     * Verifies whether Mojang's UUID → profile API endpoint is reachable.
     *
     * Performs a request using a known test UUID. The returned boolean
     * simply indicates whether the endpoint responded successfully.
     *
     * @return `true` if the endpoint is operational, otherwise `false`
     */
    fun isUuidToProfileApiRunning() = executeRequestForChecks(Request(url = "https://sessionserver.mojang.com/session/minecraft/profile/c6d2219bc8a54ccda8165328b2b32653/".toHttpUrl()))
}