import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SigningSupport {

    private static final Pattern STORE_FILE_LINE =
        Pattern.compile("^(\\s*storeFile(?:\\s*[=:]|\\s+))(.*)$");

    private SigningSupport() {}

    /**
     * Normalizes raw Windows backslashes on {@code storeFile} property lines (using {@code =},
     * {@code :}, or whitespace separators) before passing the content to
     * {@link Properties#load(java.io.Reader)} so paths such as
     * {@code C:/Users/HP/Desktop/release.jks} written with backslashes on Windows do not lose
     * separators, turn into tab/newline control characters, or fail unicode parsing.
     */
    public static String normalizeStoreFileLine(String raw) {
        String[] lines = raw.split("\\r?\\n", -1);
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append('\n');
            String line = lines[i];
            Matcher matcher = STORE_FILE_LINE.matcher(line);
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String value = matcher.group(2).replace("\\\\", "/").replace('\\', '/');
                out.append(prefix).append(value);
            } else {
                out.append(line);
            }
        }
        return out.toString();
    }

    public static Properties parseKeystoreProperties(String raw) throws IOException {
        Properties props = new Properties();
        props.load(new StringReader(normalizeStoreFileLine(raw)));
        return props;
    }

    public static Properties loadKeystoreProperties(File file) throws IOException {
        if (!file.isFile()) return new Properties();
        return parseKeystoreProperties(Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }
}
