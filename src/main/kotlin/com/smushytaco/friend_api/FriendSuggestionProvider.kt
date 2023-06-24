package com.smushytaco.friend_api
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import java.util.concurrent.CompletableFuture
object FriendSuggestionProvider: SuggestionProvider<CommandSource> {
    override fun getSuggestions(context: CommandContext<CommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        for (friendEntry in FriendApiClient.getCopyOfFriendsList()) if (friendEntry.name.contains(context.input.split(" ").last(), true)) builder.suggest(friendEntry.name)
        return builder.buildFuture()
    }
}