package com.roda.paqueue

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.widget.doOnTextChanged

class QueueOptionsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private lateinit var queueStyleDescArray: Array<TextView>
    private lateinit var textViewNumGames: EditText
    private var layoutLevelStrictness: LinearLayout? = null
    private var layoutFrequency: LinearLayout? = null
    private var prevProgress: Int = 0
    private var numGames: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue_options)

        textViewNumGames = findViewById(R.id.editTextNumGames)
        layoutLevelStrictness = findViewById(R.id.layoutQueueSubOptions)
        layoutFrequency = findViewById(R.id.layoutQueueMixedSubOptions)
        queueStyleDescArray = arrayOf(
            findViewById(R.id.textViewOpenDesc),
            findViewById(R.id.textViewByLevelDesc),
            findViewById(R.id.textViewMixedDesc)
        )
        val seekBar = findViewById<SeekBar>(R.id.seekBarQueueStyle)
        seekBar.setOnSeekBarChangeListener(this)
        val btnSaveQueueOptions = findViewById<Button>(R.id.btnSaveQueueOptions)
        btnSaveQueueOptions.setOnClickListener(this)
        textViewNumGames.setText(numGames.toString())
        textViewNumGames.doOnTextChanged { text, _, _, _ ->
            numGames = text.toString().toInt()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        queueStyleDescArray[prevProgress].visibility = View.GONE
        queueStyleDescArray[progress].visibility = View.VISIBLE
        layoutLevelStrictness?.visibility = View.GONE
        layoutFrequency?.visibility = View.GONE

        if(progress != 0) {
            // show additional settings if queue option is "By Level" or "Mixed"
            layoutLevelStrictness?.visibility = View.VISIBLE

            if(progress == 2) {
                // show additional settings if queue option is "Mixed"
                layoutFrequency?.visibility = View.VISIBLE
            }
        }

        prevProgress = progress
    }

    override fun onStartTrackingTouch(p0: SeekBar?) { }

    override fun onStopTrackingTouch(p0: SeekBar?) { }

    override fun onClick(p0: View?) {
        Toast.makeText(this, "Saved queue options", Toast.LENGTH_SHORT).show()
    }
}