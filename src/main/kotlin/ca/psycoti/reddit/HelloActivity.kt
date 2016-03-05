package ca.psycoti.reddit

import android.os.Bundle
import android.app.Activity
import android.widget.TextView
import android.widget.ImageView
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.support.v7.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import android.content.Context

import ca.psycoti.reddit.network.HotService

import ca.psycoti.reddit.models.Entry

open class HelloActivity : Activity() {
  lateinit var entries: RecyclerView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.front)

    entries = findViewById(R.id.list_view) as RecyclerView
    entries.layoutManager = LinearLayoutManager(this)
  }

  override fun onStart() {
    super.onStart()
    val textView = findViewById(R.id.text_view) as TextView
    textView.setText("Hello Kotlin!")
    HotService.create().hot()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
      { obj -> entries.setAdapter(EntryAdapter(this, obj.entries)) },
      { err -> textView.setText(err.toString()) }
    )
  }

  class EntryAdapter(val context : Context, val items : List<Entry>): RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {
    override fun getItemCount() = items.count()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
      val view = LayoutInflater.from(context).inflate(R.layout.item, parent, false)
      return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
      val (title, subreddit, thumbnail) = items[position]
      holder.titleView.text = title
      holder.subredditView.text = subreddit
      if (thumbnail.isNotBlank()) {
        Picasso.with(context).load(thumbnail).into(holder.thumbnail)
      } else {
        holder.thumbnail.setImageDrawable(null)
      }
    }

    class EntryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
      val titleView: TextView = view.findViewById(R.id.title) as TextView
      val subredditView: TextView = view.findViewById(R.id.subreddit) as TextView
      val thumbnail: ImageView = view.findViewById(R.id.thumbnail) as ImageView
    }
  }
}
