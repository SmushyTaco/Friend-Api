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

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import java.util.concurrent.CompletableFuture

/**
 * Suggestion provider for the `/clientfriend add` command.
 *
 * Suggests online player usernames that are **not already in the friend list**
 * and that match the user's current input.
 */
object UsernameSuggestionProvider: SuggestionProvider<CommandSource> {
    /**
     * Generates username suggestions for the `add` subcommand.
     *
     * This filters the server's visible player list by:
     * 1. Removing any usernames already present in the local friend list.
     * 2. Matching the remaining usernames against the last token typed
     *    in the command (case-insensitive substring match).
     *
     * @param context the command context containing the input and source
     * @param builder the suggestions builder to populate
     * @return a future that completes with the constructed suggestions
     */
    override fun getSuggestions(context: CommandContext<CommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val playerList = context.source.playerNames
        val friendListUsernames = FriendApiClient.getCopyOfFriendsList().map { it.name.lowercase() }
        playerList.removeIf { it.lowercase() in friendListUsernames }
        for (playerName in playerList) if (playerName.contains(context.input.split(' ').last(), true)) builder.suggest(playerName)
        return builder.buildFuture()
    }
}