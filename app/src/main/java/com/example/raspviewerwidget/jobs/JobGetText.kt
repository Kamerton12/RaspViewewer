package com.example.raspviewerwidget.jobs

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import com.example.raspviewerwidget.Aplicat
import com.example.raspviewerwidget.OkHttpSingleon
import com.example.raspviewerwidget.RaspWidget
import okhttp3.Request
import java.lang.ref.WeakReference

class JobGetText: JobService(){

    var task: AsyncTask<JobParameters?, Void, Void>? = null

    override fun onStopJob(params: JobParameters?): Boolean {
        task?.cancel(true)
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i("ggp", "In onStartJob")
        task = AsyncGetText(this).execute(params)
        return true
    }

    class AsyncGetText(joba: JobService): AsyncTask<JobParameters?, Void, Void>(){

        val job = WeakReference<JobService>(joba)

        @SuppressLint("ApplySharedPref")
        override fun doInBackground(vararg params: JobParameters?): Void? {
            if(!job.isEnqueued) {
                val client = OkHttpSingleon.getInstance(job.get()!!)
                val request = Request.Builder().url("https://kamerton12.github.io/RaspisunText/").build()
                val response = client.newCall(request).execute()
                val header = response.body()?.string()?.trim()
                Log.i("ggp", header)
                val intent = Intent(job.get()!!, RaspWidget::class.java)
                intent.action = RaspWidget.UPDATE_HEADER
                intent.putExtra(RaspWidget.INTENT_EXTRA_STRING, header)
                PreferenceManager.getDefaultSharedPreferences(job.get()!!).edit().putString(RaspWidget.PREFERENCES_HEADER, header).commit()
                job.get()?.sendBroadcast(intent)
            }
            job.get()?.jobFinished(params[0], true)
            return null
        }
    }
}