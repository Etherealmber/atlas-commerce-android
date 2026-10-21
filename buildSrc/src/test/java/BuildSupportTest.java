import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class BuildSupportTest {

    @Test
    public void handlesWindowsBackslashPathsWithoutUnicodeOrControlEscapes() throws Exception {
        String upperU = String.join("\n",
            "storeFile=C:\\Users\\HP\\Desktop\\atlas-release.jks",
            "storePassword=secret\\pass",
            "keyAlias=atlas",
            "keyPassword=secret456"
        );
        Properties p1 = SigningSupport.parseKeystoreProperties(upperU);
        assertEquals("C:/Users/HP/Desktop/atlas-release.jks", p1.getProperty("storeFile"));
        assertEquals("atlas", p1.getProperty("keyAlias"));

        String lowerEscapes = "storeFile=C:\\users\\hp\\temp\\new\\release.jks\nkeyAlias=atlas";
        Properties p2 = SigningSupport.parseKeystoreProperties(lowerEscapes);
        assertEquals("C:/users/hp/temp/new/release.jks", p2.getProperty("storeFile"));

        String colonSep = "storeFile: C:\\Users\\HP\\Desktop\\release.jks\nkeyAlias: atlas";
        Properties pColon = SigningSupport.parseKeystoreProperties(colonSep);
        assertEquals("C:/Users/HP/Desktop/release.jks", pColon.getProperty("storeFile"));

        String spaceSep = "storeFile C:\\Users\\HP\\Desktop\\release.jks\nkeyAlias atlas";
        Properties pSpace = SigningSupport.parseKeystoreProperties(spaceSep);
        assertEquals("C:/Users/HP/Desktop/release.jks", pSpace.getProperty("storeFile"));

        String relative = "storeFile=atlas-release.jks\nkeyAlias=atlas";
        Properties p3 = SigningSupport.parseKeystoreProperties(relative);
        assertEquals("atlas-release.jks", p3.getProperty("storeFile"));
    }

    @Test
    public void quarantinesUntrackedFilesWhilePreservingTrackedAndStagedFiles() throws Exception {
        Path repo = Files.createTempDirectory("atlas-quarantine-test");
        runGit(repo, "init");

        Path srcDir = repo.resolve("app/src");
        Path tracked = srcDir.resolve("main/java/ma/etherealmber/atlascommerce/MainActivity.kt");
        Path staged = srcDir.resolve("main/java/ma/etherealmber/atlascommerce/NewFeature.kt");
        Path untrackedPush = srcDir.resolve(
            "main/java/ma/etherealmber/atlascommerce/push/AtlasFirebaseMessagingService.kt"
        );

        Files.createDirectories(tracked.getParent());
        Files.createDirectories(untrackedPush.getParent());
        Files.writeString(tracked, "class MainActivity\n", StandardCharsets.UTF_8);
        Files.writeString(staged, "class NewFeature\n", StandardCharsets.UTF_8);
        Files.writeString(untrackedPush, "class AtlasFirebaseMessagingService\n", StandardCharsets.UTF_8);

        runGit(repo, "add", "app/src/main/java/ma/etherealmber/atlascommerce/MainActivity.kt");
        runGit(repo, "-c", "user.name=test", "-c", "user.email=test@example.com", "commit", "-m", "init");
        runGit(repo, "add", "app/src/main/java/ma/etherealmber/atlascommerce/NewFeature.kt");

        Path quarantineDir = repo.resolve(".atlas-1.0.4-quarantine");
        List<String> moved = LegacySourceQuarantine.quarantineUntracked(
            repo.toFile(), srcDir.toFile(), quarantineDir.toFile()
        );

        assertEquals(
            List.of("main/java/ma/etherealmber/atlascommerce/push/AtlasFirebaseMessagingService.kt"),
            moved
        );
        assertTrue(Files.exists(tracked));
        assertTrue(Files.exists(staged));
        assertFalse(Files.exists(untrackedPush));
        assertFalse(Files.exists(untrackedPush.getParent()));
        assertTrue(Files.exists(quarantineDir.resolve(
            "main/java/ma/etherealmber/atlascommerce/push/AtlasFirebaseMessagingService.kt"
        )));
    }

    private static void runGit(Path cwd, String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "git";
        System.arraycopy(args, 0, cmd, 1, args.length);
        Process p = new ProcessBuilder(cmd).directory(cwd.toFile()).redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();
        assertEquals("git failed: " + output, 0, exit);
    }
}
