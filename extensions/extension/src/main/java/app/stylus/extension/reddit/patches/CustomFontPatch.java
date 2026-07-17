package app.stylus.extension.reddit.patches;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.LongSparseArray;

@SuppressWarnings("unused")
public final class CustomFontPatch {

    /**
     * Asset path (relative to the APK {@code assets/} root) where the patcher
     * bundled the user's chosen font. Must match the path written by the patch.
     */
    private static final String FONT_ASSET_PATH = "stylus/custom_font.ttf";

    /** Cache of instantiated typefaces keyed by weight + italic. */
    private static final LongSparseArray<Typeface> CACHE = new LongSparseArray<>();
    private static boolean loadFailed;

    /**
     * Injection point.
     *
     * <p>Returns a custom {@link Typeface} matching the weight the app requested for the
     * original font. The weight is recovered from {@code path} (the original font file
     * name, e.g. {@code reddit_sans_semi_bold.ttf}); when the bundled font is a variable
     * font, that weight is selected from its {@code wght} axis, preserving the
     * heading/body weight distinction. Returns {@code null} to fall back to the app's
     * original font loading if the custom font cannot be loaded.
     *
     * @param resources The {@link Resources} used to resolve the font asset.
     * @param path      The original font file path/name the app was loading.
     * @param style     Typeface style bits (used only to detect italic).
     * @return The custom typeface for the requested weight, or {@code null} to keep the original.
     */
    public static Typeface getCustomTypeface(Resources resources, String path, int style) {
        if (loadFailed) {
            return null;
        }

        int weight = weightFromPath(path);
        boolean italic = (style & Typeface.ITALIC) != 0
                || (path != null && path.toLowerCase().contains("italic"));

        long key = ((long) weight << 1) | (italic ? 1L : 0L);
        Typeface cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        Typeface typeface = build(resources.getAssets(), weight, italic);
        if (typeface == null) {
            loadFailed = true;
            return null;
        }

        CACHE.put(key, typeface);
        return typeface;
    }

    /**
     * Builds a {@link Typeface} from the bundled font at the given weight and slant.
     * For a variable font the {@code wght} (and {@code ital}) axes are applied; for a
     * static font {@link Typeface.Builder#setWeight} falls back to synthetic weighting.
     */
    private static Typeface build(AssetManager assets, int weight, boolean italic) {
        try {
            String variation = "'wght' " + weight + (italic ? ", 'ital' 1" : "");
            return new Typeface.Builder(assets, FONT_ASSET_PATH)
                    .setFontVariationSettings(variation)
                    .setWeight(weight)
                    .setItalic(italic)
                    .build();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Recovers the intended font weight from the original font file name. Reddit's font
     * resources encode the weight in the file name (e.g. {@code reddit_sans_semi_bold.ttf}).
     */
    private static int weightFromPath(String path) {
        if (path == null) {
            return 400;
        }
        String p = path.toLowerCase();
        if (p.contains("black")) {
            return 900;
        }
        if (p.contains("extrabold") || p.contains("extra_bold") || p.contains("extra-bold")) {
            return 800;
        }
        if (p.contains("semibold") || p.contains("semi_bold") || p.contains("semi-bold")
                || p.contains("demibold") || p.contains("demi_bold")) {
            return 600;
        }
        if (p.contains("bold")) {
            return 700;
        }
        if (p.contains("medium")) {
            return 500;
        }
        if (p.contains("light")) {
            return 300;
        }
        if (p.contains("thin")) {
            return 100;
        }
        return 400;
    }
}
