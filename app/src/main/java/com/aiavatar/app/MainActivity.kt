package com.aiavatar.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.aiavatar.app.commons.util.AppStartup
import com.aiavatar.app.commons.util.StorageUtil
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.viewmodels.SharedViewModel
import com.aiavatar.app.viewmodels.UserViewModel
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private val sharedViewModel: SharedViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    private lateinit var mainNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        AppStartup.getInstance().onCriticalRenderEventStart()
        super.onCreate(savedInstanceState)
        installSplashScreen()
        checkSecureMode()
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
                userViewModel.autoLogin()
            }
        }

        ApplicationDependencies.getPersistentStore().apply {
            Timber.d("User data: logged = $isLogged userId = $userId")
        }

        setupObservers()
    }

    private fun setNavGraph(@IdRes jumpToDestination: Int? = null) {
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

        if (jumpToDestination != null) {
            graph = inflater.inflate(R.navigation.home_nav_graph)
            graph.setStartDestination(jumpToDestination)
        } else {
            val persistentStore = ApplicationDependencies.getPersistentStore()
            when {
                persistentStore.isProcessingModel &&
                    !persistentStore.isLogged -> {
                    graph = inflater.inflate(R.navigation.home_nav_graph)
                    graph.setStartDestination(R.id.avatar_status)
                }
                persistentStore.isLogged ||
                persistentStore.isUploadStepSkipped -> {
                    graph = inflater.inflate(R.navigation.home_nav_graph)
                    graph.setStartDestination(R.id.catalog_list)
                }
                persistentStore.isOnboardPresented -> {
                    graph = inflater.inflate(R.navigation.home_nav_graph)
                    graph.setStartDestination(R.id.upload_step_1)
                }
                else -> {
                    graph = inflater.inflate(R.navigation.home_nav_graph)
                    graph.setStartDestination(R.id.walkthrough_fragment)
                }
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
        mainNavController.handleDeepLink(intent)
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
        /*val isGuestUserFlow = userViewModel.loginUser
            .map { it == null }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    sharedViewModel.shouldShowStatus,
                    isGuestUserFlow,
                    Boolean::and
                ).collectLatest { show ->
                    if (show) {
                        safeCall {
                            mainNavController.apply {
                                val navOpts = defaultNavOptsBuilder()
                                    .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                                    .build()
                                navigate(R.id.avatar_status, null, navOpts)
                            }
                        }
                    }
                }
            }
        }*/
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.jumpToDestination.collectLatest { destinationId ->
                    setNavGraph(destinationId)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.forceUpdate.collectLatest { forceUpdate ->
                    if (forceUpdate) {
                        checkForUpdates()
                    }
                }
            }
        }
    }

    private fun checkForUpdates() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    || appUpdateInfo.availableVersionCode() > BuildConfig.VERSION_CODE
                ) {
                    gotoForceUpdate()
                } else {
                    // Nothing to do. Ignore the forceUpdate flag.
                }
            }.addOnFailureListener {
                // Nothing to do. Ignore the forceUpdate flag.
            }
    }

    private fun gotoForceUpdate() = safeCall {
        mainNavController.apply {
            val navOpts = defaultNavOptsBuilder()
                .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                .build()
            navigate(R.id.forceUpdate, null, navOpts)
        }
    }

    @Deprecated("use navigation")
    fun restart(args: Bundle? = null) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra("restart_hint", "self")
        if (args != null) {
            intent.putExtras(args)
        }
        startActivity(intent)
    }

    private fun checkSecureMode() {
        if (BuildConfig.IS_SECURED) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    companion object {

        const val DEFAULT_UI_RENDER_WAIT_TIME = 50L

        public fun getThumbnail(stream: String): String {
            return "${BuildConfig.THUMBNAIL_BASE_URL}${stream}/thumbnail_05.jpg".also {
                Timber.d("Thumb: id=$stream url=$it")
            }
        }
    }
}