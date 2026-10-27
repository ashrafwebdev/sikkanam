package com.ashrafwebdev.sikkanam.contacts

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.os.RemoteException
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** Whether the app may write to the phone's contacts. */
fun hasContactsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED

/**
 * Adds a new contact with the name and mobile number to the phone's contacts.
 * The contact is stored on the device only (no Google or other account).
 * Returns false if the permission is missing or the insert fails.
 */
fun saveToPhoneContacts(context: Context, name: String, phone: String): Boolean {
    if (!hasContactsPermission(context)) return false
    val ops = arrayListOf(
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build(),
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            .build(),
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            .build(),
    )
    return try {
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        true
    } catch (e: RemoteException) {
        false
    } catch (e: OperationApplicationException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
