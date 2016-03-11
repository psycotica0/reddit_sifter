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

import java.lang.reflect.Type
import kotlin.collections.List

import com.github.salomonbrys.kotson.*

fun l(s : String) = android.util.Log.e("TEST", s)

data class Listing(val kind: String, val entries: MutableList<Entry>, var after: String?) {
  constructor() : this("Listing", arrayListOf<Entry>(), null)
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
    val init = Pair(this.entries.count(), listOf<Operation>())
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
          this.entries.add(item)
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
      val kind = je["kind"].string
      val data = je["data"].obj
      val list = data["children"].array
      val after = data["after"].nullString

      val jList = HotService.gson().fromJson<MutableList<Entry>>(list)

      return Listing(kind, jList, after)
    }
  }
}
