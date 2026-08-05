package app.stylus.patches.github.font

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.stylus.patches.github.shared.Constants.COMPATIBILITY_GITHUB

private const val EXTENSION_CLASS =
    "Lapp/stylus/extension/github/patches/ForceSystemFontPatch;"

@Suppress("unused")
val forceSystemFontGithubPatch = bytecodePatch(
    name = "Force system font (GitHub)",
    description = "Renders GitHub UI text using the device system font by overriding bundled font resources at runtime.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_GITHUB)

    extendWith("extensions/extension.mpe")

    execute {
        ResourcesCompatGetFontFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p0, p1, p3 }, $EXTENSION_CLASS->getSystemTypeface(Landroid/content/Context;II)Landroid/graphics/Typeface;
                move-result-object v0
                if-eqz v0, :original
                return-object v0
                :original
                nop
            """
        )
    }
}