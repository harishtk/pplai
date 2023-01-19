package com.aiavatar.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.aiavatar.app.commons.util.AppStartup
import com.aiavatar.app.commons.util.StorageUtil
import com.aiavatar.app.di.ApplicationDependencies
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private val sharedViewModel: SharedViewModel by viewModels()

    private lateinit var mainNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        AppStartup.getInstance().onCriticalRenderEventStart()
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)

        val token = ApplicationDependencies.getPersistentStore().fcmToken
        Timber.d("FCM Token: $token")

        val fragmentContainer = findViewById<FragmentContainerView>(R.id.fragment_container)

        fragmentContainer.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom
                val navHeight = windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
                val statusBarHeight = windowInsets.getInsets(WindowInsets.Type.statusBars()).top
                Timber.d("Insets: imeHeight=$imeHeight navHeight=$navHeight statusBarHeight=$statusBarHeight")
                fragmentContainer.setPadding(0, statusBarHeight, 0, imeHeight.coerceAtLeast(navHeight))
            }
            windowInsets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }

        setNavGraph()

        AppStartup.getInstance().onCriticalRenderEventEnd()
        lifecycleScope.launch { StorageUtil.cleanUp(applicationContext) }

        val restartHint = intent.extras?.getString("restart_hint", "")
        if (ApplicationDependencies.getPersistentStore().isLogged) {
            if (restartHint != "from_login") {
                sharedViewModel.autoLogin()
            }
        }

        setupObservers()
    }

    private fun setNavGraph() {
        val navHostFragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
                    as NavHostFragment
        if (!this::mainNavController.isInitialized) {
            mainNavController = navHostFragment.navController
            mainNavController.addOnDestinationChangedListener(this)
        }

        val inflater = navHostFragment.navController.navInflater
        val graph: NavGraph
        val startDestinationArgs = Bundle()

        /*graph = inflater.inflate(R.navigation.home_nav_graph)
        graph.setStartDestination(R.id.catalog_list)*/

        val persistentStore = ApplicationDependencies.getPersistentStore()
        when {
            persistentStore.isProcessingModel ||
            persistentStore.isUploadingPhotos -> {
                graph = inflater.inflate(R.navigation.home_nav_graph)
                graph.setStartDestination(R.id.avatar_status)
            }
            persistentStore.isLogged -> {
                graph = inflater.inflate(R.navigation.home_nav_graph)
                graph.setStartDestination(R.id.catalog_list)
            }
            persistentStore.isOnboardPresented -> {
                graph = inflater.inflate(R.navigation.home_nav_graph)
                graph.setStartDestination(R.id.upload_step_1)
            }
            else -> {
                graph = inflater.inflate(R.navigation.login_nav_graph)
                graph.setStartDestination(R.id.walkthrough_fragment)
            }
        }

        mainNavController.setGraph(graph, startDestinationArgs)
    }

    override fun onSupportNavigateUp(): Boolean {
        return mainNavController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setNavGraph()
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?,
    ) {
        when (destination.id) {
            // TODO: parse destination
        }
    }

    private fun setupObservers() {
        /*lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.shouldShowStatus.collectLatest {
                    if (it) {
                        findNavController(R.id.fragment_container).apply {
                            clearBackStack(R.id.avatar_status)
                            navigate(R.id.avatar_status)
                        }
                    }
                }
            }
        }*/
    }

    fun restart(args: Bundle? = null) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra("restart_hint", "self")
        if (args != null) {
            intent.putExtras(args)
        }
        startActivity(intent)
    }

    companion object {

        public fun getThumbnail(stream: String): String {
            return "${BuildConfig.THUMBNAIL_BASE_URL}${stream}/thumbnail_05.jpg".also {
                Timber.d("Thumb: id=$stream url=$it")
            }
        }
    }
}