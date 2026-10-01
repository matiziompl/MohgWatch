package com.mohgwatch.phone.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdateHelper {
    suspend fun updateAllWidgets(context: Context) {
        listOf(
            MohgWatchWidget(),
            InjectionSitesWidget(),
            CombinedWidget(),
            DashboardWidget(),
            CompactBarWidget()
        ).forEach { runCatching { it.updateAll(context) } }
    }
}
