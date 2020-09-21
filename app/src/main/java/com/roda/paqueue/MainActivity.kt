package com.roda.paqueue

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.roda.paqueue.ui.players.PlayersFragment
import io.realm.Realm
import io.realm.RealmConfiguration

class MainActivity : AppCompatActivity(), PlayersFragment.OnPlayerCountChangeListener {

    var playersInputShown = true
    var queueInputShown = true
    var shufflePlayers = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Realm.init(this)
        // val realmConfig = RealmConfiguration.Builder().schemaVersion(1).migration(DbMigration()).build()
        val realmConfig = RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build()
        Realm.setDefaultConfiguration(realmConfig)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_queue, R.id.navigation_players, R.id.navigation_settings))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onPlayerCountChange(title: String) {
        supportActionBar?.title = title
    }
}
