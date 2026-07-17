package app.stylus.extension.reddit.patches;

import android.graphics.Typeface;

@SuppressWarnings("unused")
public final class ForceSystemFontPatch {

    /**
     * Injection point.
     *
     * <p>Returns the system default {@link Typeface} for the given style so the
     * app renders using the device's system font instead of its bundled font.
     *
     * @param style Typeface style (Typeface.NORMAL, BOLD, ITALIC, or BOLD_ITALIC).
     * @return The system default typeface for {@code style}.
     */
    public static Typeface getSystemTypeface(int style) {
        return Typeface.create(Typeface.DEFAULT, style);
    }
}
