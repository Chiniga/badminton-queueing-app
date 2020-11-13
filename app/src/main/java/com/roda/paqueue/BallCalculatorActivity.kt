package com.roda.paqueue

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.roda.paqueue.models.Player
import io.realm.Realm
import io.realm.kotlin.where

class BallCalculatorActivity : AppCompatActivity() {

    private lateinit var playerCostListAdapter: PlayerCostListAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ball_calculator)

        playerCostListAdapter = PlayerCostListAdapter(this)
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
                /*if(spinnerStrings[position] == "Manual") {

                } else {

                }*/
                Toast.makeText(this@BallCalculatorActivity, spinnerStrings[position], Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
    }
}