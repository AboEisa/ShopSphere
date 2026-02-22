package com.example.shopsphere.CleanArchitecture.data.module

import com.example.shopsphere.CleanArchitecture.data.Repository
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.ApiServices
import com.example.shopsphere.CleanArchitecture.data.network.DirectionsApiServices
import com.example.shopsphere.CleanArchitecture.data.network.DummyApiServices
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.RemoteDataSource
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.example.shopsphere.CleanArchitecture.domain.auth.FacebookLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.GoogleLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.LoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.RegisterUseCase
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.BASE_URL
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.DIRECTIONS_BASE_URL
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.DUMMY_BASE_URL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Dns
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet6Address
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named

@InstallIn(SingletonComponent::class)
@Module
object Module {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder().apply {
            this.dns(
                Dns { hostname ->
                    Dns.SYSTEM.lookup(hostname).sortedBy { it is Inet6Address }
                }
            )
            this.addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
        }.build()
    }

    @Singleton
    @Provides
    @Named("primaryRetrofit")
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(BASE_URL)
            .build()
    }

    @Singleton
    @Provides
    @Named("dummyRetrofit")
    fun provideDummyRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(DUMMY_BASE_URL)
            .build()
    }

    @Singleton
    @Provides
    @Named("directionsRetrofit")
    fun provideDirectionsRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(DIRECTIONS_BASE_URL)
            .build()
    }

    @Singleton
    @Provides
    fun getApiServices(@Named("primaryRetrofit") retrofit: Retrofit): ApiServices {
        return retrofit.create(ApiServices::class.java)
    }

    @Singleton
    @Provides
    fun getDummyApiServices(@Named("dummyRetrofit") retrofit: Retrofit): DummyApiServices {
        return retrofit.create(DummyApiServices::class.java)
    }

    @Singleton
    @Provides
    fun getDirectionsApiServices(@Named("directionsRetrofit") retrofit: Retrofit): DirectionsApiServices {
        return retrofit.create(DirectionsApiServices::class.java)
    }

    @Singleton
    @Provides
    fun getRemoteDataSource(
        firestore: FirebaseFirestore,
        dummyApiService: DummyApiServices
    ): IRemoteDataSource{
        return RemoteDataSource(firestore, dummyApiService)
    }



    @Singleton
    @Provides
    fun getRepository(remoteDataSource: IRemoteDataSource,sharedPreferencesHelper: SharedPreference, firebaseAuth: FirebaseAuth): IRepository {
        return Repository(remoteDataSource,sharedPreferencesHelper,firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    fun provideLoginUseCase(repo: IRepository) = LoginUseCase(repo)
    @Provides fun provideRegisterUseCase(repo: IRepository) = RegisterUseCase(repo)
    @Provides
    fun provideGoogleLoginUseCase(repo: IRepository) = GoogleLoginUseCase(repo)
    @Provides
    fun provideFacebookLoginUseCase(repo: IRepository) = FacebookLoginUseCase(repo)

}
