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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

object UUIDSerializer : KSerializer<UUID> {
    fun stringToUUID(uuidString: String): UUID {
        val stringBuilder = StringBuilder()
        val string = uuidString.replace("-", "")
        for (i in string.indices) {
            stringBuilder.append(string[i])
            if (i == 7 || i == 11 || i == 15 || i == 19) stringBuilder.append('-')
        }
        return UUID.fromString(stringBuilder.toString())
    }
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder) = stringToUUID(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: UUID) { encoder.encodeString(value.toString().replace("-", "")) }
}