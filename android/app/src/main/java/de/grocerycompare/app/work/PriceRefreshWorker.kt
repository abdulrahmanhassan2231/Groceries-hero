package de.grocerycompare.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.grocerycompare.app.MainActivity
import de.grocerycompare.app.R
import de.grocerycompare.app.data.repo.GroceryRepository
import java.util.concurrent.TimeUnit

/**
 * Weekly refresh of watched items + price-drop notifications.
 *
 * The heavy lifting (scraping/parsing) is done by the backend on a weekly schedule; this
 * worker only pulls the normalized cache for the user's watched items and notifies when a
 * watched item's best unit price drops. Runs about daily so a mid-week correction is
 * caught, and near Monday when the new offer week lands.
 */
@HiltWorker
class PriceRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: GroceryRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val drops = runCatching { repo.checkPriceDrops() }.getOrElse { return Result.retry() }
        drops.forEach { notifyDrop(it.query, it.productName, it.price, it.chain) }
        return Result.success()
    }

    private fun notifyDrop(query: String, productName: String, price: Double, chain: String) {
        ensureChannel()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val ctx = applicationContext
        val intent = android.content.Intent(ctx, MainActivity::class.java)
        val pi = android.app.PendingIntent.getActivity(
            ctx, query.hashCode(), intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(ctx.getString(R.string.price_drop_title, query))
            .setContentText(ctx.getString(R.string.price_drop_body, productName, "%.2f €".format(price), chain))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(query.hashCode(), notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Price drops", NotificationManager.IMPORTANCE_DEFAULT,
        )
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "price_drops"
        private const val WORK_NAME = "price_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PriceRefreshWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }
    }
}
