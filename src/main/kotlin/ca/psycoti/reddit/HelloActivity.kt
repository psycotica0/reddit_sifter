package ca.psycoti.reddit

import android.os.Bundle
import android.app.Activity
import android.widget.TextView

import ca.psycoti.reddit.network.HotService

open class HelloActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.front)
  }

  override fun onStart() {
    super.onStart()
    val textView = findViewById(R.id.text_view) as TextView
    textView.setText("Hello Kotlin!")
    HotService.create().hot().subscribe(
      { obj -> textView.setText(obj.toString())},
      { err -> textView.setText(err.toString())}
    )
  }
}
