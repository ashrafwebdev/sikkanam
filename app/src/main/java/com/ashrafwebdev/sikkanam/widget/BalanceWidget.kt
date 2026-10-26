package com.ashrafwebdev.sikkanam.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ashrafwebdev.sikkanam.MainActivity
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.TransactionType
import com.ashrafwebdev.sikkanam.ui.formatMoney

/** Home-screen widget showing the all-time balance. Tapping it opens the app. */
class BalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val balance = currentBalanceCents(context)
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .background(Color(0xFFCDEFE1))
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            ) {
                Text(
                    "Sikkanam balance",
                    style = TextStyle(fontSize = 12.sp),
                )
                Text(
                    formatMoney(balance),
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }

    private suspend fun currentBalanceCents(context: Context): Long {
        val db = AppDatabase.create(context)
        return try {
            db.transactionDao().getAll().sumOf {
                if (it.type == TransactionType.INCOME) it.amountCents else -it.amountCents
            }
        } finally {
            db.close()
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
