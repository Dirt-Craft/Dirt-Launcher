package net.dirtcraft.dirtlauncher.game.installation.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dirtcraft.dirtlauncher.Data.Config;
import net.dirtcraft.dirtlauncher.game.installation.ProgressContainer;
import net.dirtcraft.dirtlauncher.game.installation.tasks.IInstallationTask;
import net.dirtcraft.dirtlauncher.gui.home.sidebar.Pack;
import net.dirtcraft.dirtlauncher.utils.FileUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UpdateInstancesManifestTask implements IInstallationTask {

    private final Pack pack;

    public UpdateInstancesManifestTask(Pack pack) {
        this.pack = pack;
    }

    @Override
    public int getNumberSteps() {
        return 1;
    }

    @Override
    public void executeTask(ExecutorService threadService, ProgressContainer progressContainer, Config config) {
        // Update Progress
        progressContainer.setProgressText("Updating Instances Manifest");
        progressContainer.setNumMinorSteps(2);

        JsonObject instanceManifest = FileUtils.readJsonFromFile(config.getDirectoryManifest(config.getInstancesDirectory()));
        JsonArray packsArray = instanceManifest.getAsJsonArray("packs");

        // Delete Old Entries
        Iterator<JsonElement> jsonIterator = packsArray.iterator();
        while(jsonIterator.hasNext()) {
            if(jsonIterator.next().getAsJsonObject().get("name").getAsString().equals(pack.getName())) jsonIterator.remove();
        }
        progressContainer.completeMinorStep();

        // Update Instances Manifest
        JsonObject packJson = new JsonObject();
        packJson.addProperty("name", pack.getName());
        packJson.addProperty("version", pack.getVersion());
        packJson.addProperty("gameVersion", pack.getGameVersion());
        packJson.addProperty("forgeVersion", pack.getForgeVersion());
        packsArray.add(packJson);
        FileUtils.writeJsonToFile(new File(config.getDirectoryManifest(config.getInstancesDirectory()).getPath()), instanceManifest);

        progressContainer.completeMinorStep();
        progressContainer.completeMajorStep();
    }
}
