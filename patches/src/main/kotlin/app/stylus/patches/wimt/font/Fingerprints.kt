package app.stylus.patches.wimt.font

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * ResourcesCompat.getFont(Context, int, TypedValue, int, FontCallback, boolean, boolean).
 *
 * This is the shared entry point used by both the Android View and Compose
 * font loaders in the app.
 */
internal object ResourcesCompatGetFontFingerprint : Fingerprint(
    accessFlags = listOf(
        AccessFlags.PUBLIC,
        AccessFlags.STATIC,
    ),
    returnType = "Landroid/graphics/Typeface;",
    custom = { method, _ ->
        method.parameters.size == 7 &&
            method.parameters[0].type == "Landroid/content/Context;" &&
            method.parameters[1].type == "I" &&
            method.parameters[2].type == "Landroid/util/TypedValue;" &&
            method.parameters[3].type == "I" &&
            method.parameters[5].type == "Z" &&
            method.parameters[6].type == "Z"
    },
)
