package com.example.ku_connect.service

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.ku_connect.util.AppConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudinaryService {
    fun init(context: Context) {
        val config = mapOf(
            "cloud_name" to AppConfig.CLOUDINARY_CLOUD_NAME,
            "api_key"    to AppConfig.CLOUDINARY_API_KEY,
            "api_secret" to AppConfig.CLOUDINARY_API_SECRET
        )
        try {
            MediaManager.init(context, config)
        } catch (_: Exception) {
        }
    }

    private fun uniqueName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val random    = (100..999).random()
        return "${timestamp}_${random}"
    }

    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        folder: String = "ku_connect",
        fileName: String = ""
    ): String = suspendCancellableCoroutine { cont ->

        val mime         = getMimeType(context, uri)
        val isImage      = mime?.startsWith("image/") == true
        val resourceType = if (isImage) "image" else "raw"

        val uniqueId = uniqueName()
        val publicId = "$folder/$uniqueId"

        val ext = when {
            !fileName.contains('.') -> null
            else -> fileName.substringAfterLast('.').lowercase().takeIf { it.isNotBlank() }
        }

        MediaManager.get()
            .upload(uri)
            .option("upload_preset", AppConfig.CLOUDINARY_UPLOAD_PRESET)
            .option("resource_type", resourceType)
            .option("public_id", publicId)
            .apply {
                if (!isImage && ext != null) {
                    option("format", ext)
                }
            }
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("secure_url")?.toString() ?: ""
                    cont.resume(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    cont.resumeWithException(
                        Exception(error?.description ?: "Upload failed")
                    )
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    cont.resumeWithException(Exception("Upload rescheduled: retry later"))
                }
            })
            .dispatch(context)
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        val fromResolver = context.contentResolver.getType(uri)
        if (!fromResolver.isNullOrBlank()) return fromResolver

        val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return if (ext.isNotBlank())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        else null
    }
}