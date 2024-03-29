package net.dirtcraft.dirtlauncher.configuration.manifests;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dirtcraft.dirtlauncher.configuration.ManifestBase;
import net.dirtcraft.dirtlauncher.data.Minecraft.JavaVersion;
import net.dirtcraft.dirtlauncher.game.installation.tasks.download.data.Result;
import net.dirtcraft.dirtlauncher.logging.Logger;
import net.dirtcraft.dirtlauncher.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class VersionManifest extends ManifestBase<Map<String, VersionManifest.Entry>> {
    @SuppressWarnings("UnstableApiUsage")
    public VersionManifest(Path dir) {
        super(dir, new TypeToken<Map<String, Entry>>(){}, HashMap::new);
        load();
        assert configBase != null;
    }

    @Override
    public void load(){
        super.load();
        configBase.values().forEach(entry->entry.setOuterReference(this));
    }

    @Override
    protected Map<String, Entry> migrate(JsonObject jsonObject) {
        Map<String, Entry> map = new HashMap<>();
        try {
            StreamSupport.stream(jsonObject.getAsJsonArray("versions").spliterator(), false)
                    .map(JsonElement::getAsJsonObject)
                    .forEach(versionManifest -> {
                        final JavaVersion javaVersion = JavaVersion.LEGACY;
                        final String version = versionManifest.get("version").getAsString();
                        final Entry entry = new Entry(version, javaVersion, this);
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
    public Entry create(String gameVersion, JavaVersion javaVersion) throws IOException {
            Entry entry = new Entry(gameVersion, javaVersion, this);
            configBase.put(gameVersion, entry);
            FileUtils.deleteDirectory(entry.getVersionFolder().toFile());
            entry.getNativesFolder().toFile().mkdirs();
            entry.getLibsFolder().toFile().mkdirs();
            entry.getVersionFolder().toFile().mkdirs();
            return entry;
    }

    public boolean isInstalled(String minecraftVersion) {
        final Entry entry = configBase.get(minecraftVersion);
        if (entry == null) return false;
        if (!entry.getVersionFolder().toFile().exists()){
            configBase.remove(minecraftVersion);
            saveAsync();
            return false;
        } else return true;
    }

    public static class Entry {
        private transient VersionManifest outerReference;
        final int manifestVersion = 1;
        final String gameVersion;
        final JavaVersion javaVersion;
        final ArrayList<String> libraries;
        public Entry(String gameVersion, JavaVersion javaVersion, VersionManifest outerReference){
            this.gameVersion = gameVersion;
            libraries = new ArrayList<>();
            this.javaVersion = javaVersion;
            this.outerReference = outerReference;
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

        public void addLibs(List<Result> files){
            List<File> downloaded = files.stream()
                    .map(Result::getFile)
                    .collect(Collectors.toList());

            addLibs(downloaded);
        }

        public JavaVersion getJavaVersion() {
            return javaVersion;
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
            return getOuterReference().directory.resolve(gameVersion);
        }

        public void saveAsync(){
            getOuterReference().saveAsync();
        }

        private VersionManifest getOuterReference(){
            return outerReference;
        }

        private void setOuterReference(VersionManifest instance){
            outerReference = instance;
        }
    }
}
