package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_HOME = "openHome"
        const val EXTRA_OPEN_LOGIN = "openLogin"
    }

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    @Inject
    lateinit var sharedPreference: SharedPreference

    // Bottom-nav tabs: Home / Favorites (saved) / Cart / Orders / Account.
    private val rootDestinations = setOf(
        R.id.homeFragment,
        R.id.savedFragment,
        R.id.cartFragment,
        R.id.ordersFragment,
        R.id.accountFragment
    )

    // Screens where the floating nav stays visible. Anything pushed on top of
    // a root tab (details, address book, checkout, …) hides the nav.
    private val bottomNavVisibleDestinations = rootDestinations + setOf(
        R.id.searchFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPersistedLanguage()
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

        // Robust tab navigation: some tabs (e.g. Account) don't have back-stack
        // entries that `setupWithNavController` can pop cleanly, so a user taps
        // "Home" and nothing visible happens. Always force-route to the picked
        // root destination with proper pop/restore options.
        binding.bottomNav.setOnItemSelectedListener { item ->
            val targetId = item.itemId
            if (targetId !in rootDestinations) return@setOnItemSelectedListener false
            val currentId = navController.currentDestination?.id
            if (currentId == targetId) return@setOnItemSelectedListener true

            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    inclusive = false,
                    saveState = true
                )
                .build()
            navController.navigate(targetId, null, options)
            true
        }
        // Tapping an already-selected tab: send the user to Home as a consistent
        // "home" gesture. (Otherwise reselection is a silent no-op on some devices.)
        binding.bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId != R.id.homeFragment) {
                navigateToHomeTab()
            }
        }

        // Hide or show bottom navigation based on current screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in bottomNavVisibleDestinations) View.VISIBLE else View.GONE
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

        if (currentId == R.id.homeFragment) {
            finish()
            return
        }

        if (currentId in rootDestinations) {
            // On a tab other than Home: go to Home via the nav controller directly
            // so it works even if BottomNavigationView.setSelectedItemId is a no-op
            // (e.g. when the item is already marked selected).
            navigateToHomeTab()
            return
        }

        if (!navController.popBackStack()) {
            navigateToHomeTab()
        }
    }

    private fun applyPersistedLanguage() {
        val tag = sharedPreference.getLanguage()
        if (tag.isNotBlank()) {
            val current = AppCompatDelegate.getApplicationLocales()
            if (current.isEmpty || current.toLanguageTags() != tag) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag)
                )
            }
        }
    }

    private fun navigateToHomeTab() {
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
            .build()
        navController.navigate(R.id.homeFragment, null, options)
    }
}
