package app.stylus.patches.reddit.font

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.stylus.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT

private const val EXTENSION_CLASS =
    "Lapp/stylus/extension/reddit/patches/ForceSystemFontPatch;"

@Suppress("unused")
val forceSystemFontPatch = bytecodePatch(
    name = "Force system font",
    description = "Renders the app using the device's system font instead of Reddit Sans / Roboto.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_REDDIT)

    extendWith("extensions/extension.mpe")

    execute {
        // TypefaceCompat.createFromResourcesFontFile is the central choke point that
        // returns a Typeface for every R.font.* lookup made by AndroidX (and therefore
        // by Compose's FontFamily resolver and View's font attribute).
        // Param mapping for this static method:
        //   p0 = Resources, p1 = font res id, p2 = path,
        //   p3 = ttc index, p4 = style (Typeface.NORMAL / BOLD / ITALIC / BOLD_ITALIC).
        TypefaceCompatCreateFromResourcesFontFileFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p4 }, $EXTENSION_CLASS->getSystemTypeface(I)Landroid/graphics/Typeface;
                move-result-object v0
                if-eqz v0, :original
                return-object v0
                :original
                nop
            """
        )
    }
}
