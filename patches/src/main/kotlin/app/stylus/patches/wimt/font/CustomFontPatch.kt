package app.stylus.patches.wimt.font

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.stylus.patches.wimt.shared.Constants.COMPATIBILITY_WIMT
import java.io.File

private const val CUSTOM_FONT_RESOURCE_NAME = "custom_ui_font"

private val allowedFontExtensions = setOf("ttf", "otf")

private val fontResourcePrefixes = listOf(
    "gm3_",
    "nunitosans",
    "googlesans",
    "google_sans",
    "roboto",
)

private val nunitoFamilyEntries = listOf(
    300 to "normal",
    400 to "normal",
    500 to "normal",
    600 to "normal",
    700 to "normal",
    400 to "italic",
    600 to "italic",
)

private fun buildSingleFontXml(
    fontResourceName: String,
    fontWeight: Int,
    fontStyle: String,
): String = buildString {
    appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
    appendLine("<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\">")
    appendLine("    <font")
    appendLine("        android:font=\"@font/$fontResourceName\"")
    appendLine("        android:fontStyle=\"$fontStyle\"")
    appendLine("        android:fontWeight=\"$fontWeight\" />")
    append("</font-family>")
}

private fun buildFontFamilyXml(
    fontResourceName: String,
    entries: List<Pair<Int, String>>,
): String = buildString {
    appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
    appendLine("<font-family xmlns:android=\"http://schemas.android.com/apk/res/android\">")
    entries.forEach { (fontWeight, fontStyle) ->
        appendLine(
            "    <font android:font=\"@font/$fontResourceName\" " +
                "android:fontStyle=\"$fontStyle\" android:fontWeight=\"$fontWeight\" />",
        )
    }
    append("</font-family>")
}

private fun isTargetFontResource(file: File): Boolean {
    val extension = file.extension.lowercase()
    if (extension !in allowedFontExtensions && extension != "xml") {
        return false
    }

    val name = file.nameWithoutExtension.lowercase()

    // Keep monospace resources untouched, matching the behavior of the
    // existing GitHub custom font patch.
    if (name.contains("code") || name.contains("mono")) {
        return false
    }

    return fontResourcePrefixes.any { name.startsWith(it) }
}

private fun fontWeightFromName(name: String): Int {
    val lowerCaseName = name.lowercase()

    // Material 3 variable font resources encode their weight in the resource
    // name rather than in a font file. Labels use 500, emphasized labels use
    // 600, and the remaining emphasized roles use 500.
    if (lowerCaseName.startsWith("gm3_variable_")) {
        return when {
            lowerCaseName.contains("label") && lowerCaseName.contains("emphasized") -> 600
            lowerCaseName.contains("emphasized") || lowerCaseName.contains("label") -> 500
            else -> 400
        }
    }

    return when {
        lowerCaseName.contains("extrabold") -> 800
        lowerCaseName.contains("semibold") -> 600
        lowerCaseName.contains("bold") -> 700
        lowerCaseName.contains("medium") -> 500
        lowerCaseName.contains("light") -> 300
        lowerCaseName.contains("thin") -> 200
        else -> 400
    }
}

private fun fontStyleFromName(name: String): String =
    if (name.contains("italic", ignoreCase = true)) "italic" else "normal"

private val customFontFileOption = stringOption(
    key = "customFontFile",
    default = null,
    title = "Custom font",
    description = "The file path to your font file. Tap the file picker and select it. " +
        "The font file must be in TTF or OTF format. " +
        "A variable font is recommended so heading and body text keep their different weights.",
    required = true,
)

@Suppress("unused")
val customFontWimtPatch = resourcePatch(
    name = "Custom font (Where is my Train)",
    description = "Replaces Where is my Train's bundled Google Sans, Nunito Sans, and Roboto UI fonts with a custom font file.",
    default = false,
) {
    category("Font")

    compatibleWith(COMPATIBILITY_WIMT)

    customFontFileOption()

    val customFontPath by customFontFileOption

    execute {
        val path = customFontPath?.trim()
            ?: throw PatchException("No custom font was provided.")

        val fontFile = File(path)
        if (!fontFile.exists()) {
            throw PatchException("Custom font file not found: ${fontFile.absolutePath}")
        }

        if (!fontFile.isFile) {
            throw PatchException("Custom font path must be a file, not a folder: ${fontFile.absolutePath}")
        }

        val fontExtension = fontFile.extension.lowercase()
        if (fontExtension !in allowedFontExtensions) {
            throw PatchException(
                "Unsupported font format '${fontFile.extension}'. " +
                    "Use one of: ${allowedFontExtensions.joinToString(", ") { ".${it}" }}."
            )
        }

        val resDir = get("res")
        val fontResDirs = resDir.listFiles()
            ?.filter { it.isDirectory && (it.name == "font" || it.name.startsWith("font-")) }
            ?.sortedBy { it.name }
            ?: emptyList()

        if (fontResDirs.isEmpty()) {
            throw PatchException("No font resource directories were found in res.")
        }

        val baseFontResDir = fontResDirs.firstOrNull { it.name == "font" }
            ?: throw PatchException("The base res/font directory was not found.")

        // Remove a previously copied custom font with a different extension so
        // the target does not end up with two resources sharing the same name.
        fontResDirs.forEach { fontResDir ->
            listOf("ttf", "otf").forEach { extension ->
                fontResDir.resolve("$CUSTOM_FONT_RESOURCE_NAME.$extension").delete()
            }
        }

        fontFile.copyTo(
            baseFontResDir.resolve("$CUSTOM_FONT_RESOURCE_NAME.$fontExtension"),
            overwrite = true,
        )

        val targets = fontResDirs
            .flatMap { fontResDir ->
                fontResDir.listFiles()
                    ?.filter { it.isFile && isTargetFontResource(it) }
                    ?: emptyList()
            }
            .sortedWith(compareBy({ it.parentFile.name }, { it.nameWithoutExtension }))

        if (targets.isEmpty()) {
            throw PatchException("No compatible font resources were found in the res/font* directories.")
        }

        targets.forEach { target ->
            val resourceName = target.nameWithoutExtension

            if (target.extension.lowercase() in allowedFontExtensions && !target.delete()) {
                throw PatchException("Failed to delete font resource: ${target.absolutePath}")
            }

            val xmlFile = target.parentFile.resolve("$resourceName.xml")
            val xmlContent = if (resourceName.equals("NunitoSans", ignoreCase = true)) {
                buildFontFamilyXml(CUSTOM_FONT_RESOURCE_NAME, nunitoFamilyEntries)
            } else {
                buildSingleFontXml(
                    CUSTOM_FONT_RESOURCE_NAME,
                    fontWeightFromName(resourceName),
                    fontStyleFromName(resourceName),
                )
            }

            xmlFile.writeText(xmlContent)
        }
    }
}
