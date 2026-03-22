package com.climasaude.data.network

import android.content.Context
import com.climasaude.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    @Provides
    @Singleton
    fun provideCache(@ApplicationContext context: Context): Cache {
        val cacheDirectory = File(context.cacheDir, "http_cache")
        return Cache(cacheDirectory, CACHE_SIZE)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    @Named("cacheInterceptor")
    fun provideCacheInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val cacheControl = CacheControl.Builder()
                .maxAge(5, TimeUnit.MINUTES) // Cache for 5 minutes
                .build()

            val newRequest = request.newBuilder()
                .cacheControl(cacheControl)
                .build()

            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    @Named("offlineCacheInterceptor")
    fun provideOfflineCacheInterceptor(@ApplicationContext context: Context): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()

            if (!isNetworkAvailable(context)) {
                val cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS) // Use cache for up to 7 days when offline
                    .build()

                request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build()
            }

            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    @Named("apiKeyInterceptor")
    fun provideApiKeyInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url.newBuilder()
                .addQueryParameter("appid", BuildConfig.WEATHER_API_KEY)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(url)
                .addHeader("User-Agent", "ClimaSaude/1.0.0")
                .addHeader("Accept", "application/json")
                .build()

            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("cacheInterceptor") cacheInterceptor: Interceptor,
        @Named("offlineCacheInterceptor") offlineCacheInterceptor: Interceptor,
        @Named("apiKeyInterceptor") apiKeyInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService {
        return retrofit.create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideHealthApiService(retrofit: Retrofit): HealthApiService {
        return retrofit.create(HealthApiService::class.java)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(
            android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }
}
