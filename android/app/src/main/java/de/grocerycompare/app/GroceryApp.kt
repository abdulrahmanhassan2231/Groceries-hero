package de.grocerycompare.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import de.grocerycompare.app.work.PriceRefreshWorker
import javax.inject.Inject

@HiltAndroidApp
class GroceryApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Schedule the weekly refresh + price-drop check (server does the scraping;
        // the phone just pulls the normalized cache and notifies on drops).
        PriceRefreshWorker.schedule(this)
    }
}
