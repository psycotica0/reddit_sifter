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

import com.github.salomonbrys.kotson.*

data class Entry(val title: String, val subreddit: String, val url: String, val images: ImageSet?) {
  object Deserializer: JsonDeserializer<Entry> {
    override fun deserialize(je: JsonElement, type: Type, jdc: JsonDeserializationContext): Entry
    {
      val data = je["data"].obj

      val imageSet = if (data.has("preview"))
        ImageSet.parse(data["preview"])
        else null

      return Entry(data["title"].string, data["subreddit"].string, data["url"].string, imageSet)
    }
  }

  data class Image(val url: String, val width: Int, val height: Int)
  data class ImageSet(val images: List<Image>) {
    companion object {
      fun parse(json: JsonElement): ImageSet {
        val images = json["images"].array.flatMap({
          val source = Gson().fromJson<Image>(it["source"])
          val recurse = it["resolutions"].array.map({
            Gson().fromJson<Image>(it)
          })
          listOf(source) + recurse
        })
        return ImageSet(images)
      }
    }

    fun biggestImage(width: Int) = images.sortedBy({it.width}).findLast({it.width < width})
  }
}
