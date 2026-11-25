package com.mohgwatch.phone.data

import com.mohgwatch.core.model.WristStatus
import kotlinx.coroutines.flow.MutableStateFlow

object WatchState {
    val lastUpdateTimestamp = MutableStateFlow(0L)
    val wristStatus = MutableStateFlow(WristStatus.UNKNOWN)

    fun statusAt(nowMillis: Long): WristStatus = wristStatus.value.at(lastUpdateTimestamp.value, nowMillis)
}
