package com.smushytaco.friend_api
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.smushytaco.friend_api.mojang_api_parser.MojangApiParser
import com.smushytaco.friend_api.mojang_api_parser.NameAndUUID
import com.smushytaco.friend_api.mojang_api_parser.UUIDSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.command.CommandSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import org.lwjgl.glfw.GLFW
import java.nio.file.Files
import java.util.UUID
import kotlin.concurrent.thread
@Environment(EnvType.CLIENT)
object FriendApiClient : ClientModInitializer {
    private fun MutableText.copySupport(copyString: String, hoverText: Text): MutableText {
        style = style.withClickEvent(ClickEvent.CopyToClipboard(copyString)).withHoverEvent(HoverEvent.ShowText(hoverText))
        return this
    }
    private fun MutableText.commandSupport(commandString: String, hoverText: Text): MutableText {
        style = style.withClickEvent(ClickEvent.RunCommand(commandString)).withHoverEvent(HoverEvent.ShowText(hoverText))
        return this
    }
    private val filePath = FabricLoader.getInstance().configDir.resolve("friend_api.json")
    private val friends = arrayListOf<NameAndUUID>()
    private const val MOD_ID = "friend_api"
    private val KEYBINDING = KeyBinding("key.$MOD_ID.$MOD_ID", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, KeyBinding.Category.create(Identifier.of(MOD_ID, "category")))
    fun getCopyOfFriendsList() = friends.toTypedArray()
    @Suppress("UNUSED")
    fun getFriend(username: String) = friends.find { it.name.equals(username, true) }
    @Suppress("UNUSED")
    fun getFriend(uuid: UUID) = friends.find { it.id == uuid }
    @Suppress("UNUSED")
    fun getFriend(nameAndUUID: NameAndUUID) = friends.find { it == nameAndUUID }
    @Suppress("UNUSED")
    fun containsFriend(username: String) = friends.any { it.name.equals(username, true) }
    @Suppress("UNUSED")
    fun containsFriend(uuid: UUID) = friends.any { it.id == uuid }
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
    private fun addFriendWithPlayerOutput(username: String, clientPlayerEntity: ClientPlayerEntity) {
        thread {
            var uuid: UUID? = null
            try {
                uuid = UUIDSerializer.stringToUUID(username)
            } catch (_: Exception) {}
            val successStatus = uuid?.let { id -> addFriend(id) } ?: addFriend(username)
            if (successStatus == null) {
                clientPlayerEntity.sendMessage(Text.literal("§c${uuid ?: username} §4does not exist!"), true)
                return@thread
            } else if (!successStatus) {
                clientPlayerEntity.sendMessage(Text.literal("§c${if (uuid != null) friends.find { predicate -> predicate.id == uuid }?.id ?: uuid else friends.find { predicate -> predicate.name.equals(username, true) }?.name ?: username} §4is already on your friend list!"), true)
                return@thread
            }
            clientPlayerEntity.sendMessage(Text.literal("§b${if (uuid != null) friends.find { predicate -> predicate.id == uuid }?.id ?: uuid else friends.find { predicate -> predicate.name.equals(username, true) }?.name ?: username} §3has been successfully added to your friend list!"), false)
        }
    }
    override fun onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(KEYBINDING)
        if (!Files.exists(filePath)) Files.createFile(filePath)
        readFriendsFromFile()
        updateFriendsList()
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            val friend = dispatcher.register(literal("clientfriend")
                .then(literal("list").executes {
                    if (friends.isEmpty()) {
                        it.source.player.sendMessage(Text.literal("§4You currently have no friends on your friend list."), true)
                        return@executes Command.SINGLE_SUCCESS
                    }
                    it.source.player.sendMessage(Text.literal("§3Showing friend list:"), false)
                    for (index in friends.indices) it.source.player.sendMessage(Text.literal("§3${index + 1}. ").append(Text.literal("§3${friends[index].name}").copySupport(friends[index].name, Text.literal("§3Click to copy the username §b${friends[index].name}§3!"))).append(Text.literal(" ")).append(Text.literal("§b[UUID]").copySupport(friends[index].id.toString(), Text.literal("§3Click to copy the §bUUID§3!"))).append(Text.literal(" ")).append(Text.literal("§b[Remove]").commandSupport("/clientfriend remove ${friends[index].name}", Text.literal("§3Click to remove the friend §b${friends[index].name}§3!"))), false)
                    return@executes Command.SINGLE_SUCCESS
                })
                .then(literal("add")
                    .then(ClientCommandManager.argument("username", StringArgumentType.word())
                        .suggests { context, builder ->
                            @Suppress("UNCHECKED_CAST")
                            UsernameSuggestionProvider.getSuggestions(context as CommandContext<CommandSource>, builder)
                        }.executes {
                            addFriendWithPlayerOutput(StringArgumentType.getString(it, "username"), it.source.player)
                            return@executes Command.SINGLE_SUCCESS
                        }))
                .then(literal("remove")
                    .then(ClientCommandManager.argument("username", StringArgumentType.word())
                        .suggests { context, builder ->
                            @Suppress("UNCHECKED_CAST")
                            FriendSuggestionProvider.getSuggestions(context as CommandContext<CommandSource>, builder)
                        }.executes {
                            val username = StringArgumentType.getString(it, "username")
                            var uuid: UUID? = null
                            try {
                                uuid = UUIDSerializer.stringToUUID(username)
                            } catch (_: Exception) {}
                            val successStatus = uuid?.let { id -> removeFriend(id) } ?: removeFriend(username)
                            if (!successStatus) {
                                it.source.player.sendMessage(Text.literal("§c${uuid ?: username} §4isn't on your friend list!"), true)
                                return@executes Command.SINGLE_SUCCESS
                            }
                            it.source.player.sendMessage(Text.literal("§b${if (uuid != null) friends.find { predicate -> predicate.id == uuid }?.id ?: uuid else friends.find { predicate -> predicate.name.equals(username, true) }?.name ?: username} §3has been successfully removed from your friend list!"), false)
                            return@executes Command.SINGLE_SUCCESS
                        }))
                .then(literal("clear").executes {
                    val clearCount = clearFriendList()
                    if (clearCount == 0) {
                        it.source.player.sendMessage(Text.literal("§4You currently have no friends on your friend list to clear."), true)
                        return@executes Command.SINGLE_SUCCESS
                    }
                    it.source.player.sendMessage(Text.literal("${if (clearCount != 1) "§3All " else ""}§b$clearCount§3 friend${if (clearCount != 1) "s have" else " has"} been cleared from the friend list!"), false)
                    return@executes Command.SINGLE_SUCCESS
                })
                .then(literal("update").executes {
                    thread {
                        updateFriendsList()
                        it.source.player.sendMessage(Text.literal("§3Your friend list has been checked and updated accordingly!"), false)
                    }
                    return@executes Command.SINGLE_SUCCESS
                }))
            dispatcher.register(literal("cf").redirect(friend))
        })
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvents.StartTick {
            while (KEYBINDING.wasPressed()) {
                val player = it.player ?: return@StartTick
                val target = target(it) ?: return@StartTick
                if (target.type != HitResult.Type.ENTITY || target !is EntityHitResult || target.entity !is PlayerEntity) return@StartTick
                val playerToFriend = target.entity as PlayerEntity
                addFriendWithPlayerOutput(playerToFriend.name.string, player)
            }
        })
    }
    private fun target(client: MinecraftClient, range: Double = 250.0, tickDelta: Float = 1.0F): HitResult? {
        val clientCameraEntity = client.cameraEntity ?: return null
        var hitResult = clientCameraEntity.raycast(range, tickDelta, false)
        val rotationVector = clientCameraEntity.getRotationVec(1.0F)
        val positionVector = clientCameraEntity.getCameraPosVec(tickDelta)
        val box = clientCameraEntity.boundingBox.stretch(rotationVector.multiply(range)).expand(1.0, 1.0, 1.0)
        val entityHitResult = ProjectileUtil.raycast(clientCameraEntity, positionVector, positionVector.add(rotationVector.x * range, rotationVector.y * range, rotationVector.z * range), box, { !it.isSpectator && it.canHit() }, range * range)
        if (entityHitResult != null) {
            val distanceFromEntity = positionVector.squaredDistanceTo(entityHitResult.pos)
            if (distanceFromEntity < range * range && (hitResult.type == HitResult.Type.MISS || hitResult.type == HitResult.Type.BLOCK && distanceFromEntity < positionVector.squaredDistanceTo(hitResult.pos))) hitResult = entityHitResult
        }
        return hitResult
    }
}