package net.dirtcraft.dirtlauncher.Controllers;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import javax.annotation.Nullable;
import java.util.Optional;

public class Install {

    private static Install instance = null;

    private static Stage stage = null;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private TextFlow notificationText;

    @FXML
    private ProgressBar loadingBar;

    @FXML
    private ProgressBar bottomBar;

    @FXML
    private FlowPane buttonPane;

    @FXML
    private Button playButton;

    @FXML
    private void initialize() {
        instance = this;
        playButton.setCursor(Cursor.HAND);

    }

    @FXML
    private void onButtonClick(MouseEvent event) {
        if (!buttonPane.isVisible()) return;
        buttonPane.setVisible(false);
        getStage().ifPresent(Stage::close);

        Home.getInstance().getLoginBar().getActionButton().launchPack(Home.getInstance().getLoginBar().login(), Home.getInstance().getLoginBar().getActivePackCell().get().getPack());
    }

    public void setStage(Stage stage) {
        Install.stage = stage;
    }

    @Nullable
    public Stage getStageUnsafe() {
        return stage;
    }

    public static Optional<Stage> getStage() {
        if (stage == null || instance == null) return Optional.empty();
        return Optional.of(stage);
    }

    public FlowPane getButtonPane() {
        return buttonPane;
    }

    public AnchorPane getAnchorPane() {
        return anchorPane;
    }

    public ProgressBar getLoadingBar() {
        return loadingBar;
    }

    public ProgressBar getBottomBar() {
        return bottomBar;
    }

    public static Optional<Install> getInstance() {
        if (instance == null) return Optional.empty();
        return Optional.of(instance);
    }

    public TextFlow getNotificationText() {
        return notificationText;
    }
}
