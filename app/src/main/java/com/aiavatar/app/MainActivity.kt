package com.aiavatar.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.aiavatar.app.analytics.Analytics
import com.aiavatar.app.analytics.AnalyticsLogger
import com.aiavatar.app.commons.util.AppStartup
import com.aiavatar.app.commons.util.StorageUtil
import com.aiavatar.app.commons.util.net.ConnectivityManagerLiveData
import com.aiavatar.app.di.ApplicationDependencies
import com.aiavatar.app.eventbus.UnAuthorizedEvent
import com.aiavatar.app.viewmodels.SharedViewModel
import com.aiavatar.app.viewmodels.UserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject

const val NO_POSITION = -1


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    private val sharedViewModel: SharedViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private val googleSignInClient by lazy {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso)
    }

    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    private lateinit var mainNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        AppStartup.getInstance().onCriticalRenderEventStart()
        setUserPreferredTheme()
        installSplashScreen()
        super.onCreate(savedInstanceState)

        checkSecureMode()
        setContentView(R.layout.activity_main)


        ApplicationDependencies.setDisplaySize(getDisplaySize())
        ifDebug {
            lifecycleScope.launch {
                val token = ApplicationDependencies.getPersistentStore().fcmToken
                Timber.d("FCM Token: $token")
            }
        }

        val fragmentContainer = findViewById<FragmentContainerView>(R.id.fragment_container)

        fragmentContainer.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom
                val navHeight = windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
                val statusBarHeight = windowInsets.getInsets(WindowInsets.Type.statusBars()).top
                Timber.d("Insets: imeHeight=$imeHeight navHeight=$navHeight statusBarHeight=$statusBarHeight")
                fragmentContainer.setPadding(
                    0,
                    statusBarHeight,
                    0,
                    imeHeight.coerceAtLeast(navHeight)
                )
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

        ifDebug {
            lifecycleScope.launch {
                ApplicationDependencies.getPersistentStore().apply {
                    Timber.d("User data: logged = $isLogged userId = $userId")
                }
            }
        }

        setupObservers()
        handleDeepLinks(intent)
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
        if (!mainNavController.handleDeepLink(intent)) {
            handleDeepLinks(intent)
        }
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
            if (ApplicationDependencies.getPersistentStore().isLogged) {
                userViewModel.autoLogin()
            }
        }

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.connectionStateSharedFlow.collectLatest { connected ->
                    Timber.d("Network: connected = $connected")
                }
            }
        }

        sharedViewModel.createCheckDataFlow
            .onEach { createCheckData ->
                Timber.d("Create check: $createCheckData")
                if (createCheckData != null) {
                    if (createCheckData.siteDown) {
                        setNavGraph(jumpToDestination = R.id.maintenance)
                    }
                }
            }
            .launchWhenStarted(lifecycleOwner = this)
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

    private fun handleDeepLinks(intent: Intent?) {
        intent ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
                .addOnSuccessListener { pendingDynamicLink ->
                    if (pendingDynamicLink != null) {
                        var deepLink: Uri? = null
                        deepLink = pendingDynamicLink.link
                        val keyType = deepLink?.getQueryParameter("keytype").nullAsEmpty()
                        val key = deepLink?.getQueryParameter("key").nullAsEmpty()
                        Timber.tag("Deeplink.Msg")
                            .d("Deep Link: %s key=$key keytype=$keyType", deepLink?.toString())
                        if (keyType == "invite" && key?.isNotEmpty() == true) {
                            ApplicationDependencies.getPersistentStore()
                                .setDeepLinkKey(keyType, key)
                            val bundle = Bundle().apply {
                                putInt("parseId", 0)
                                putString("keytype", keyType)
                                putString("key", key)
                            }
                            analyticsLogger.logEvent(
                                Analytics.OtherEvents.DEEP_LINK_INVITATION,
                                bundle
                            )
                        } else if (keyType == "profile" && key?.isNotEmpty() == true) {
                            ApplicationDependencies.getPersistentStore()
                                .setDeepLinkKey(keyType, key)
                            // TODO: goto profile
                        } else {
                            ApplicationDependencies.getPersistentStore()
                                .setDeepLinkKey(keyType, key)
                        }
                    } else {
                        ifDebug {
                            val t = IllegalStateException("Failed to get deep link data!")
                            Timber.e(t)
                        }
                    }
                }.addOnFailureListener {
                    FirebaseDynamicLinks.getInstance().getDynamicLink(intent.data!!)
                        .addOnSuccessListener { pendingDynamicLink ->
                            if (pendingDynamicLink != null) {
                                var deepLink: Uri? = null
                                deepLink = pendingDynamicLink.link
                                val keyType = deepLink?.getQueryParameter("keytype").toString()
                                val key = deepLink?.getQueryParameter("key").toString()
                                if (keyType == "invite" && key?.isNotEmpty() == true) {
                                    ApplicationDependencies.getPersistentStore()
                                        .setDeepLinkKey(keyType, key)
                                    val bundle = Bundle().apply {
                                        putInt("parseId", 1)
                                        putString("keytype", keyType)
                                        putString("key", key)
                                    }
                                    analyticsLogger.logEvent(
                                        Analytics.OtherEvents.DEEP_LINK_INVITATION,
                                        bundle
                                    )
                                } else if (keyType == "profile" && key?.isNotEmpty() == true) {
                                    // TODO: goto profile
                                }
                            } else {
                                ifDebug {
                                    val t = IllegalStateException("Failed to get deep link data!")
                                    Timber.e(t)
                                }
                            }
                        }.addOnFailureListener {
                            ifDebug {
                                val t = IllegalStateException("Failed to get deep link data", it)
                                Timber.e(t)
                            }
                        }
                }
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

    private fun setUserPreferredTheme() {
        ifDebug {
            ApplicationDependencies.getPersistentStore().userPreferredTheme.let { theme ->
                val mode = when (theme) {
                    THEME_MODE_AUTO -> {
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    THEME_MODE_DARK -> {
                        AppCompatDelegate.MODE_NIGHT_YES
                    }
                    THEME_MODE_LIGHT -> {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    /**
     * This will keep the splash for a pre-defined time.
     *
     * @param durationMillis - Time to keep
     */
    private fun keepSplash(durationMillis: Long = DEFAULT_SPLASH_DURATION) {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                Thread.sleep(durationMillis)
                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnAuthorizedEvent(e: UnAuthorizedEvent) {
        Timber.d("onUnAuthorizedEvent: $e")
        // TODO: gracefully logout the user.
        userViewModel.logout()
        ApplicationDependencies.getPersistentStore().logout()
        googleSignInClient.signOut()

        showToast("Session expired! Please log in again.")
        gotoUploadStep1()
    }

    private fun gotoUploadStep1() {
        try {
            mainNavController.apply {
                val navOpts = NavOptions.Builder()
                    .setExitAnim(R.anim.slide_bottom)
                    .setEnterAnim(R.anim.slide_up)
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.main_nav_graph, inclusive = true, saveState = false)
                    .build()
                navigate(R.id.upload_step_1, null, navOpts)
            }
        } catch (e: Exception) {
            Timber.e(e)
            restart()
        }
    }

    companion object {

        const val DEFAULT_UI_RENDER_WAIT_TIME = 50L
        const val DEFAULT_SPLASH_DURATION: Long = 500

        const val THEME_MODE_AUTO = 0
        const val THEME_MODE_LIGHT = 1
        const val THEME_MODE_DARK = 2

        val THEME_MAP = mapOf<Int, Int>(
            THEME_MODE_AUTO to R.drawable.baseline_auto_mode_24,
            THEME_MODE_LIGHT to R.drawable.baseline_light_mode_24,
            THEME_MODE_DARK to R.drawable.baseline_dark_mode_24
        )

        public fun getThumbnail(stream: String): String {
            return "${BuildConfig.THUMBNAIL_BASE_URL}${stream}/thumbnail_05.jpg".also {
                Timber.d("Thumb: id=$stream url=$it")
            }
        }
    }
}

