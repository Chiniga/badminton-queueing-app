package com.roda.paqueue

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar

class QueueOptionsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue_options)
        val seekBar = findViewById<SeekBar>(R.id.seekBarQueueStyle)
        seekBar.setOnSeekBarChangeListener(this)
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
        TODO("Not yet implemented")
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        TODO("Not yet implemented")
    }
}