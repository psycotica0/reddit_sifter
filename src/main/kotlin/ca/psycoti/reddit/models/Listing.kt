package ca.psycoti.reddit.models;

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import com.google.gson.FieldNamingPolicy
import com.google.gson.reflect.TypeToken

import ca.psycoti.reddit.network.HotService


import java.util.ArrayList

import java.lang.reflect.Type

inline fun <reified T> genericType() = object: TypeToken<T>() {}.type

data class Listing(val kind: String, val entries: List<Entry>) {

  object Deserializer: JsonDeserializer<Listing> {
    override fun deserialize(je: JsonElement, type: Type, jdc: JsonDeserializationContext): Listing
    {
      val kind = je.asJsonObject.get("kind").getAsString()
      val list = je.asJsonObject.getAsJsonObject("data").getAsJsonArray("children")

      val subtype = genericType<ArrayList<Entry>>()
      val jList = HotService.gson().fromJson<ArrayList<Entry>>(list, subtype)

      return Listing(kind, jList)
    }
  }
}
