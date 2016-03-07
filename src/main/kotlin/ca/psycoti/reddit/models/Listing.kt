package ca.psycoti.reddit.models;

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import com.google.gson.FieldNamingPolicy
import com.google.gson.reflect.TypeToken

import ca.psycoti.reddit.network.HotService

import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

import rx.subjects.PublishSubject


import java.util.ArrayList

import java.lang.reflect.Type

inline fun l(s : String) = android.util.Log.e("TEST", s)
inline fun <reified T> genericType() = object: TypeToken<T>() {}.type

data class Listing(val kind: String, val entries: ArrayList<Entry>, var after: String?) {
  constructor() : this("Listing", ArrayList(), null)
  val service = HotService.create()

  val loadAfter = PublishSubject.create<String>()
  //Dedupe

  val results = loadAfter
    .distinctUntilChanged()
    .switchMap({service.hot(it).subscribeOn(Schedulers.io())})
    .observeOn(AndroidSchedulers.mainThread())

  val diff = results.map({this.merge(it)})

  fun loadMore() = loadAfter.onNext(this.after)

  interface Operation
  data class Insert(val pos: Int, val data: Entry) : Operation
  data class Move(val from: Int, val to: Int) : Operation

  data class Diff(val items: List<Operation>)

  fun merge(other: Listing) : Diff {
    val init = Pair(this.entries.count(), ArrayList<Operation>())
    val (n, operations) = other.entries.fold(
      init,
      fun (state, item) : Pair<Int, List<Operation>> {
        val (idx, ops) = state
        val pos = this.entries.indexOf(item)
        if (pos == -1) {
          // New item
          this.entries.add(item)
          l("New " + idx + 1)
          return Pair(idx + 1, ops + Insert(idx, item))
        } else {
          // Move
          l("Move " + pos + " to "+ idx)
          this.entries.removeAt(pos)
          return Pair(idx, ops + Move(pos, idx))
        }
      }
    )
    this.after = other.after
    return Diff(operations)
  }

  object Deserializer: JsonDeserializer<Listing> {
    override fun deserialize(je: JsonElement, type: Type, jdc: JsonDeserializationContext): Listing
    {
      val kind = je.asJsonObject.get("kind").getAsString()
      val data = je.asJsonObject.getAsJsonObject("data")
      val list = data.getAsJsonArray("children")
      val after = data.get("after").asString

      val subtype = genericType<ArrayList<Entry>>()
      val jList = HotService.gson().fromJson<ArrayList<Entry>>(list, subtype)

      return Listing(kind, jList, after)
    }
  }
}
