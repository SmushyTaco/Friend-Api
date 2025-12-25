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

package com.smushytaco.friend_api

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.smushytaco.friend_api.mojang_api_parser.MojangApiParser
import com.smushytaco.friend_api.mojang_api_parser.NameAndUUID
import com.smushytaco.friend_api.mojang_api_parser.UUIDSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.lwjgl.glfw.GLFW
import java.nio.file.Files
import java.util.*
import kotlin.concurrent.thread

/**
 * Client-side entry point for the Friend API mod.
 *
 * Keeps track of the local friend list, persists it to disk, and wires up
 * commands, keybinds, and Mojang API calls used to manage friends.
 */
object FriendApiClient : ClientModInitializer {
    private fun MutableComponent.copySupport(copyString: String, hoverText: Component): MutableComponent {
        style = style.withClickEvent(ClickEvent.CopyToClipboard(copyString)).withHoverEvent(HoverEvent.ShowText(hoverText))
        return this
    }
    private fun MutableComponent.commandSupport(commandString: String, hoverText: Component): MutableComponent {
        style = style.withClickEvent(ClickEvent.RunCommand(commandString)).withHoverEvent(HoverEvent.ShowText(hoverText))
        return this
    }
    private val filePath = FabricLoader.getInstance().configDir.resolve("friend_api.json")
    private val friends = arrayListOf<NameAndUUID>()
    private const val MOD_ID = "friend_api"
    private val KEYBINDING = KeyMapping("key.$MOD_ID.$MOD_ID", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, KeyMapping.Category.register(
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "category")))
    /**
     * Returns an immutable snapshot of the current friend list.
     *
     * The returned array is a copy; modifying it will not affect the
     * internally stored list.
     *
     * @return an array containing all known friends
     */
    fun getCopyOfFriendsList() = friends.toTypedArray()
    /**
     * Looks up a friend by their Minecraft username.
     *
     * The comparison is case-insensitive.
     *
     * @param username the username to search for
     * @return the matching friend entry, or `null` if no friend with that name exists
     */
    @Suppress("UNUSED")
    fun getFriend(username: String) = friends.find { it.name.equals(username, true) }
    /**
     * Looks up a friend by their UUID.
     *
     * @param uuid the UUID to search for
     * @return the matching friend entry, or `null` if no friend with that UUID exists
     */
    @Suppress("UNUSED")
    fun getFriend(uuid: UUID) = friends.find { it.id == uuid }
    /**
     * Looks up a friend by a full friend entry.
     *
     * This uses structural equality to find an exact match.
     *
     * @param nameAndUUID the friend entry to search for
     * @return the matching friend entry, or `null` if it is not present
     */
    @Suppress("UNUSED")
    fun getFriend(nameAndUUID: NameAndUUID) = friends.find { it == nameAndUUID }
    /**
     * Checks whether a friend with the given username exists.
     *
     * The comparison is case-insensitive.
     *
     * @param username the username to check
     * @return `true` if the friend list contains a friend with this name, otherwise `false`
     */
    @Suppress("UNUSED")
    fun containsFriend(username: String) = friends.any { it.name.equals(username, true) }
    /**
     * Checks whether a friend with the given UUID exists.
     *
     * @param uuid the UUID to check
     * @return `true` if the friend list contains a friend with this UUID, otherwise `false`
     */
    @Suppress("UNUSED")
    fun containsFriend(uuid: UUID) = friends.any { it.id == uuid }
    /**
     * Checks whether the given friend entry is present in the friend list.
     *
     * @param nameAndUUID the friend entry to check
     * @return `true` if this entry is contained in the friend list, otherwise `false`
     */
    @Suppress("UNUSED")
    fun containsFriend(nameAndUUID: NameAndUUID) = friends.any { it == nameAndUUID }
    private fun addFriend(username: String): Boolean? {
        if (friends.any { it.name.equals(username, true) }) return false
        val nameAndUUID = MojangApiParser.getUuid(username) ?: return null
        friends.add(nameAndUUID)
        writeFriendsToFile()
        return true
    }
    private fun addFriend(uuid: UUID): Boolean? {
        if (friends.any { it.id == uuid }) return false
        val nameAndUUID = MojangApiParser.getUsername(uuid) ?: return null
        friends.add(nameAndUUID)
        writeFriendsToFile()
        return true
    }
    private fun removeFriend(username: String): Boolean {
        if (friends.removeIf { it.name.equals(username, true) }) {
            writeFriendsToFile()
            return true
        }
        return false
    }
    private fun removeFriend(uuid: UUID): Boolean {
        if (friends.removeIf { it.id == uuid }) {
            writeFriendsToFile()
            return true
        }
        return false
    }
    private fun clearFriendList(): Int {
        if (friends.isEmpty()) return 0
        val friendsCount = friends.size
        friends.clear()
        writeFriendsToFile()
        return friendsCount
    }
    private fun writeFriendsToFile() {
        val jsonString = Json.encodeToString(ListSerializer(NameAndUUID.serializer()), friends)
        Files.write(filePath, jsonString.toByteArray())
    }
    private fun readFriendsFromFile() {
        val jsonString = String(Files.readAllBytes(filePath))
        friends.clear()
        try {
            friends.addAll(Json.decodeFromString(ListSerializer(NameAndUUID.serializer()), jsonString))
        } catch (_: Exception) {}
    }
    private fun updateFriendsList() {
        for (index in friends.indices.reversed()) {
            val nameAndUUID = MojangApiParser.getUsername(friends[index].id)
            if (nameAndUUID == null) {
                if (MojangApiParser.isUuidToProfileApiRunning()) friends.removeAt(index)
                continue
            }
            if (friends[index].name != nameAndUUID.name) friends[index] = nameAndUUID
        }
        val noDuplicates = friends.distinct()
        friends.clear()
        friends.addAll(noDuplicates)
        writeFriendsToFile()
    }
    private fun addFriendWithPlayerOutput(username: String, clientPlayerEntity: LocalPlayer) {
        val minecraftClient = Minecraft.getInstance()
        thread {
            var uuid: UUID? = null
            try {
                uuid = UUIDSerializer.stringToUUID(username)
            } catch (_: Exception) {}
            val successStatus = uuid?.let { id -> addFriend(id) } ?: addFriend(username)
            if (successStatus == null) {
                minecraftClient.execute {
                    clientPlayerEntity.displayClientMessage(Component.literal("§c${uuid ?: username} §4does not exist!"), true)
                }
                return@thread
            } else if (!successStatus) {
                minecraftClient.execute {
                    clientPlayerEntity.displayClientMessage(Component.literal("§c${if (uuid != null) friends.find { predicate -> predicate.id == uuid }?.id ?: uuid else friends.find { predicate -> predicate.name.equals(username, true) }?.name ?: username} §4is already on your friend list!"), true)
                }
                return@thread
            }
            minecraftClient.execute {
                clientPlayerEntity.displayClientMessage(Component.literal("§b${if (uuid != null) friends.find { predicate -> predicate.id == uuid }?.id ?: uuid else friends.find { predicate -> predicate.name.equals(username, true) }?.name ?: username} §3has been successfully added to your friend list!"), false)
            }
        }
    }
    /**
     * Called by Fabric when the client mod is initialized.
     *
     * Registers the keybinding and client commands, loads or creates the
     * friend list file, updates it using Mojang's profile API, and hooks
     * the client tick event used for the keybind-based friend adding.
     */
    override fun onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(KEYBINDING)
        if (Files.notExists(filePath)) Files.createFile(filePath)
        readFriendsFromFile()
        updateFriendsList()
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            val friend = dispatcher.register(literal("clientfriend")
                .then(literal("list").executes {
                    if (friends.isEmpty()) {
                        it.source.player.displayClientMessage(Component.literal("§4You currently have no friends on your friend list."), true)
                        return@executes Command.SINGLE_SUCCESS
                    }
                    it.source.player.displayClientMessage(Component.literal("§3Showing friend list:"), false)
                    for (index in friends.indices) it.source.player.displayClientMessage(
                        Component.literal("§3${index + 1}. ").append(
                            Component.literal("§3${friends[index].name}").copySupport(friends[index].name, Component.literal("§3Click to copy the username §b${friends[index].name}§3!"))).append(
                            Component.literal(" ")).append(Component.literal("§b[UUID]").copySupport(friends[index].id.toString(), Component.literal("§3Click to copy the §bUUID§3!"))).append(
                            Component.literal(" ")).append(Component.literal("§b[Remove]").commandSupport("/clientfriend remove ${friends[index].name}", Component.literal("§3Click to remove the friend §b${friends[index].name}§3!"))), false)
                    return@executes Command.SINGLE_SUCCESS
                })
                .then(literal("add")
                    .then(ClientCommandManager.argument("username", StringArgumentType.word())
                        .suggests { context, builder ->
                            @Suppress("UNCHECKED_CAST")
                            UsernameSuggestionProvider.getSuggestions(context as CommandContext<SharedSuggestionProvider>, builder)
                        }.executes {
                            addFriendWithPlayerOutput(StringArgumentType.getString(it, "username"), it.source.player)
                            return@executes Command.SINGLE_SUCCESS
                        }))
                .then(literal("remove")
                    .then(ClientCommandManager.argument("username", StringArgumentType.word())
                        .suggests { context, builder ->
                            @Suppress("UNCHECKED_CAST")
                            FriendSuggestionProvider.getSuggestions(context as CommandContext<SharedSuggestionProvider>, builder)
                        }.executes {
                            val username = StringArgumentType.getString(it, "username")
                            var uuid: UUID? = null
                            try {
                                uuid = UUIDSerializer.stringToUUID(username)
                            } catch (_: Exception) {}
                            val successStatus = uuid?.let { id -> removeFriend(id) } ?: removeFriend(username)
                            if (!successStatus) {
                                it.source.player.displayClientMessage(Component.literal("§c${uuid ?: username} §4isn't on your friend list!"), true)
                                return@executes Command.SINGLE_SUCCESS
                            }
                            it.source.player.displayClientMessage(Component.literal("§b${if (uuid != null) friends.find { predicate -> predicate.id == uuid }?.id ?: uuid else friends.find { predicate -> predicate.name.equals(username, true) }?.name ?: username} §3has been successfully removed from your friend list!"), false)
                            return@executes Command.SINGLE_SUCCESS
                        }))
                .then(literal("clear").executes {
                    val clearCount = clearFriendList()
                    if (clearCount == 0) {
                        it.source.player.displayClientMessage(Component.literal("§4You currently have no friends on your friend list to clear."), true)
                        return@executes Command.SINGLE_SUCCESS
                    }
                    it.source.player.displayClientMessage(Component.literal("${if (clearCount != 1) "§3All " else ""}§b$clearCount§3 friend${if (clearCount != 1) "s have" else " has"} been cleared from the friend list!"), false)
                    return@executes Command.SINGLE_SUCCESS
                })
                .then(literal("update").executes {
                    val minecraftClient = Minecraft.getInstance()
                    thread {
                        updateFriendsList()
                        minecraftClient.execute {
                            it.source.player.displayClientMessage(Component.literal("§3Your friend list has been checked and updated accordingly!"), false)
                        }
                    }
                    return@executes Command.SINGLE_SUCCESS
                }))
            dispatcher.register(literal("cf").redirect(friend))
        })
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvents.StartTick {
            while (KEYBINDING.consumeClick()) {
                val player = it.player ?: return@StartTick
                val target = target(it) ?: return@StartTick
                if (target.type != HitResult.Type.ENTITY || target !is EntityHitResult || target.entity !is Player) return@StartTick
                val playerToFriend = target.entity as Player
                addFriendWithPlayerOutput(playerToFriend.name.string, player)
            }
        })
    }
    private fun target(client: Minecraft, range: Double = 250.0, tickDelta: Float = 1.0F): HitResult? {
        val clientCameraEntity = client.cameraEntity ?: return null
        var hitResult = clientCameraEntity.pick(range, tickDelta, false)
        val rotationVector = clientCameraEntity.getViewVector(1.0F)
        val positionVector = clientCameraEntity.getEyePosition(tickDelta)
        val box = clientCameraEntity.boundingBox.expandTowards(rotationVector.scale(range)).inflate(1.0, 1.0, 1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(clientCameraEntity, positionVector, positionVector.add(rotationVector.x * range, rotationVector.y * range, rotationVector.z * range), box, { !it.isSpectator && it.isPickable }, range * range)
        if (entityHitResult != null) {
            val distanceFromEntity = positionVector.distanceToSqr(entityHitResult.location)
            if (distanceFromEntity < range * range && (hitResult.type == HitResult.Type.MISS || hitResult.type == HitResult.Type.BLOCK && distanceFromEntity < positionVector.distanceToSqr(hitResult.location))) hitResult = entityHitResult
        }
        return hitResult
    }
}