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
import com.roda.paqueue.models.Balls
import com.roda.paqueue.models.Player
import com.roda.paqueue.ui.queue.QueueConstants
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class BallCalculatorActivity : AppCompatActivity(), PlayerCostListAdapter.OnCalculationMethodChangeListener {

    private lateinit var playerCostListAdapter: PlayerCostListAdapter
    private lateinit var recyclerView: RecyclerView
    private var ballCost: Double = 0.00
    private var calcOptionSelected: Int = 0
    private object BallConstants {
        const val AUTOMATIC: Int = 1
        const val MANUAL: Int = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ball_calculator)

        playerCostListAdapter = PlayerCostListAdapter(this, this)
        recyclerView = findViewById(R.id.rvPlayerCost)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = playerCostListAdapter

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

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val editTextBallCost = findViewById<TextInputEditText>(R.id.editTextBallCost)
        editTextBallCost.doOnTextChanged { text, _, _, _ ->
            ballCost = text.toString().toDouble()
            playerCostListAdapter.updatePricePerBall(ballCost)
            if (calcOptionSelected != 0) {
                updateCost()
            }
        }

        Realm.getDefaultInstance().use { realm ->
            val players = realm.where<Player>().findAll()
            val balls = realm.where<Balls>().findFirst()
            playerCostListAdapter.addPlayers(players)

            balls?.cost?.toString()?.let { editTextBallCost.setText(it) }
            balls?.calculation_method?.let { spinnerCalcOptions.setSelection(it) }
        }
    }

    private fun updateCost() {
        Realm.getDefaultInstance().use { realm ->
            val players = realm.where<Player>().findAll()
            players.forEach { player ->
                val multiplier = if(calcOptionSelected == BallConstants.AUTOMATIC) player.num_games else player.balls_used
                realm.executeTransaction {
                    player.total_cost = multiplier * ballCost / QueueConstants.PLAYERS_PER_COURT
                }
            }

            realm.executeTransaction {
                var balls = realm.where<Balls>().findFirst()
                if (balls == null) {
                    balls = realm.createObject()
                }
                balls.cost = ballCost
                balls.calculation_method = calcOptionSelected
            }
            playerCostListAdapter.updatePlayerCost(players)
        }
    }

    override fun onMethodChange(viewHolder: PlayerCostListAdapter.PlayerCostViewHolder) {
        if (calcOptionSelected == BallConstants.AUTOMATIC) {
            viewHolder.imageViewAdd.visibility = View.GONE
            viewHolder.textViewNumBalls.visibility = View.GONE
            viewHolder.imageViewSub.visibility = View.GONE
            viewHolder.textViewPlayerGames.visibility = View.VISIBLE
        } else if (calcOptionSelected == BallConstants.MANUAL) {
            viewHolder.imageViewAdd.visibility = View.VISIBLE
            viewHolder.textViewNumBalls.visibility = View.VISIBLE
            viewHolder.imageViewSub.visibility = View.VISIBLE
            viewHolder.textViewPlayerGames.visibility = View.GONE
        }
    }
}