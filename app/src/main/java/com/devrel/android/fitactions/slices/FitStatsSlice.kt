/*
 * Copyright 2019 Google LLC
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
 *
 */

package com.devrel.android.fitactions.slices

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.slice.Slice
import androidx.slice.builders.*
import com.devrel.android.fitactions.R
import com.devrel.android.fitactions.model.FitActivity
import com.devrel.android.fitactions.model.FitRepository
import java.util.*

/**
 * Slice that loads the last user activities and stats, and displays them once loaded.
 *
 * This class shows how to deal with Slices that needs to load content asynchronously.
 */
class FitStatsSlice(
    context: Context,
    sliceUri: Uri,
    fitRepo: FitRepository
) : FitSlice(context, sliceUri) {

    /**
     * Get the last activities and refresh the slice.
     */
    private val activitiesLiveData = fitRepo.getLastActivities(5).apply {
        // The ContentProvider is called in a different thread and liveData
        // only works with MainThread
        Handler(Looper.getMainLooper()).post {
            observeForever {
                refresh()
            }
        }
    }

    override fun getSlice(): Slice {
        // If the data is still loading, return a loading slice, otherwise create the stats slice.
        return activitiesLiveData.value?.let { createStatsSlice(it) } ?: createLoadingSlice()
    }

    /**
     * Simple loading Slice while the DB is still loading the last activities.
     */
    private fun createLoadingSlice(): Slice = list(context, sliceUri, ListBuilder.INFINITY) {
        header {
            setTitle(context.getString(R.string.slice_stats_loading), true)
        }
    }

    /**
     * Create the stats slices showing the data provided by the DB.
     */
    private fun createStatsSlice(data: List<FitActivity>): Slice {
        // Create the list content
        return list(context, sliceUri, ListBuilder.INFINITY) {
            // Add the header of the slice
            header {
                title = context.getString(R.string.slice_stats_title)
                subtitle = context.getString(R.string.slice_stats_subtitle)
                // Defines the primary action when slice is clicked
                primaryAction = createActivityAction()
            }
            // Add a grid row to handle multiple cells
            gridRow {
                data.forEach { fitActivity ->
                    // For each activity add a cell with the fit data
                    cell {
                        setFitActivity(fitActivity)
                    }
                }
            }
        }
    }

    /**
     * Given a Slice cell, setup the content to display the given FitActivity.
     */
    private fun CellBuilderDsl.setFitActivity(value: FitActivity) {
        val distanceInKm = String.format("%.2f", value.distanceMeters / 1000)
        val distance = context.getString(R.string.slice_stats_distance, distanceInKm)
        addText(distance)

        val calendar = Calendar.getInstance().apply { timeInMillis = value.date }
        addTitleText(
            calendar.getDisplayName(
                Calendar.DAY_OF_WEEK,
                Calendar.LONG,
                Locale.getDefault()
            )
        )
    }
}
