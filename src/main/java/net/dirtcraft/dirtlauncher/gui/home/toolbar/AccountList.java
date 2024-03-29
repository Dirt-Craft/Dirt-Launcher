package net.dirtcraft.dirtlauncher.gui.home.toolbar;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.dirtcraft.dirtlauncher.Main;
import net.dirtcraft.dirtlauncher.configuration.Constants;
import net.dirtcraft.dirtlauncher.game.authentification.Account;
import net.dirtcraft.dirtlauncher.utils.MiscUtils;

import java.util.Set;

final class AccountList extends Stage {
    private final AccountList instance = this;
    private final ScrollPane scrollPane;
    AccountList(){
        final Set<Account> sessions = Main.getAccounts().getAltAccounts();
        double vBoxSize = (sessions.size() + 2) * (59+5) + 5;
        vBoxSize = vBoxSize > 450 ? 450 : vBoxSize;
        final VBox backing = new VBox();
        backing.setBackground(Background.EMPTY);
        backing.setAlignment(Pos.TOP_CENTER);
        backing.getStyleClass().add(Constants.CSS_CLASS_VBOX);
        backing.setMinHeight(vBoxSize);

        final FlowPane title = new FlowPane();
        title.getChildren().add(new Text("Accounts"));
        title.getChildren().get(0).setTranslateY(5);
        title.setAlignment(Pos.CENTER);
        title.getStyleClass().add(Constants.CSS_CLASS_TITLE);

        scrollPane = new ScrollPane();
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setMinHeight(vBoxSize);
        scrollPane.setMaxHeight(vBoxSize);
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(backing);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);

        final AnchorPane root = new AnchorPane();
        AnchorPane.setTopAnchor(title, 000d);
        AnchorPane.setLeftAnchor(title, 010d);
        AnchorPane.setRightAnchor(title, 010d);
        AnchorPane.setBottomAnchor(title, vBoxSize + 20);
        AnchorPane.setTopAnchor(scrollPane, 30d);
        AnchorPane.setLeftAnchor(scrollPane, 10d);
        AnchorPane.setRightAnchor(scrollPane, 10d);
        AnchorPane.setBottomAnchor(scrollPane, 5d);
        root.setMinHeight(vBoxSize + 50);
        root.setMaxHeight(vBoxSize + 50);
        root.setBackground(Background.EMPTY);
        root.getChildren().addAll(title, scrollPane);
        root.getStylesheets().add(MiscUtils.getCssPath(Constants.JAR_CSS_FXML, "Accounts", "Global.css"));

        final Scene scene = new Scene(root, 292, vBoxSize + 50);
        scene.setFill(Paint.valueOf("transparent"));
        setScene(scene);
        initModality(Modality.WINDOW_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Accounts");

        final ObservableList<Node> contents = backing.getChildren();
        sessions.forEach(session -> contents.add(0, new AccountButton(session)));
        contents.add(new AddMicroAccountButton());


        focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (! isNowFocused) {
                hide();
            }
        });

    }

    private class AccountButton extends Button {
        private final Account session;
        private double lastDragY;
        AccountButton(Account session){
            this.session = session;
            setCursor(Cursor.HAND);
            setText(session.getAlias());
            setOnMouseDragged(event->{
                if (event.isPrimaryButtonDown()) {
                    double change = (lastDragY - event.getY()) / scrollPane.getHeight();
                    scrollPane.setVvalue(scrollPane.getVvalue() + change);
                    lastDragY = change;
                }
            });
        }
        @Override
        public void fire() {
            Main.getAccounts().setSelectedAccount(session);
            instance.close();
            Main.getHome().getLoginBar().setInputs();
        }
    }

    private class AddMicroAccountButton extends Button {
        private double lastDragY;

        AddMicroAccountButton() {
            setCursor(Cursor.HAND);
            setText("Add Microsoft Account");
            setOnMouseDragged(event -> {
                if (event.isPrimaryButtonDown()) {
                    double change = (lastDragY - event.getY()) / scrollPane.getHeight();
                    scrollPane.setVvalue(scrollPane.getVvalue() + change);
                    lastDragY = change;
                }
            });
        }

        @Override
        public void fire() {
            Main.getAccounts().login();
        }
    }
}
