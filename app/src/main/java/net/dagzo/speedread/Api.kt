package net.dagzo.speedread

import com.facebook.stetho.okhttp3.StethoInterceptor
import io.reactivex.Single
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


class Api {

    private val yahooApi: IYahooApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jlp.yahooapis.jp")
            .client(
                OkHttpClient.Builder()
                    .addNetworkInterceptor(StethoInterceptor())
                    .build()
            )
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

        yahooApi = retrofit.create(IYahooApi::class.java)
    }

    fun parse(applicationId: String, sentence: String): Single<ResultSet> {
        return yahooApi.parse(
            applicationId,
            sentence,
            "ma"
        )
    }

    interface IYahooApi {
        @GET("/MAService/V1/parse")
        fun parse(@Query("appid") appId: String, @Query("sentence") sentence: String, @Query("results") results: String): Single<ResultSet>
    }

}