package app.stylus.extension.wimt.patches;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;

import java.util.Locale;

@SuppressWarnings("unused")
@SuppressLint("WrongConstant")
public final class ForceSystemFontPatch {

    private static final String SANS_SERIF = "sans-serif";
    private static final String SANS_SERIF_MEDIUM = "sans-serif-medium";

    private ForceSystemFontPatch() {
    }

    /**
     * Injection point.
     *
     * <p>Returns a system {@link Typeface} when the requested resource is one of
     * the bundled Google Sans, Nunito Sans, or Roboto fonts used by the app.
     * Returning {@code null} leaves the original resource untouched.
     *
     * @param context    Application context.
     * @param resourceId Font resource ID.
     * @param style      Requested typeface style.
     * @return The system replacement typeface, or {@code null} to leave the
     * original font in place.
     */
    public static Typeface getSystemTypeface(Context context, int resourceId, int style) {
        if (context == null || resourceId == 0) {
            return null;
        }

        String resourceName = resolveFontResourceName(context, resourceId);
        if (resourceName == null) {
            return null;
        }

        String name = resourceName.toLowerCase(Locale.ROOT);
        int requestedStyle = style & (Typeface.BOLD | Typeface.ITALIC);

        /*
         * Nunito Sans is the app's primary text font.
         */
        if (name.startsWith("nunitosans")) {
            return Typeface.create(
                    SANS_SERIF,
                    requestedStyle | styleFromName(name)
            );
        }

        /*
         * Google Sans is used for headings and display text. Google Sans Code
         * is the monospace variant.
         */
        if (name.startsWith("googlesans") || name.startsWith("google_sans")) {
            if (name.contains("code")) {
                return Typeface.create(
                        Typeface.MONOSPACE,
                        requestedStyle | styleFromName(name)
                );
            }

            String family = name.contains("medium")
                    ? SANS_SERIF_MEDIUM
                    : SANS_SERIF;

            return Typeface.create(
                    family,
                    requestedStyle | styleFromName(name)
            );
        }

        /*
         * Roboto is only used for a few fallback styles.
         */
        if (name.equals("roboto_regular") || name.startsWith("roboto")) {
            return Typeface.create(
                    SANS_SERIF,
                    requestedStyle | styleFromName(name)
            );
        }

        return null;
    }

    private static int styleFromName(String name) {
        int style = Typeface.NORMAL;

        /*
         * NunitoSans_SemiBold is the app's default body font, so treat it as a
         * normal-weight system font instead of making all body text bold.
         */
        if (name.contains("bold") && !name.contains("semibold")) {
            style |= Typeface.BOLD;
        }

        if (name.contains("italic")) {
            style |= Typeface.ITALIC;
        }

        return style;
    }

    private static String resolveFontResourceName(Context context, int resourceId) {
        try {
            Resources resources = context.getResources();

            if (!"font".equals(resources.getResourceTypeName(resourceId))) {
                return null;
            }

            return resources.getResourceEntryName(resourceId);
        } catch (Exception ignored) {
            return null;
        }
    }
}
