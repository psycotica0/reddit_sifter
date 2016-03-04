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
import ca.psycoti.reddit.models.Entry;



interface HotService {
  @GET("/hot.json")
  public fun hot(): Observable<Listing>

  companion object {
    fun gson() = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Listing::class.java, Listing.Deserializer)
        .registerTypeAdapter(Entry::class.java, Entry.Deserializer)
        .create()

    fun create(): HotService {
      val adapter = Retrofit.Builder()
        .baseUrl("https://api.reddit.com")
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson()))
        .build()

      return adapter.create(HotService::class.java)
    }
  }
}
