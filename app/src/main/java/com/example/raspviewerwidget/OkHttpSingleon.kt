package com.example.raspviewerwidget


import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient

class OkHttpSingleon{

    companion object{
        private var httpClient: OkHttpClient? = null

        @Synchronized
        public fun getInstance(context: Context): OkHttpClient{
            if (httpClient == null)
                httpClient = OkHttpClient.Builder().cache(Cache(context.cacheDir, 10 * 1024 * 1024L)).build()
            return httpClient!!

        }
    }
}