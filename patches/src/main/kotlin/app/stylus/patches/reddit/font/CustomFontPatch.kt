package app.stylus.patches.reddit.font

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.stylus.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import java.io.File

private const val EXTENSION_CLASS =
    "Lapp/stylus/extension/reddit/patches/CustomFontPatch;"

/**
 * Directory (relative to the APK `assets/` root) and file name where the user's
 * chosen font is bundled at patch time. The extension loads the typeface from
 * this exact asset path at runtime, so both sides must stay in sync.
 */
private const val FONT_ASSET_DIRECTORY = "stylus"
private const val FONT_ASSET_FILE_NAME = "custom_font.ttf"

private val allowedFontExtensions = setOf("ttf", "otf")

/**
 * File path to a `.ttf` / `.otf` font file on the machine running the patcher.
 *
 * The Morphe manager decides between a file picker and a folder picker by
 * inspecting the option text: an option whose description contains the literal
 * phrase "file path" (with no presets) is shown with a file picker. The phrase
 * below is therefore required to make the manager prompt for a single file
 * rather than a folder. Declared at top level so it can be shared between the
 * resource patch (which bundles the file) and be surfaced to the user while patching.
 */
private val customFontFileOption = stringOption(
    key = "customFontFile",
    default = null,
    title = "Custom font",
    description = "The file path to your font file. Tap the file picker and select it. " +
        "The font file must be in TTF or OTF format. " +
        "A variable font is recommended so heading and body text keep their different weights.",
    required = true,
)

private val customFontResourcePatch = resourcePatch {
    compatibleWith(COMPATIBILITY_REDDIT)

    val customFontPath by customFontFileOption

    execute {
        val path = customFontPath?.trim()
            ?: throw PatchException("No custom font was provided.")

        val fontFile = File(path)
        if (!fontFile.exists())
            throw PatchException("Custom font file not found: ${fontFile.absolutePath}")

        if (!fontFile.isFile)
            throw PatchException("Custom font path must be a file, not a folder: ${fontFile.absolutePath}")

        if (fontFile.extension.lowercase() !in allowedFontExtensions)
            throw PatchException(
                "Unsupported font format '${fontFile.extension}'. " +
                    "Use one of: ${allowedFontExtensions.joinToString(", ") { ".$it" }}."
            )

        val fontDirectory = get("assets").resolve(FONT_ASSET_DIRECTORY)
        fontDirectory.mkdirs()

        fontFile.copyTo(fontDirectory.resolve(FONT_ASSET_FILE_NAME), overwrite = true)
    }
}

@Suppress("unused")
val customFontPatch = bytecodePatch(
    name = "Custom font",
    description = "Applies a custom font selected while patching, replacing Reddit Sans / Roboto. " +
        "A variable font is recommended so heading and body text keep their different weights. " +
        "Do not enable \"Force system font\" at the same time.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_REDDIT)

    dependsOn(customFontResourcePatch)

    extendWith("extensions/extension.mpe")

    // Surface the font option on this (user-selected) patch so the patcher
    // prompts for a font while patching. The dependency resource patch reads
    // the same shared option to bundle the chosen file.
    customFontFileOption()

    execute {
        // TypefaceCompat.createFromResourcesFontFile is the central choke point that
        // returns a Typeface for every R.font.* lookup made by AndroidX (and therefore
        // by Compose's FontFamily resolver and View's font attribute).
        // Param mapping for this static method:
        //   p0 = Resources, p1 = font res id, p2 = path (e.g. reddit_sans_semi_bold.ttf),
        //   p3 = ttc index, p4 = style (Typeface.NORMAL / BOLD / ITALIC / BOLD_ITALIC).
        // p2 is forwarded so the extension can recover the requested weight from the
        // original font's file name and select that weight from the custom variable font.
        TypefaceCompatCreateFromResourcesFontFileFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0, p2, p4 }, $EXTENSION_CLASS->getCustomTypeface(Landroid/content/res/Resources;Ljava/lang/String;I)Landroid/graphics/Typeface;
                move-result-object v0
                if-eqz v0, :original
                return-object v0
                :original
                nop
            """
        )
    }
}
