package com.example.shopsphere.CleanArchitecture.data.module

import android.content.Context
import androidx.room.Room
import com.example.shopsphere.BuildConfig
import com.example.shopsphere.CleanArchitecture.data.Repository
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.local.notifications.AppDatabase
import com.example.shopsphere.CleanArchitecture.data.local.notifications.NotificationDao
import com.example.shopsphere.CleanArchitecture.data.network.ApiServices
import com.example.shopsphere.CleanArchitecture.data.network.DirectionsApiServices
import com.example.shopsphere.CleanArchitecture.data.network.DummyApiServices
import com.example.shopsphere.CleanArchitecture.data.network.GeminiApiService
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.RemoteDataSource
import com.example.shopsphere.CleanArchitecture.domain.GetMyDetailsUseCase
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.example.shopsphere.CleanArchitecture.domain.LogoutUseCase
import com.example.shopsphere.CleanArchitecture.domain.UpdateMyDetailsUseCase
import com.example.shopsphere.CleanArchitecture.domain.UploadImageUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.FacebookLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.GoogleLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.LoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.RegisterUseCase
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.BASE_URL
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.DIRECTIONS_BASE_URL
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.DUMMY_BASE_URL
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.GEMINI_BASE_URL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet6Address
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object Module {

    /**
     * Bypass the ngrok "You are about to visit…" browser interstitial.
     * Without this header ngrok returns a 403 HTML page instead of the API response.
     */
    private class NgrokInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()
            return chain.proceed(request)
        }
    }

    /**
     * Attaches the saved JWT token as a Bearer Authorization header
     * to every request (when a token is available).
     */
    private class AuthInterceptor(private val prefs: SharedPreference) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = prefs.getToken()
            val requestBuilder = chain.request().newBuilder()
            if (token.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            return chain.proceed(requestBuilder.build())
        }
    }

    /**
     * Retry up to [maxRetries] times on genuine network failures (SocketTimeoutException,
     * non-cancel IOExceptions) before giving up.
     *
     * IMPORTANT: deliberately cancelled requests (scope cancellation on back-press / VM cleared)
     * throw IOException with message "Canceled" or "Socket closed". We MUST NOT retry those —
     * retrying a cancelled coroutine's request causes a cascade of duplicate API calls and
     * saturates the connection pool.
     */
    private class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var attempt = 0
            var lastException: Exception? = null
            while (attempt <= maxRetries) {
                try {
                    return chain.proceed(chain.request())
                } catch (e: java.net.SocketTimeoutException) {
                    lastException = e
                    attempt++
                } catch (e: java.io.IOException) {
                    // FIX: Do NOT retry deliberate cancellations. When a ViewModel is
                    // cleared (back press, navigation) its coroutine scope is cancelled,
                    // which causes OkHttp to throw IOException("Canceled") or
                    // IOException("Socket closed"). Retrying those creates duplicate POSTs
                    // (observed: 6 spurious AddToCart calls on back press in Logcat).
                    val msg = e.message?.lowercase().orEmpty()
                    if (msg.contains("cancel") || msg.contains("closed") || msg.contains("reset")) {
                        throw e
                    }
                    lastException = e
                    attempt++
                }
            }
            throw lastException ?: java.io.IOException("Request failed after $maxRetries retries")
        }
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(prefs: SharedPreference): OkHttpClient {
        return OkHttpClient.Builder().apply {
            // Prefer IPv4 — avoids ngrok IPv6 resolution failures on some devices/emulators
            dns(Dns { hostname ->
                Dns.SYSTEM.lookup(hostname).sortedBy { it is Inet6Address }
            })

            // Increased timeouts — ngrok free tier is slow
            connectTimeout(60, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            writeTimeout(60, TimeUnit.SECONDS)

            // Add ngrok browser-warning bypass header
            addInterceptor(NgrokInterceptor())

            // Attach Bearer token for authenticated requests
            addInterceptor(AuthInterceptor(prefs))

            // Retry on genuine network failures (NOT cancellations — see class javadoc)
            addInterceptor(RetryInterceptor(maxRetries = 2))

            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build()
    }

    @Singleton
    @Provides
    @Named("primaryRetrofit")
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(BASE_URL)
            .build()

    @Singleton
    @Provides
    @Named("dummyRetrofit")
    fun provideDummyRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(DUMMY_BASE_URL)
            .build()

    @Singleton
    @Provides
    @Named("directionsRetrofit")
    fun provideDirectionsRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(DIRECTIONS_BASE_URL)
            .build()

    @Singleton
    @Provides
    fun getApiServices(@Named("primaryRetrofit") retrofit: Retrofit): ApiServices =
        retrofit.create(ApiServices::class.java)

    @Singleton
    @Provides
    fun getDummyApiServices(@Named("dummyRetrofit") retrofit: Retrofit): DummyApiServices =
        retrofit.create(DummyApiServices::class.java)

    @Singleton
    @Provides
    fun getDirectionsApiServices(@Named("directionsRetrofit") retrofit: Retrofit): DirectionsApiServices =
        retrofit.create(DirectionsApiServices::class.java)

    // ─── Gemini (Sphere AI chatbot) ──────────────────────────────────────────
    // Gemini needs a plain OkHttpClient without our Bearer/Ngrok interceptors —
    // Google's endpoint rejects the `ngrok-skip-browser-warning` header noise.
    @Singleton
    @Provides
    @Named("geminiOkHttpClient")
    fun provideGeminiOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()

    @Singleton
    @Provides
    @Named("geminiRetrofit")
    fun provideGeminiRetrofit(
        @Named("geminiOkHttpClient") client: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .baseUrl(GEMINI_BASE_URL)
        .build()

    @Singleton
    @Provides
    fun getGeminiApiService(@Named("geminiRetrofit") retrofit: Retrofit): GeminiApiService =
        retrofit.create(GeminiApiService::class.java)

    @Singleton
    @Provides
    fun getRemoteDataSource(
        apiServices: ApiServices,
        prefs: SharedPreference
    ): IRemoteDataSource =
        RemoteDataSource(apiServices, prefs)

    @Singleton
    @Provides
    fun getRepository(
        remoteDataSource: IRemoteDataSource,
        sharedPreferencesHelper: SharedPreference
    ): IRepository = Repository(remoteDataSource, sharedPreferencesHelper)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    fun provideLoginUseCase(repo: IRepository) = LoginUseCase(repo)

    @Provides
    fun provideRegisterUseCase(repo: IRepository) = RegisterUseCase(repo)

    @Provides
    fun provideGoogleLoginUseCase(repo: IRepository) = GoogleLoginUseCase(repo)

    @Provides
    fun provideFacebookLoginUseCase(repo: IRepository) = FacebookLoginUseCase(repo)

    // ─── Profile use-cases (AccountViewModel) ────────────────────────────────

    @Provides
    fun provideGetMyDetailsUseCase(remote: IRemoteDataSource) = GetMyDetailsUseCase(remote)

    @Provides
    fun provideUpdateMyDetailsUseCase(remote: IRemoteDataSource) = UpdateMyDetailsUseCase(remote)

    @Provides
    fun provideUploadImageUseCase(remote: IRemoteDataSource) = UploadImageUseCase(remote)

    @Provides
    fun provideLogoutUseCase(remote: IRemoteDataSource, repo: IRepository) = LogoutUseCase(remote, repo)

    // ─── Room (notifications) ────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()
}
