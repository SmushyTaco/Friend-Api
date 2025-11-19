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

object MojangApiParser {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private fun executeRequestForGets(request: Request) = runCatching { client.newCall(request).execute().use { response -> json.decodeFromString(NameAndUUID.serializer(), response.body.string()) } }.getOrNull()
    private fun executeRequestForChecks(request: Request) = runCatching { client.newCall(request).execute().use { response -> response.isSuccessful } }.getOrElse { false }
    fun getUuid(name: String) = executeRequestForGets(Request(url = "https://api.mojang.com/users/profiles/minecraft/$name/".toHttpUrl()))
    fun getUsername(uuid: UUID) = executeRequestForGets(Request(url = "https://sessionserver.mojang.com/session/minecraft/profile/${uuid.toString().replace("-", "")}/".toHttpUrl()))
    @Suppress("UNUSED")
    fun isUsernameToUuidApiRunning() = executeRequestForChecks(Request(url = "https://api.mojang.com/users/profiles/minecraft/SmushyTaco/".toHttpUrl()))
    fun isUuidToProfileApiRunning() = executeRequestForChecks(Request(url = "https://sessionserver.mojang.com/session/minecraft/profile/c6d2219bc8a54ccda8165328b2b32653/".toHttpUrl()))
}