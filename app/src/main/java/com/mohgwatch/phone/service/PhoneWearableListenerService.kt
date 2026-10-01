package com.mohgwatch.phone.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.mohgwatch.core.data.DataLayerPaths
import com.mohgwatch.phone.data.WatchState

/**
 * Nasłuchuje zdarzeń przychodzących z zegarka do telefonu przez DataLayer API.
 * Aktualizuje stan nadgarstka (WatchState).
 */
class PhoneWearableListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "PhoneWearableListener"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue

            if (path == DataLayerPaths.WRIST_STATUS) {
                try {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val isWorn = dataMap.getBoolean(DataLayerPaths.Keys.IS_WORN, true)
                    val timestamp = dataMap.getLong(DataLayerPaths.Keys.TIMESTAMP, System.currentTimeMillis())

                    WatchState.isWornOnWrist.value = isWorn
                    WatchState.lastUpdateTimestamp.value = timestamp

                    Log.d(TAG, "Odebrano stan nadgarstka: isWorn=$isWorn")
                } catch (e: Exception) {
                    Log.e(TAG, "Błąd przetwarzania stanu nadgarstka", e)
                }
            }
        }
    }
}
