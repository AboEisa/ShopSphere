package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_HOME = "openHome"
        const val EXTRA_OPEN_LOGIN = "openLogin"
    }

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    private val rootDestinations = setOf(
        R.id.homeFragment,
        R.id.searchFragment,
        R.id.savedFragment,
        R.id.cartFragment,
        R.id.accountFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        val openHome = intent.getBooleanExtra(EXTRA_OPEN_HOME, false)
        val openLogin = intent.getBooleanExtra(EXTRA_OPEN_LOGIN, false)

        val graph = navController.navInflater.inflate(R.navigation.nav_graph)

        when {
            openHome -> graph.setStartDestination(R.id.homeFragment)
            openLogin -> graph.setStartDestination(R.id.loginFragment)
            else -> graph.setStartDestination(R.id.onBoardFragment)
        }

        navController.graph = graph

        // Connect bottom nav AFTER setting the graph
        binding.bottomNav.setupWithNavController(navController)

        // Hide or show bottom navigation based on current screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in rootDestinations) View.VISIBLE else View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun handleBackPress() {
        val currentId = navController.currentDestination?.id
        if (currentId == null) {
            finish()
            return
        }

        if (currentId in rootDestinations) {
            if (currentId != R.id.homeFragment) {
                binding.bottomNav.selectedItemId = R.id.homeFragment
            } else {
                finish()
            }
            return
        }

        if (!navController.popBackStack()) {
            finish()
        }
    }
}
