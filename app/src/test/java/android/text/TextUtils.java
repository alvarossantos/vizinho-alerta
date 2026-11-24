package android.text;

/** Stub mínimo para exceções Firebase em testes JVM locais. */
public final class TextUtils {
    private TextUtils() {}

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
