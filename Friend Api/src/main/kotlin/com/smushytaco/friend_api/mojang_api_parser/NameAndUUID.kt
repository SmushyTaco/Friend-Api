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

import kotlinx.serialization.Serializable
import java.util.*

/**
 * Represents a Mojang profile consisting of a Minecraft username and its UUID.
 *
 * This class is used as the decoded result of Mojang API calls and is also
 * serialized to disk as part of the friend list.
 *
 * @property name the current Minecraft username associated with the UUID
 * @property id the UUID of the player, serialized using [UUIDSerializer]
 */
@Serializable
data class NameAndUUID(val name: String, @Serializable(with = UUIDSerializer::class) val id: UUID)