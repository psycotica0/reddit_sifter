package ca.psycoti.reddit.models;

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import com.google.gson.FieldNamingPolicy
import com.google.gson.reflect.TypeToken

import java.util.ArrayList

import java.lang.reflect.Type


data class Entry(val title: String, val subreddit: String, val thumbnail: String) {
  object Deserializer: JsonDeserializer<Entry> {
    override fun deserialize(je: JsonElement, type: Type, jdc: JsonDeserializationContext): Entry
    {
      val data = je.asJsonObject.get("data")
      return Gson().fromJson(data, type)
    }
  }
}
