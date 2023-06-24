package com.smushytaco.friend_api.mojang_api_parser
import kotlinx.serialization.Serializable
import java.util.*
@Serializable
data class NameAndUUID(val name: String, @Serializable(with = UUIDSerializer::class) val id: UUID)