package com.example.shopsphere.CleanArchitecture.data.module

import android.content.SharedPreferences
import com.example.shopsphere.CleanArchitecture.data.Repository
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.ApiServices
import com.example.shopsphere.CleanArchitecture.data.network.IRemoteDataSource
import com.example.shopsphere.CleanArchitecture.data.network.RemoteDataSource
import com.example.shopsphere.CleanArchitecture.domain.IRepository
import com.google.firebase.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object Module {

    @Singleton
    @Provides
    fun provideRetrofit() : Retrofit {

        val interceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder().apply {
            this.addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
        }.build()

        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl("https://fakestoreapi.com/")
            .build()
    }

    @Singleton
    @Provides
    fun getApiServices(retrofit: Retrofit): ApiServices {
        return retrofit.create(ApiServices::class.java)
    }

    @Singleton
    @Provides
    fun getRemoteDataSource(apiService: ApiServices): IRemoteDataSource{
        return RemoteDataSource(apiService)
    }



    @Singleton
    @Provides
    fun getRepository(remoteDataSource: IRemoteDataSource,sharedPreferencesHelper: SharedPreference): IRepository {
        return Repository(remoteDataSource,sharedPreferencesHelper)
    }
}