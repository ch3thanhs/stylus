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
        val targetMethod = resolveTypefaceCompatCreateFromResourcesFontFileTarget()

        // Param mapping changes between legacy and modern method shapes.
        targetMethod.method.addInstructionsWithLabels(
            0,
            when (targetMethod.variant) {
                TypefaceCompatCreateFromResourcesFontFileVariant.LEGACY_STATIC ->
                    """
                        invoke-static { p4 }, $EXTENSION_CLASS->getSystemTypeface(I)Landroid/graphics/Typeface;
                        move-result-object v0
                        if-eqz v0, :original
                        return-object v0
                        :original
                        nop
                    """

                TypefaceCompatCreateFromResourcesFontFileVariant.MODERN_VIRTUAL ->
                    """
                        invoke-static { p5 }, $EXTENSION_CLASS->getSystemTypeface(I)Landroid/graphics/Typeface;
                        move-result-object v0
                        if-eqz v0, :original
                        return-object v0
                        :original
                        nop
                    """
            }
        )
    }
}
