package com.roda.paqueue

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.roda.paqueue.models.Player
import com.roda.paqueue.ui.queue.QueueConstants
import io.realm.Realm
import io.realm.kotlin.where

class BallCalculatorActivity : AppCompatActivity(), PlayerCostListAdapter.OnCalculationMethodChangeListener {

    private lateinit var playerCostListAdapter: PlayerCostListAdapter
    private lateinit var recyclerView: RecyclerView
    private var ballCost: Double = 0.00
    private var calcOptionSelected: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ball_calculator)

        playerCostListAdapter = PlayerCostListAdapter(this, this)
        recyclerView = findViewById(R.id.rvPlayerCost)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = playerCostListAdapter

        Realm.getDefaultInstance().use { realm ->
            val players = realm.where<Player>().findAll()
            playerCostListAdapter.addPlayers(players)
        }

        val spinnerStrings = resources.getStringArray(R.array.calc_options)
        val spinnerCalcOptions = findViewById<Spinner>(R.id.spinnerCalcOptions)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerStrings)
        spinnerCalcOptions.adapter = spinnerAdapter
        spinnerCalcOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calcOptionSelected = position
                if (calcOptionSelected != 0 && ballCost != 0.00) {
                    updateCost()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        val editTextBallCost = findViewById<TextInputEditText>(R.id.editTextBallCost)
        editTextBallCost.doOnTextChanged { text, _, _, _ ->
            ballCost = text.toString().toDouble()
            playerCostListAdapter.updatePricePerBall(ballCost)
            if (calcOptionSelected != 0) {
                updateCost()
            }
        }
        Log.d("TAG", "onCreate: $savedInstanceState")

        if (savedInstanceState != null) {
            with(savedInstanceState) {
                ballCost = getDouble("ballCost")
                calcOptionSelected = getInt("calcOptionSelected")
            }

            editTextBallCost.setText(ballCost.toString())
            spinnerCalcOptions.setSelection(calcOptionSelected)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("TAG", "onStop: ")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putDouble("ballCost", ballCost)
            putInt("calcOptionSelected", calcOptionSelected)
        }
        Log.d("TAG", "onSaveInstanceState: $outState")

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d("TAG", "onRestoreInstanceState: $savedInstanceState")
    }

    private fun updateCost() {
        Realm.getDefaultInstance().use { realm ->
            val players = realm.where<Player>().findAll()
            players.forEach { player ->
                val multiplier = if(calcOptionSelected == 1) player.num_games else player.balls_used
                realm.executeTransaction {
                    player.total_cost = multiplier * ballCost / QueueConstants.PLAYERS_PER_COURT
                }
            }
            playerCostListAdapter.updatePlayerCost(players)
        }
    }

    override fun onMethodChange(viewHolder: PlayerCostListAdapter.PlayerCostViewHolder) {
        if (calcOptionSelected == 1) {
            viewHolder.imageViewAdd.visibility = View.GONE
            viewHolder.textViewNumBalls.visibility = View.GONE
            viewHolder.imageViewSub.visibility = View.GONE
            viewHolder.textViewPlayerGames.visibility = View.VISIBLE
        } else {
            viewHolder.imageViewAdd.visibility = View.VISIBLE
            viewHolder.textViewNumBalls.visibility = View.VISIBLE
            viewHolder.imageViewSub.visibility = View.VISIBLE
            viewHolder.textViewPlayerGames.visibility = View.GONE
        }
    }
}