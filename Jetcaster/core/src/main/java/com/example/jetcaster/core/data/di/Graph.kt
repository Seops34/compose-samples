/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.jetcaster.core.data.di

import android.content.Context
import androidx.room.Room
import com.example.jetcaster.core.BuildConfig
import com.example.jetcaster.core.data.database.JetcasterDatabase
import com.example.jetcaster.core.data.database.dao.TransactionRunner
import com.example.jetcaster.core.data.domain.FilterableCategoriesUseCase
import com.example.jetcaster.core.data.domain.GetLatestFollowedEpisodesUseCase
import com.example.jetcaster.core.data.domain.PodcastCategoryFilterUseCase
import com.example.jetcaster.core.data.network.PodcastsFetcher
import com.example.jetcaster.core.data.repository.CategoryStore
import com.example.jetcaster.core.data.repository.EpisodeStore
import com.example.jetcaster.core.data.repository.LocalCategoryStore
import com.example.jetcaster.core.data.repository.LocalEpisodeStore
import com.example.jetcaster.core.data.repository.LocalPodcastStore
import com.example.jetcaster.core.data.repository.PodcastsRepository
import com.rometools.rome.io.SyndFeedInput
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.LoggingEventListener

/**
 * A very simple global singleton dependency graph.
 *
 * For a real app, you would use something like Hilt/Dagger instead.
 */
object Graph {
    lateinit var okHttpClient: OkHttpClient

    lateinit var database: JetcasterDatabase
        private set

    private val transactionRunner: TransactionRunner
        get() = database.transactionRunnerDao()

    private val syndFeedInput by lazy { SyndFeedInput() }

    val podcastRepository by lazy {
        PodcastsRepository(
            podcastsFetcher = podcastFetcher,
            podcastStore = podcastStore,
            episodeStore = episodeStore,
            categoryStore = categoryStore,
            transactionRunner = transactionRunner,
            mainDispatcher = mainDispatcher
        )
    }

    private val podcastFetcher by lazy {
        PodcastsFetcher(
            okHttpClient = okHttpClient,
            syndFeedInput = syndFeedInput,
            ioDispatcher = ioDispatcher
        )
    }

    val podcastStore by lazy {
        LocalPodcastStore(
            podcastDao = database.podcastsDao(),
            podcastFollowedEntryDao = database.podcastFollowedEntryDao(),
            transactionRunner = transactionRunner
        )
    }

    val episodeStore: EpisodeStore by lazy {
        LocalEpisodeStore(
            episodesDao = database.episodesDao()
        )
    }

    val getLatestFollowedEpisodesUseCase by lazy {
        GetLatestFollowedEpisodesUseCase(
            episodeStore = episodeStore,
            podcastStore = podcastStore
        )
    }

    val podcastCategoryFilterUseCase by lazy {
        PodcastCategoryFilterUseCase(
            categoryStore = categoryStore
        )
    }

    val filterableCategoriesUseCase by lazy {
        FilterableCategoriesUseCase(
            categoryStore = categoryStore
        )
    }

    val categoryStore: CategoryStore by lazy {
        LocalCategoryStore(
            categoriesDao = database.categoriesDao(),
            categoryEntryDao = database.podcastCategoryEntryDao(),
            episodesDao = database.episodesDao(),
            podcastsDao = database.podcastsDao()
        )
    }

    private val mainDispatcher: CoroutineDispatcher
        get() = Dispatchers.Main

    private val ioDispatcher: CoroutineDispatcher
        get() = Dispatchers.IO

    fun provide(context: Context) {
        okHttpClient = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), (20 * 1024 * 1024).toLong()))
            .apply {
                if (BuildConfig.DEBUG) eventListenerFactory(LoggingEventListener.Factory())
            }
            .build()

        database = Room.databaseBuilder(context, JetcasterDatabase::class.java, "data.db")
            // This is not recommended for normal apps, but the goal of this sample isn't to
            // showcase all of Room.
            .fallbackToDestructiveMigration()
            .build()
    }
}
