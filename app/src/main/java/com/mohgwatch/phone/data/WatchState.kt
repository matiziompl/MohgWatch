package com.mohgwatch.phone.data

import kotlinx.coroutines.flow.MutableStateFlow

object WatchState {
    val isWornOnWrist = MutableStateFlow(true)
    val lastUpdateTimestamp = MutableStateFlow(0L)
}
