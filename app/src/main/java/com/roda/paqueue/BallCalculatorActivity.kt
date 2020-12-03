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
import io.realm.Realm
import io.realm.kotlin.where

class BallCalculatorActivity : AppCompatActivity() {

    private lateinit var playerCostListAdapter: PlayerCostListAdapter
    private lateinit var recyclerView: RecyclerView
    private var ballCost: Double = 0.00
    private var calcOptionSelected: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ball_calculator)

        playerCostListAdapter = PlayerCostListAdapter(this, false)
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
                    addCost()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        val editTextBallCost = findViewById<TextInputEditText>(R.id.editTextBallCost)
        editTextBallCost.doOnTextChanged { text, _, _, _ ->
            ballCost = text.toString().toDouble()
            if (calcOptionSelected != 0) {
                addCost()
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

    private fun addCost() {
        when(calcOptionSelected) {
            1 -> {
                // automatic (1 game = 1 ball)
                Realm.getDefaultInstance().use { realm ->
                    val players = realm.where<Player>().findAll()
                    players.forEach { player ->
                        realm.executeTransaction {
                            player.total_cost = player.num_games * ballCost
                        }
                    }
                    playerCostListAdapter.updatePlayerCost(players)
                }
            }
            2 -> {
                // manual
            }
        }
    }
}