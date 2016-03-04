package ca.psycoti.reddit.network;

import rx.Observable

import retrofit.http.*
import retrofit.Retrofit
import retrofit.GsonConverterFactory
import retrofit.RxJavaCallAdapterFactory

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import com.google.gson.FieldNamingPolicy

import java.lang.reflect.Type

import ca.psycoti.reddit.models.Listing;



interface HotService {
  @GET("/hot.json")
  public fun hot(): Observable<JsonElement>

  companion object {
    fun create(): HotService {
      val gsonBuilder = GsonBuilder()

      val adapter = Retrofit.Builder()
        .baseUrl("https://api.reddit.com")
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
        .build()

      return adapter.create(HotService::class.java)
    }
  }
}
