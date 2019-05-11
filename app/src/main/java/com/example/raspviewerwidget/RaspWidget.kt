package com.example.raspviewerwidget

import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import okhttp3.Request
import android.net.ConnectivityManager
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.text.HtmlCompat
import com.example.raspviewerwidget.jobs.JobGetText


/**
 * Implementation of App Widget functionality.
 */
class RaspWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }


    override fun onEnabled(context: Context) {
        Log.i("ggp", "onEnabled")
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
//        scheduler.schedule(JobInfo.Builder(1, ComponentName(context, JobGetText::class.java))
//            //.setPeriodic(10000)
//            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
//            .build())

        scheduler.schedule(JobInfo.Builder(2, ComponentName(context, JobGetText::class.java))
            .setPeriodic(1800000)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .build())

    }

    override fun onDisabled(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(3)
        scheduler.cancel(2)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        for (id in appWidgetIds) {
            editor.remove("x1$id")
            editor.remove("x2$id")
            editor.remove("y1$id")
            editor.remove("y2$id")
            editor.remove("page$id")
        }
        editor.apply()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        when(intent?.action){
            UPDATE_HEADER -> {

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val remoteViews = RemoteViews(context!!.packageName, R.layout.rasp_widget)
                val thisWidget = ComponentName(context, RaspWidget::class.java)
                val header = intent.getStringExtra(INTENT_EXTRA_STRING)
                remoteViews.setTextViewText(R.id.appwidget_text, HtmlCompat.fromHtml(header, 0))
                appWidgetManager.updateAppWidget(thisWidget, remoteViews)

            }
        }
    }

    companion object {

        const val UPDATE_HEADER = "maxim.drozd.updateHeader"
        const val INTENT_EXTRA_STRING = "maxim.drozd.string"
        const val PREFERENCES_HEADER = "maxim.drozd.preferences_header"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            //Toast.makeText(context, "lol", Toast.LENGTH_SHORT).show()

            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            scheduler.schedule(JobInfo.Builder(3, ComponentName(context, JobGetText::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build())

            val intentUpdate = Intent(context, RaspWidget::class.java)
            intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, RaspWidget::class.java))
            intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

            val pendingUpdate = PendingIntent.getBroadcast(context, appWidgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT)
            val views = RemoteViews(context.packageName, R.layout.rasp_widget)
            views.setOnClickPendingIntent(R.id.button, pendingUpdate)

            val header = PreferenceManager.getDefaultSharedPreferences(context).getString(PREFERENCES_HEADER, "У нас тут весело")
            views.setTextViewText(R.id.appwidget_text, HtmlCompat.fromHtml(header!!,0))
            Log.i("ggp", "Before Updater")

            if(isNetworkAvailable(context)) {
                Updater().execute(views, appWidgetManager, appWidgetId, context)
                views.setViewVisibility(R.id.progressBar, View.VISIBLE)
                views.setViewVisibility(R.id.button, View.INVISIBLE)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            else
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()

        }

        private fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val activeNetworkInfo = connectivityManager!!.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    class Updater: AsyncTask<Any, Void?, Void?>(){

        override fun doInBackground(vararg params: Any?): Void? {
            Log.d("ggp", "in Updater")
            val site = "http://www.mrk-bsuir.by/ru/"
            val id = params[2] as Int
            val context = params[3] as Context
            val views = params[0]!! as RemoteViews
            val client = OkHttpSingleon.getInstance(context)
            val request = Request.Builder().url(site).build()
            val response = client.newCall(request).execute()
            if(!response.isSuccessful){
                Toast.makeText(context, "Look's like MRC's site is down)", Toast.LENGTH_SHORT).show()
            } else {
                val resp = response.body()?.string()

                val pos1 = resp!!.indexOf("Объявления")
                val end1 = resp.indexOf(".pdf", pos1)
                val pdfUrl = resp.substring(pos1+78, end1+4)

                val pdfRequest = Request.Builder().url(pdfUrl).build()
                val responsePDF = client.newCall(pdfRequest).execute()
                if(!responsePDF.isSuccessful){
                    Toast.makeText(context, "Look's like MRC's site is down)", Toast.LENGTH_SHORT).show()
                } else {
                    val fileName = context.filesDir.absolutePath + "/rasp.pdf"
                    val fos = FileOutputStream(fileName)
                    fos.write(responsePDF.body()!!.bytes())
                    fos.close()

                    val f = File(fileName)
                    PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                        val pageNumber = sharedPreferences.getInt("page$id", 4)
                        val page = renderer.openPage(pageNumber)
                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        page.render(
                            bitmap,
                            Rect(0, 0, page.width * 2, page.height * 2),
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )
                        val x = sharedPreferences.getInt("x1$id", 0)
                        val y = sharedPreferences.getInt("y1$id", 0)
                        val width = sharedPreferences.getInt("x2$id", 10) - x
                        val height = sharedPreferences.getInt("y2$id", 10) - y
                        val bmp = Bitmap.createBitmap(bitmap, x, y, width, height)
                        views.setBitmap(R.id.imageView, "setImageBitmap", bmp)
                        page.close()
                    }


                    Log.i("ggp", pdfUrl)
                }
            }
            views.setViewVisibility(R.id.progressBar, View.INVISIBLE)
            views.setViewVisibility(R.id.button, View.VISIBLE)
            (params[1] as AppWidgetManager).updateAppWidget(params[2] as Int, views)
            return null
        }
    }
}
