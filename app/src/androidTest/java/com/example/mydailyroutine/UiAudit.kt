package com.example.mydailyroutine

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Screenshot and shell helpers shared by the instrumented tests.
 *
 * Gradle uninstalls both APKs once `connectedDebugAndroidTest` finishes, which deletes the app's
 * `Android/data` and `files` dirs, so the primary copy goes to `Pictures/ui-audit` through
 * MediaStore: that is public storage, survives the uninstall and needs no permission. The
 * app-private dirs are kept as a fallback for local `adb pull` runs.
 */
internal fun saveUiAudit(activity: Activity, name: String, image: ImageBitmap) {
    val bitmap = image.asAndroidBitmap()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ui-audit")
            }
            val resolver = activity.contentResolver
            val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
                "MediaStore refused $name.png"
            }
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "no output stream for $name.png" }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }.onFailure { error -> println("ui-audit: MediaStore save failed for $name: $error") }
    }
    val testOutput = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
    val targets = listOfNotNull(
        testOutput?.let { File(it) },
        activity.getExternalFilesDir(null)?.let { File(it, "ui-audit") },
        File(activity.filesDir, "ui-audit"),
    )
    for (directory in targets) {
        directory.mkdirs()
        runCatching {
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.onFailure { error -> println("ui-audit: failed to write $name into $directory: $error") }
    }
}

/**
 * Runs a shell command as the shell user and returns its output. Used by the font-scale test, which
 * has to change the device's own display metrics: an instrumented test cannot fake a smaller screen
 * for an activity that is already running, and a screenshot of a device that was never actually
 * narrow would be evidence of nothing.
 */
internal fun shell(command: String): String {
    val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    return descriptor.use { parcel ->
        java.io.FileInputStream(parcel.fileDescriptor).use { input -> input.readBytes().toString(Charsets.UTF_8) }
    }
}
