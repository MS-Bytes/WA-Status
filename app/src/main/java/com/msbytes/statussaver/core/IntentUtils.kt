package com.msbytes.statussaver.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object IntentUtils {
    fun appSettings(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun openUrl(url: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    fun viewMedia(uri: Uri, mimeType: String): Intent =
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    fun launchApp(context: Context, packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    fun playStoreApp(packageName: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

    fun email(address: String, subject: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).putExtra(Intent.EXTRA_SUBJECT, subject)

    /** wa.me deep link. [phone] must contain only digits including the country code. */
    fun directChat(phone: String, message: String, packageName: String?): Intent {
        val uri = Uri.parse("https://wa.me/$phone").buildUpon().apply {
            if (message.isNotBlank()) appendQueryParameter("text", message)
        }.build()
        return Intent(Intent.ACTION_VIEW, uri).apply { if (packageName != null) setPackage(packageName) }
    }
}
