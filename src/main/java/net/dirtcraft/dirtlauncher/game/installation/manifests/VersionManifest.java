package net.dirtcraft.dirtlauncher.game.installation.manifests;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dirtcraft.dirtlauncher.Main;
import net.dirtcraft.dirtlauncher.configuration.Manifests;
import net.dirtcraft.dirtlauncher.logging.Logger;
import net.dirtcraft.dirtlauncher.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class VersionManifest extends InstallationManifest<Map<String, VersionManifest.Entry>> {
    final Path parent;
    @SuppressWarnings("UnstableApiUsage")
    public VersionManifest() {
        super(Main.getConfig().getDirectoryManifest(Main.getConfig().getVersionsDirectory()), new TypeToken<Map<String, Entry>>(){}, HashMap::new);
        parent = path.getParentFile().toPath();
        CompletableFuture.runAsync(this::load);
    }

    @Override
    protected Map<String, Entry> migrate(JsonObject jsonObject) {
        Map<String, Entry> map = new HashMap<>();
        try {
            StreamSupport.stream(jsonObject.getAsJsonArray("versions").spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .forEach(versionManifest -> {
                        final String version = versionManifest.get("version").getAsString();
                        final Entry entry = new Entry(version);
                        final Path libDir = entry.getLibsFolder();
                        Arrays.stream(versionManifest.get("classpathLibraries").getAsString().split(";"))
                                .map(Paths::get)
                                .map(libDir::relativize)
                                .map(Path::toString)
                                .forEach(entry.libraries::add);
                        entry.libraries.sort(String::compareTo);
                        map.put(version, entry);
                    });
        } catch (Exception e){
            Logger.INSTANCE.error(e);
        }
        return map;
    }

    public Optional<Entry> get(String version){
        if (!isInstalled(version)) return Optional.empty();
        else return Optional.of(configBase.get(version));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Entry create(String version) throws IOException {
            Entry entry = new Entry(version);
            configBase.put(version, entry);
            FileUtils.deleteDirectory(entry.getVersionFolder().toFile());
            entry.getNativesFolder().toFile().mkdirs();
            entry.getLibsFolder().toFile().mkdirs();
            entry.getVersionFolder().toFile().mkdirs();
            return entry;
    }

    public boolean isInstalled(String minecraftVersion) {
        try {
            return configBase.containsKey(minecraftVersion);
        } catch (NullPointerException exception) {
            exception.printStackTrace();
            return true;
        }
    }

    public static class Entry {
        final int manifestVersion = 1;
        final String gameVersion;
        final ArrayList<String> libraries;
        public Entry(String version){
            this.gameVersion = version;
            libraries = new ArrayList<>();
        }

        public String getLibs(){
            final Path libDir = getLibsFolder();
            return libraries.stream()
                    .map(libDir::resolve)
                    .map(Path::toString)
                    .collect(Collectors.joining(";"));
        }

        public void addLibs(Collection<File> files){
            final Path libDir = getLibsFolder();
            files.stream()
                    .map(File::toPath)
                    .map(libDir::relativize)
                    .map(Path::toString)
                    .forEach(libraries::add);
            libraries.sort(String::compareTo);
        }

        public Path getLibsFolder(){
            return getVersionFolder().resolve("libraries");
        }

        public Path getNativesFolder(){
            return getVersionFolder().resolve("natives");
        }

        public File getVersionManifestFile(){
            return new File(getVersionFolder().toFile(), gameVersion + ".json");
        }

        public File getVersionJarFile(){
            return new File(getVersionFolder().toFile(), gameVersion + ".jar");
        }

        public Path getVersionFolder(){
            return getMain().parent.resolve(gameVersion);
        }

        public void saveAsync(){
            getMain().saveAsync();
        }

        private VersionManifest getMain(){
            return Manifests.VERSION;
        }
    }

}
