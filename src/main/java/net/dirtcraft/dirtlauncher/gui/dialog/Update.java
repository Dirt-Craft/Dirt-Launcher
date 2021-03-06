package net.dirtcraft.dirtlauncher.gui.dialog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.dirtcraft.dirtlauncher.Main;
import net.dirtcraft.dirtlauncher.configuration.Constants;
import net.dirtcraft.dirtlauncher.logging.Logger;
import net.dirtcraft.dirtlauncher.utils.MiscUtils;
import net.dirtcraft.dirtlauncher.utils.WebUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Update {

    private static Update instance;

    private static Stage stage;

    @FXML
    private Button cancel;

    @FXML
    private Button download;

    @FXML
    private void initialize() {
        instance = this;
    }

    @FXML
    private void onClick(MouseEvent event) {
        Object source = event.getSource();
        if (source == download) MiscUtils.updateLauncher();
        else if (source == cancel) stage.close();
    }

    public static Update getInstance() {
        return instance;
    }

    public static void showStage() {
        try {
            Parent root = FXMLLoader.load(MiscUtils.getResourceURL(Constants.JAR_SCENES, "update.fxml"));
            root.getStylesheets().add("https://fonts.gstatic.com/s/russoone/v7/Z9XUDmZRWg6M1LvRYsHOz8mJvLuL9A.woff2");

            Stage stage = new Stage();
            stage.initOwner(Main.getHome().getStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);

            stage.setTitle("Version " + WebUtils.getLatestVersion() + " available");
            stage.getIcons().setAll(MiscUtils.getImage(Constants.JAR_ICONS, "update.png"));
            Scene scene = new Scene(root, 400, 200);
            stage.setScene(scene);
            stage.setResizable(false);

            stage.show();

            Update.stage = stage;

        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }

    public static boolean hasUpdate() throws IOException {
        //noinspection ConstantConditions //literally gets replaced in production.
        if (Constants.LAUNCHER_VERSION.equals("@" + "VERSION" + "@")) return false;
        final String webVersionString = WebUtils.getLatestVersion();
        List<Integer> webVersion = Arrays.stream(webVersionString.split("\\."))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        List<Integer> localVersion = Arrays.stream(Constants.LAUNCHER_VERSION.split("\\."))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        Logger.INSTANCE.debug("Installed Version: " + Constants.LAUNCHER_VERSION);
        Logger.INSTANCE.debug("Latest Version: " + webVersionString);

        if (localVersion.size() != webVersion.size()) return true;
        for (int i = 0; i < webVersion.size(); i++) if (webVersion.get(i) > localVersion.get(i)) return true;

        return false;
    }

    public static void checkForUpdates(){
        try {
            if (Main.getOptions().contains("-update") && Update.hasUpdate()) MiscUtils.updateLauncher();
            if (Update.hasUpdate()) Platform.runLater(Update::showStage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
