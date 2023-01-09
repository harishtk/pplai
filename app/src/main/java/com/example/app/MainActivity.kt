package com.example.app

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.example.app.commons.util.AppStartup
import com.example.app.commons.util.StorageUtil
import com.example.app.di.ApplicationDependencies
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppStartup.getInstance().onCriticalRenderEventStart()
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)

        setNavGraph()

        AppStartup.getInstance().onCriticalRenderEventEnd()
        lifecycleScope.launch { StorageUtil.cleanUp(applicationContext) }

        if (ApplicationDependencies.getPersistentStore().isLogged) {
            sharedViewModel.autoLogin()
        }
    }

    private fun setNavGraph() {
        val navHostFragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)
                    as NavHostFragment
        val navController: NavController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this)

        val inflater = navHostFragment.navController.navInflater
        val graph: NavGraph
        val startDestinationArgs = Bundle()

        when {
            ApplicationDependencies.getPersistentStore().isLogged -> {
                graph = inflater.inflate(R.navigation.home_nav_graph)
                graph.setStartDestination(R.id.stream_list)
            }
            else -> {
                graph = inflater.inflate(R.navigation.login_nav_graph)
                graph.setStartDestination(R.id.login_fragment)
            }
        }

        navController.setGraph(graph, startDestinationArgs)
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
            R.id.watch_live -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            R.id.publish_live -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            else -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    fun restart(args: Bundle? = null) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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