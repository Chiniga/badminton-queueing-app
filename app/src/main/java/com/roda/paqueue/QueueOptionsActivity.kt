package com.roda.paqueue

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

class QueueOptionsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    private lateinit var queueStyleDescArray: Array<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue_options)
        val seekBar = findViewById<SeekBar>(R.id.seekBarQueueStyle)
        seekBar.setOnSeekBarChangeListener(this)
        queueStyleDescArray = arrayOf(
            findViewById(R.id.textViewOpenDesc),
            findViewById(R.id.textViewByLevelDesc),
            findViewById(R.id.textViewMixedDesc)
        )
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        for(desc in queueStyleDescArray) {
            desc.visibility = View.GONE
        }
        queueStyleDescArray[progress].visibility = View.VISIBLE
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    override fun onStopTrackingTouch(p0: SeekBar?) {

    }
}