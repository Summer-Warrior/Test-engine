package com.example.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared singleton OkHttpClient for all PokéAPI and encyclopedia network fetches.
 * Avoids leaking connection pools and thread executors across repositories.
 */
object NetworkClientProvider {
  val sharedOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }
}
