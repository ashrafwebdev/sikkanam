package com.ashrafwebdev.sikkanam.payments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.ashrafwebdev.sikkanam.ui.saveReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** A receipt shared into Sikkanam from another app, such as Google Pay, waiting to be saved as an entry. */
data class SharedReceipt(
    val receiptName: String?,
    val text: SharedPaymentText,
)

/** Holds the latest shared receipt until the user saves or dismisses it. */
object IncomingShare {
    private val _pending = MutableStateFlow<SharedReceipt?>(null)
    val pending: StateFlow<SharedReceipt?> = _pending

    fun clear() {
        _pending.value = null
    }

    /**
     * Reads a share intent. The image is copied into the app's private folder right away, since
     * access to it ends when the sharing app's grant does. Returns false if it isn't a share.
     */
    suspend fun accept(context: Context, intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_SEND) return false
        val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        val isImage = intent.type?.startsWith("image/") == true
        val receipt = if (stream != null && isImage) withContext(Dispatchers.IO) { saveReceipt(context, stream) } else null
        val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
            .joinToString(" ")
        if (receipt == null && text.isBlank()) return false
        _pending.value = SharedReceipt(receipt, parseSharedPaymentText(text))
        return true
    }
}
