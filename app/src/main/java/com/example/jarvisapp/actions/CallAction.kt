package com.example.jarvisapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import org.json.JSONObject

class CallAction(private val context: Context) {

    fun execute(command: JSONObject) {
        val contact = command.optString("contact", "")
        if (contact.isBlank()) {
            Log.e("CallAction", "No contact specified")
            return
        }

        val number = getPhoneNumber(contact) ?: contact
        Log.i("CallAction", "Calling $contact → $number")

        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun getPhoneNumber(name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val cursor = context.contentResolver.query(
            uri, projection, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val displayName = it.getString(
                    it.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                ).lowercase()
                if (displayName.contains(name.lowercase())) {
                    return it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                    )
                }
            }
        }
        return null
    }
}