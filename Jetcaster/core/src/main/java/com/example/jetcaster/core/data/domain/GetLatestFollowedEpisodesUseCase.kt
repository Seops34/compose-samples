/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetcaster.core.data.domain

import com.example.jetcaster.core.data.database.model.EpisodeToPodcast
import com.example.jetcaster.core.data.di.Graph
import com.example.jetcaster.core.data.repository.EpisodeStore
import com.example.jetcaster.core.data.repository.PodcastStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * A use case which returns all the latest episodes from all the podcasts the user follows.
 */
class GetLatestFollowedEpisodesUseCase(
    private val episodeStore: EpisodeStore = Graph.episodeStore,
    private val podcastStore: PodcastStore = Graph.podcastStore,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<EpisodeToPodcast>> =
        podcastStore.followedPodcastsSortedByLastEpisode()
            .flatMapLatest { followedPodcasts ->
                if (followedPodcasts.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        followedPodcasts.map { p ->
                            episodeStore.episodesInPodcast(p.podcast.uri, 5)
                        }
                    ) { allEpisodes ->
                        allEpisodes.toList().flatten().sortedByDescending { it.episode.published }
                    }
                }
            }
}
