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
import java.util.ArrayList
import android.util.DisplayMetrics
import android.view.WindowManager
import rx.subjects.PublishSubject

import android.content.Intent
import android.net.Uri

import android.support.percent.PercentFrameLayout

import com.jakewharton.rxbinding.view.clicks

import ca.psycoti.reddit.network.HotService

import ca.psycoti.reddit.models.Entry
import ca.psycoti.reddit.models.Listing

open class HelloActivity : Activity() {
  lateinit var entries: RecyclerView
  val listing = Listing()
  val adapter = EntryAdapter(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.front)

    entries = findViewById(R.id.list_view) as RecyclerView
    entries.layoutManager = LinearLayoutManager(this)

    val textView = findViewById(R.id.text_view) as TextView
    textView.setText("Hello Kotlin!")
    textView.clicks().subscribe({listing.loadMore()})
    entries.setAdapter(adapter)

    adapter.clickedItems.subscribe {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
    }

    val metrics = DisplayMetrics()

    getWindowManager()
      .getDefaultDisplay()
      .getMetrics(metrics);
    adapter.width = metrics.widthPixels

    listing.diff.subscribe(
      {diff ->
        adapter.items = listing.entries
        diff.items.forEach({
          when(it) {
            is Listing.Insert -> {
              adapter.notifyItemInserted(it.pos)
            }
            is Listing.Move -> {
              adapter.notifyItemMoved(it.from, it.to)
            }
          }
        })
      },
      {err -> textView.setText(err.toString())}
    )
    listing.loadMore()
  }

  class EntryAdapter(val context : Context, var items : List<Entry> = ArrayList()): RecyclerView.Adapter<EntryAdapter.EntryViewHolder>() {
    override fun getItemCount() = items.count()
    var width: Int = 0
    val clickedItems = PublishSubject.create<Entry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
      val view = LayoutInflater.from(context).inflate(R.layout.item, parent, false)
      return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
      val (title, subreddit, url, imageSet) = items[position]
      holder.view.setOnClickListener {
        clickedItems.onNext(items[position])
      }
      holder.titleView.text = title
      holder.subredditView.text = subreddit

      val img = imageSet?.biggestImage(width)

      if (img == null || img.url.isBlank()) {
        holder.thumbnail.setImageDrawable(null)
        holder.thumbnail.visibility = View.GONE
      } else {
        holder.thumbnail.visibility = View.VISIBLE
        val pParams = holder.thumbnail.layoutParams as? PercentFrameLayout.LayoutParams
        if (pParams != null) {
          pParams.percentLayoutInfo.aspectRatio =  img.width.toFloat() / img.height.toFloat()
        }
        Picasso.with(context).load(img.url).into(holder.thumbnail)
      }
    }

    class EntryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
      val titleView = view.findViewById(R.id.title) as TextView
      val subredditView = view.findViewById(R.id.subreddit) as TextView
      val thumbnail = view.findViewById(R.id.thumbnail) as ImageView
    }
  }
}
