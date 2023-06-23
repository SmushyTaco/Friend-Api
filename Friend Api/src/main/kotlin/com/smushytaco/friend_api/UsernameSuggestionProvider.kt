package com.smushytaco.friend_api
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import java.util.concurrent.CompletableFuture
object UsernameSuggestionProvider: SuggestionProvider<CommandSource> {
    override fun getSuggestions(context: CommandContext<CommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val playerList = context.source.playerNames
        val friendListUsernames = FriendApiClient.getCopyOfFriendsList().map { it.name.lowercase() }
        playerList.removeIf { it.lowercase() in friendListUsernames }
        for (playerName in playerList) if (playerName.contains(context.input.split(" ").last(), true)) builder.suggest(playerName)
        return builder.buildFuture()
    }
}