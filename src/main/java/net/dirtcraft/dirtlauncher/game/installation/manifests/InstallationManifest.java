package net.dirtcraft.dirtlauncher.game.installation.manifests;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import net.dirtcraft.dirtlauncher.configuration.ConfigBase;
import net.dirtcraft.dirtlauncher.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public abstract class InstallationManifest<T> extends ConfigBase<ArrayList<T>> {

    public InstallationManifest(File path, TypeToken<ArrayList<T>> type){
        super(path, type);
        load();
    }

    public ListIterator<T> listIterator(){
        return configBase.listIterator();
    }

    public Stream<T> stream(){
        return configBase.stream();
    }

    @Override
    public void load(){
        configBase = FileUtils.parseJson(path, type, this::migrate).orElse(new ArrayList<>());
    }

    protected abstract ArrayList<T> migrate(JsonObject jsonObject);

}