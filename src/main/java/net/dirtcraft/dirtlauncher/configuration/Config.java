package net.dirtcraft.dirtlauncher.configuration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.management.OperatingSystemMXBean;
import net.dirtcraft.dirtlauncher.configuration.manifests.ForgeManifest;
import net.dirtcraft.dirtlauncher.configuration.manifests.InstanceManifest;
import net.dirtcraft.dirtlauncher.configuration.manifests.VersionManifest;
import net.dirtcraft.dirtlauncher.utils.JsonUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public final class Config {
    private ForgeManifest forgeManifest;
    private VersionManifest versionManifest;
    private InstanceManifest instanceManifest;
    private final Path launcherDirectory;
    private final String defaultRuntime;
    private int minimumRam;
    private int maximumRam;
    private String javaArguments;
    private Path gameDirectory;

    public Config(Path launcherDirectory, List<String> options) {
        final String javaExecutable = SystemUtils.IS_OS_WINDOWS ? "javaw" : "java";
        if (options.contains("-installed") || options.contains("-useBundledRuntime")) {
            final Path runtimeDirectory = launcherDirectory.resolve("Runtime");
            defaultRuntime = runtimeDirectory
                    .resolve(options.contains("-x86") ? "jre8_x86" : "jre8_x64")
                    .resolve("bin")
                    .resolve(javaExecutable)
                    .toFile().getPath();
        } else {
            defaultRuntime = javaExecutable;
        }
        File configFile = launcherDirectory.resolve("configuration.json").toFile();
        this.launcherDirectory = launcherDirectory;
        JsonObject config = null;
        try (FileReader reader = new FileReader(configFile)) {
            JsonParser parser = new JsonParser();
            config = parser.parse(reader).getAsJsonObject();
        } catch (IOException ignored) {
        }
        if (configFile.exists() && config != null) {
            if (config.has("minimum-ram")) minimumRam = config.get("minimum-ram").getAsInt();
            else {
                final int value = getDefaultMinimumRam() * 1024;
                config.addProperty("minimum-ram", value);
                minimumRam = value;
            }
            if (config.has("maximum-ram")) maximumRam = config.get("maximum-ram").getAsInt();
            else {
                final int value = getDefaultRecommendedRam() * 1024;
                config.addProperty("maximum-ram", value);
                maximumRam = value;
            }
            if (config.has("java-arguments")) javaArguments = config.get("java-arguments").getAsString();
            else {
                final String value = Constants.DEFAULT_JAVA_ARGS;
                config.addProperty("java-arguments", value);
                javaArguments = value;
            }
            if (config.has("game-directory")) gameDirectory = Paths.get(config.get("game-directory").getAsString());
            else {
                final String value = launcherDirectory.toString();
                config.addProperty("game-directory", value);
                gameDirectory = launcherDirectory;
            }
        } else {
            minimumRam = getDefaultMinimumRam() * 1024;
            maximumRam = getDefaultRecommendedRam() * 1024;
            javaArguments = Constants.DEFAULT_JAVA_ARGS;
            gameDirectory = launcherDirectory;
            try {
                initGameDirectory();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
            saveSettings();
        }

        forgeManifest = new ForgeManifest(getForgeDirectory());
        versionManifest = new VersionManifest(getVersionsDirectory());
        instanceManifest = new InstanceManifest(getInstancesDirectory());
    }

    private int getDefaultRecommendedRam() {
        final long maxMemory = (((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize()) / 1024 / 1024;
        if (maxMemory > 10000) return 8;
        else if (maxMemory > 7000) return 6;
        else if (maxMemory > 5000) return 4;
        else if (maxMemory > 2000) return 3;
        else return 2;
    }

    private int getDefaultMinimumRam() {
        final long maxMemory = (((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize()) / 1024 / 1024;
        if (maxMemory > 5000) return 4;
        else if (maxMemory > 3000) return 3;
        else return 2;
    }

    private void initGameDirectory(){
        File assets = getAssetsDirectory().toFile();
        System.out.println(getGameDirectory().toFile().mkdirs()?"Successfully created":"Failed to create"+" game directory");
        System.out.println(assets.mkdirs()?"Successfully created":"Failed to create"+" assets directory.");
        // Ensure that the application folders are created
        if(!getDirectoryManifest(assets).exists()) {
            JsonObject emptyManifest = new JsonObject();
            emptyManifest.add("assets", new JsonArray());
            JsonUtils.writeJsonToFile(getDirectoryManifest(assets), emptyManifest);
        }
    }

    private void saveSettings(){
        File configFile = launcherDirectory.resolve("configuration.json").toFile();
        final JsonObject config = new JsonObject();
        config.addProperty("minimum-ram", minimumRam);
        config.addProperty("maximum-ram", maximumRam);
        config.addProperty("java-arguments", javaArguments);
        config.addProperty("game-directory", gameDirectory.toString());
        JsonUtils.writeJsonToFile(configFile, config);
    }

    public void updateSettings(int minimumRam, int maximumRam, String javaArguments, String gameDirectory){
        final boolean changedDir = !this.gameDirectory.toString().equals(gameDirectory);
        final File oldGameDir = getGameDirectory().toFile();
        final File oldInstanceDir = getInstancesDirectory().toFile();
        final File oldVersionDir = getVersionsDirectory().toFile();
        final File oldAssetsDir = getAssetsDirectory().toFile();
        final File oldForgeDir = getForgeDirectory().toFile();
        this.minimumRam = minimumRam;
        this.maximumRam = maximumRam;
        this.javaArguments = javaArguments;
        this.gameDirectory = Paths.get(gameDirectory);
        if (changedDir){
            try {
                org.apache.commons.io.FileUtils.moveDirectory(oldInstanceDir, getInstancesDirectory().toFile());
                org.apache.commons.io.FileUtils.moveDirectory(oldVersionDir, getVersionsDirectory().toFile());
                org.apache.commons.io.FileUtils.moveDirectory(oldAssetsDir, getAssetsDirectory().toFile());
                org.apache.commons.io.FileUtils.moveDirectory(oldForgeDir, getForgeDirectory().toFile());
            } catch (IOException e){
                e.printStackTrace();
            }
            System.out.println(oldGameDir.delete()?"Successfully deleted":"Failed to delete"+" "+oldGameDir);
            initGameDirectory();
        }
        saveSettings();
    }

    public ForgeManifest getForgeManifest(){
        return forgeManifest;
    }

    public VersionManifest getVersionManifest(){
        return versionManifest;
    }

    public InstanceManifest getInstanceManifest(){
        return instanceManifest;
    }

    public Path getLogDirectory() {
        return launcherDirectory.resolve("logs");
    }

    public Path getInstancesDirectory() {
        return gameDirectory.resolve("instances");
    }

    public Path getVersionsDirectory() {
        return gameDirectory.resolve("versions");
    }

    public Path getAssetsDirectory() {
        return gameDirectory.resolve("assets");
    }

    public Path getForgeDirectory() {
        return gameDirectory.resolve("forge");
    }

    public File getDirectoryManifest(File directory) {
        return directory.toPath().resolve("manifest.json").toFile();
    }

    public int getMinimumRam() {
        return minimumRam;
    }

    public int getMaximumRam() {
        return maximumRam;
    }

    public String getJavaArguments() {
        return javaArguments;
    }

    public Path getGameDirectory() {
        return gameDirectory;
    }

    public String getDefaultRuntime() {
        return defaultRuntime;
    }
}
