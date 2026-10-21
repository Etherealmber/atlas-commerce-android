import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class LegacySourceQuarantine {

    private LegacySourceQuarantine() {}

    /**
     * Moves Git-untracked files inside {@code srcDir} (such as leftover 1.0.4 sources like
     * {@code push/AtlasFirebaseMessagingService.kt} left behind after {@code git pull}) into a
     * persistent backup folder outside {@code build/} ({@code .atlas-1.0.4-quarantine/}) so
     * {@code :app:kspReleaseKotlin} never scans orphaned sources referencing removed SDKs.
     */
    public static List<String> quarantineUntracked(File rootDir, File srcDir, File quarantineDir) {
        if (!srcDir.isDirectory() || !new File(rootDir, ".git").exists()) {
            return Collections.emptyList();
        }
        String gitBin = findGitExecutable();
        if (gitBin == null) {
            return Collections.emptyList();
        }

        List<File> untracked = listUntrackedFiles(gitBin, rootDir, srcDir);
        if (untracked.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> moved = new ArrayList<>();
        Path srcPath = srcDir.toPath();
        for (File candidate : untracked) {
            if (!candidate.isFile()) continue;
            String relPath = srcPath.relativize(candidate.toPath()).toString().replace('\\', '/');
            Path target = quarantineDir.toPath().resolve(relPath);
            try {
                Files.createDirectories(target.getParent());
                Files.copy(candidate.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(candidate.toPath());
                moved.add(relPath);
            } catch (Exception ignored) {
                // Leave file untouched if locked; KSP will surface any error explicitly.
            }
        }

        if (!moved.isEmpty()) {
            pruneEmptyDirectories(srcPath);
        }
        return moved;
    }

    private static List<File> listUntrackedFiles(String gitBin, File rootDir, File srcDir) {
        try {
            String srcRel = rootDir.toPath().relativize(srcDir.toPath()).toString().replace('\\', '/');
            Process proc = new ProcessBuilder(
                gitBin, "ls-files", "--others", "--exclude-standard", "-z", "--", srcRel
            )
                .directory(rootDir)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            byte[] stdout = proc.getInputStream().readAllBytes();
            if (proc.waitFor() != 0) {
                return Collections.emptyList();
            }
            String decoded = new String(stdout, StandardCharsets.UTF_8);
            if (decoded.isEmpty()) {
                return Collections.emptyList();
            }
            List<File> files = new ArrayList<>();
            for (String entry : decoded.split("\0")) {
                if (!entry.isBlank()) {
                    files.add(new File(rootDir, entry));
                }
            }
            return files;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static void pruneEmptyDirectories(Path srcRoot) {
        try (Stream<Path> walk = Files.walk(srcRoot)) {
            walk.sorted(Comparator.reverseOrder())
                .filter(p -> !p.equals(srcRoot) && Files.isDirectory(p))
                .forEach(dir -> {
                    try (Stream<Path> entries = Files.list(dir)) {
                        if (entries.findAny().isEmpty()) {
                            Files.deleteIfExists(dir);
                        }
                    } catch (Exception ignored) {
                    }
                });
        } catch (Exception ignored) {
        }
    }

    private static String findGitExecutable() {
        List<String> candidates = new ArrayList<>();
        candidates.add("git");
        candidates.add("C:/Program Files/Git/cmd/git.exe");
        candidates.add("C:/Program Files (x86)/Git/cmd/git.exe");
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            candidates.add(localAppData + "/Programs/Git/cmd/git.exe");
            File ghDesktop = new File(localAppData, "GitHubDesktop");
            File[] children = ghDesktop.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory() && child.getName().startsWith("app-")) {
                        candidates.add(new File(child, "resources/app/git/cmd/git.exe").getPath());
                    }
                }
            }
        }
        for (String bin : candidates) {
            try {
                Process p = new ProcessBuilder(bin, "--version")
                    .redirectErrorStream(true)
                    .start();
                p.getInputStream().readAllBytes();
                if (p.waitFor() == 0) {
                    return bin;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
