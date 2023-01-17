package com.aiavatar.app

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <T> Flow<T>.launchWhenStarted(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycleScope.launchWhenStarted {
        collect()
    }
}

fun <T> Flow<T>.launchWhenCreated(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycleScope.launchWhenCreated {
        collect()
    }
}

fun <T> Flow<T>.launch(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycleScope.launch {
       collect()
    }
}