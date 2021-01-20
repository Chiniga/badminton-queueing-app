package com.roda.paqueue

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class QueueOptionsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private lateinit var queueStyleDescArray: Array<TextView>
    private var prevProgress: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue_options)

        queueStyleDescArray = arrayOf(
            findViewById(R.id.textViewOpenDesc),
            findViewById(R.id.textViewByLevelDesc),
            findViewById(R.id.textViewMixedDesc)
        )
        val seekBar = findViewById<SeekBar>(R.id.seekBarQueueStyle)
        seekBar.setOnSeekBarChangeListener(this)
        val btnSaveQueueOptions = findViewById<Button>(R.id.btnSaveQueueOptions)
        btnSaveQueueOptions.setOnClickListener(this)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        queueStyleDescArray[prevProgress].visibility = View.GONE
        queueStyleDescArray[progress].visibility = View.VISIBLE
        prevProgress = progress
    }

    override fun onStartTrackingTouch(p0: SeekBar?) { }

    override fun onStopTrackingTouch(p0: SeekBar?) { }

    override fun onClick(p0: View?) {
        Toast.makeText(this, "Saved queue options", Toast.LENGTH_SHORT).show()
    }
}