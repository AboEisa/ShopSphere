package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.NavGraph


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    @Inject
    lateinit var sharedPref: SharedPreference   // <-- injected login cache

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        val openHome = intent.getBooleanExtra("openHome", false)

        val graph = navController.navInflater.inflate(R.navigation.nav_graph)

        if (openHome) {
            graph.setStartDestination(R.id.homeFragment)
        } else {
            graph.setStartDestination(R.id.onBoardFragment)
        }


        navController.graph = graph

        // ------------------------------------


        // Connect bottom nav AFTER setting the graph
        binding.bottomNav.setupWithNavController(navController)

        // Hide or show bottom navigation based on current screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.onBoardFragment,
                R.id.loginFragment,
                R.id.signupFragment -> {
                    binding.bottomNav.visibility = View.GONE
                }
                else -> binding.bottomNav.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
