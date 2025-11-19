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

/**
 * A custom Kotlinx Serialization serializer for Mojang-style UUID strings.
 *
 * Mojang's APIs use a compact UUID format (32 hexadecimal characters without
 * hyphens). This serializer ensures UUIDs are encoded in that format and
 * decoded back into standard Java UUID objects.
 *
 * Also provides a helper method to convert compact or hyphenated UUID strings
 * into valid [UUID] instances.
 */
object UUIDSerializer : KSerializer<UUID> {
    /**
     * Converts a compact (no-hyphen) or hyphenated UUID string into a
     * standard Java [UUID] by inserting hyphens at the correct positions.
     *
     * Mojang API responses may return UUIDs without hyphens; this helper
     * produces a valid, parseable UUID.
     *
     * @param uuidString the raw UUID string to convert
     * @return the resulting [UUID]
     */
    fun stringToUUID(uuidString: String): UUID {
        val stringBuilder = StringBuilder()
        val string = uuidString.replace("-", "")
        for (i in string.indices) {
            stringBuilder.append(string[i])
            if (i == 7 || i == 11 || i == 15 || i == 19) stringBuilder.append('-')
        }
        return UUID.fromString(stringBuilder.toString())
    }
    /**
     * Describes this serializer as a primitive string type, since UUIDs are
     * encoded and decoded using their string form.
     */
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    /**
     * Decodes a compact or hyphenated UUID string into a [UUID].
     *
     * @param decoder the decoder providing the raw UUID string
     * @return a parsed and normalized [UUID] instance
     */
    override fun deserialize(decoder: Decoder) = stringToUUID(decoder.decodeString())
    /**
     * Encodes a [UUID] into Mojang's compact (no-hyphen) 32-character format.
     *
     * @param encoder the encoder used to output the serialized UUID string
     * @param value the UUID to serialize
     */
    override fun serialize(encoder: Encoder, value: UUID) { encoder.encodeString(value.toString().replace("-", "")) }
}