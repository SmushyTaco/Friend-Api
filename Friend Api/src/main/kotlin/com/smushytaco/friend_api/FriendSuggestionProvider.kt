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

object FriendSuggestionProvider: SuggestionProvider<CommandSource> {
    override fun getSuggestions(context: CommandContext<CommandSource>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        for (friendEntry in FriendApiClient.getCopyOfFriendsList()) if (friendEntry.name.contains(context.input.split(' ').last(), true)) builder.suggest(friendEntry.name)
        return builder.buildFuture()
    }
}