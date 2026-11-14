package com.msbytes.statussaver.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.msbytes.statussaver.MainActivity
import com.msbytes.statussaver.R
import com.msbytes.statussaver.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** Small home-screen widget showing the number of new statuses. */
class StatusWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).settingsRepository()
        val count = repo.widgetNewCount.first()
        val title = if (count > 0) {
            context.resources.getQuantityString(R.plurals.widget_new_count, count, count)
        } else {
            context.getString(R.string.widget_no_new)
        }
        val appName = context.getString(R.string.app_name)

        provideContent {
            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(20.dp)
                        .background(ColorProvider(Color(0xFF5B4CF0)))
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = GlanceModifier.size(40.dp),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = appName,
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}
