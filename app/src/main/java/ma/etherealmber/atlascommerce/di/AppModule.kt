package ma.etherealmber.atlascommerce.di

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ma.etherealmber.atlascommerce.BuildConfig
import ma.etherealmber.atlascommerce.core.net.AtlasHeaderInterceptor
import ma.etherealmber.atlascommerce.core.net.SessionEvents
import ma.etherealmber.atlascommerce.core.net.SessionGuardInterceptor
import ma.etherealmber.atlascommerce.core.net.TokenHolder
import ma.etherealmber.atlascommerce.core.security.DeviceIdentity
import ma.etherealmber.atlascommerce.core.update.AndroidUpdateNotifier
import ma.etherealmber.atlascommerce.core.update.UpdateNotifier
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateBindingsModule {
    @Binds
    @Singleton
    abstract fun bindUpdateNotifier(impl: AndroidUpdateNotifier): UpdateNotifier
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        val errorHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("AtlasApp", "Unhandled background coroutine exception", throwable)
        }
        return CoroutineScope(SupervisorJob() + Dispatchers.IO + errorHandler)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenHolder: TokenHolder,
        deviceIdentity: DeviceIdentity,
        sessionEvents: SessionEvents,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            // OkHttp would otherwise replay a request (including POSTs) after a
            // stale pooled connection. Writes here must not happen twice, so
            // automatic retries are off; the UI can retry deliberately.
            .retryOnConnectionFailure(false)
            .addInterceptor(AtlasHeaderInterceptor(tokenHolder, deviceIdentity))
            .addInterceptor(SessionGuardInterceptor(sessionEvents))
            .addInterceptor(logging)
            .build()
    }

    /**
     * Client used to test a server address the user typed and check public updates.
     *
     * It must be **anonymous**: the probe may target a host that is not the one
     * the session belongs to, so it carries no `Authorization` header and no
     * device fingerprint — only the header the worker needs to answer.
     */
    @Provides
    @Singleton
    @ProbeClient
    fun provideProbeClient(deviceIdentity: DeviceIdentity): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", deviceIdentity.userAgent)
                .build()
            chain.proceed(req)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    /**
     * Streaming client for downloading large `.apk` update packages.
     *
     * `callTimeout` is unlimited (0) so a 30+ MB APK on slow mobile networks never
     * gets aborted mid-stream at 45 seconds as long as chunks keep arriving within
     * `readTimeout`.
     */
    @Provides
    @Singleton
    @UpdateClient
    fun provideUpdateClient(deviceIdentity: DeviceIdentity): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", deviceIdentity.userAgent)
                .header("X-Atlas-Client", "android")
                .build()
            chain.proceed(request)
        }
        .build()
}
