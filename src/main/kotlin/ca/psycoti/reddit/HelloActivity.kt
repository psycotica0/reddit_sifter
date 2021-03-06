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
import android.widget.Toast

import android.support.percent.PercentFrameLayout

import ca.psycoti.reddit.network.HotService

import ca.psycoti.reddit.models.Entry
import ca.psycoti.reddit.models.Listing

import android.support.v7.widget.helper.ItemTouchHelper

open class HelloActivity : Activity() {
  lateinit var entries: RecyclerView
  val listing = Listing()
  val adapter = EntryAdapter(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.front)

    entries = findViewById(R.id.list_view) as RecyclerView
    entries.layoutManager = LinearLayoutManager(this)

    entries.setAdapter(adapter)

    val swipeCallback = SwipeCallback()
    ItemTouchHelper(swipeCallback).attachToRecyclerView(entries)

    swipeCallback.itemHidden.subscribe {
      // Currently I just temporarily remove it
      adapter.items.removeAt(it)
      adapter.notifyItemRemoved(it)
    }

    swipeCallback.itemStashed.subscribe {
      // Do nothing for now
      adapter.notifyItemChanged(it)
    }

    adapter.clickedItems.subscribe {
      //startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url)))
      val intent = Intent(this, ItemViewActivity::class.java)
      intent.putExtra(ItemViewActivity.URL, it.url)
      startActivity(intent)
    }

    adapter.loadingSignal.subscribe { listing.loadMore() }

    val metrics = DisplayMetrics()

    getWindowManager()
      .getDefaultDisplay()
      .getMetrics(metrics);
    adapter.width = metrics.widthPixels

    listing.diff.subscribe(
      {diff ->
        val oldLast = adapter.getItemCount() - 1
        adapter.items = listing.entries

        if (oldLast == 0) {
          // This is the first run, remove spinner, it's all inserts
          // If I let it go through the other logic, it works but moves the
          // spinner to the bottom of the screen and only loads stuff above
          // that
          adapter.notifyItemRemoved(oldLast)
          adapter.notifyItemRangeInserted(oldLast, adapter.getItemCount())
        } else {
          // We put everything at +1, then move the spinner at the end
          diff.items.forEach({
            when(it) {
              is Listing.Insert -> {
                adapter.notifyItemInserted(it.pos + 1)
              }
              is Listing.Move -> {
                adapter.notifyItemMoved(it.from, it.to + 1)
              }
            }
          })

          // Move the spinner back to the end
          adapter.notifyItemMoved(oldLast, adapter.getItemCount() - 1)
        }
      },
      {err -> Toast.makeText(this, err.toString(), Toast.LENGTH_LONG).show()}
    )
  }

  class EntryAdapter(val context : Context, var items : MutableList<Entry> = ArrayList()): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount() = items.count() + 1
    var width: Int = 0
    val clickedItems = PublishSubject.create<Entry>()
    val loadingSignal = PublishSubject.create<Unit>()
    enum class ViewType {ITEM, LOADING}

    override fun getItemViewType(position: Int)  = if (position == items.count())
      ViewType.LOADING.ordinal
      else ViewType.ITEM.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
      when(ViewType.values()[viewType]) {
        ViewType.ITEM -> {
          val view = LayoutInflater.from(context).inflate(R.layout.item, parent, false)
          return EntryViewHolder(view)
        }
        ViewType.LOADING -> {
          val view = LayoutInflater.from(context).inflate(R.layout.loading, parent, false)
          return LoadingViewHolder(view)
        }
      }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
      when(holder) {
        is LoadingViewHolder -> {loadingSignal.onNext(Unit)}
        is EntryViewHolder -> {
          val (title, subreddit, url, selftext, imageSet) = items[position]
          holder.view.setOnClickListener {
            // Get the position again, because it might have changed since we
            // built this
            clickedItems.onNext(items[holder.getAdapterPosition()])
          }
          holder.titleView.text = title
          holder.subredditView.text = subreddit

          val img = imageSet?.biggestImage(width)

          if (selftext == null) {
            holder.body.visibility = View.GONE
            holder.line.visibility = View.GONE
          } else {
            holder.body.visibility = View.VISIBLE
            holder.line.visibility = View.VISIBLE
            holder.body.text = selftext
          }

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
      }
    }

    class EntryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
      val titleView = view.findViewById(R.id.title) as TextView
      val subredditView = view.findViewById(R.id.subreddit) as TextView
      val thumbnail = view.findViewById(R.id.thumbnail) as ImageView
      val body = view.findViewById(R.id.body) as TextView
      val line = view.findViewById(R.id.line)
    }

    class LoadingViewHolder(val view: View) : RecyclerView.ViewHolder(view)
  }

  class SwipeCallback : ItemTouchHelper.Callback() {

    val itemHidden = PublishSubject.create<Int>()
    val itemStashed = PublishSubject.create<Int>()

    override fun getMovementFlags(recycler: RecyclerView, holder: RecyclerView.ViewHolder): Int {
      when(holder) {
        is EntryAdapter.LoadingViewHolder -> return 0
        else -> return makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE, ItemTouchHelper.START or ItemTouchHelper.END)
      }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
      when(direction) {
        ItemTouchHelper.START -> itemStashed.onNext(viewHolder.getAdapterPosition())
        ItemTouchHelper.END -> itemHidden.onNext(viewHolder.getAdapterPosition())
      }
    }

    override fun onMove(
      recycler: RecyclerView,
      view1: RecyclerView.ViewHolder,
      view2: RecyclerView.ViewHolder) = false
  }


}
