package net.dirtcraft.dirtlauncher;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.dirtcraft.dirtlauncher.configuration.ConfigurationManager;
import net.dirtcraft.dirtlauncher.configuration.Constants;
import net.dirtcraft.dirtlauncher.data.serializers.MultiMapAdapter;
import net.dirtcraft.dirtlauncher.game.authentification.AccountManager;
import net.dirtcraft.dirtlauncher.game.authentification.Account;
import net.dirtcraft.dirtlauncher.gui.components.DiscordPresence;
import net.dirtcraft.dirtlauncher.gui.dialog.Update;
import net.dirtcraft.dirtlauncher.gui.home.Home;
import net.dirtcraft.dirtlauncher.gui.home.toolbar.Settings;
import net.dirtcraft.dirtlauncher.logging.Logger;
import net.dirtcraft.dirtlauncher.utils.forge.Artifact;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

public class Main extends Application {
    private static final long x = System.currentTimeMillis();
    public static final Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
    public static ThreadPoolExecutor threadPool;
    private static CompletableFuture<ConfigurationManager> config = null;
    private static CompletableFuture<AccountManager> accounts = null;
    private static CompletableFuture<Settings> settingsMenu = null;
    private static CompletableFuture<Home> home = null;
    private static Path launcherDirectory;
    private static List<String> options;
    public static Gson gson;
    public static final int[] JREVersion = Stream.of(System.getProperty("java.version")
            .replaceAll("[^\\d.]", "")
            .split("\\.")).mapToInt(Integer::parseInt)
            .toArray();

    public static void main(String[] args) {
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(24);
        //noinspection rawtypes
        gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Multimap.class, new MultiMapAdapter())
                .registerTypeAdapter(Artifact.class, new Artifact.Adapter())
                .create();
        options = Arrays.asList(args);
        if (options.contains("-postUpdate")) System.out.println("\n\n");
        launcherDirectory = getLauncherDirectory(options);
        home = CompletableFuture
                .supplyAsync(Home::new, threadPool)
                .whenComplete(Main::announceCompletion);
        accounts = CompletableFuture
                .supplyAsync(()->new AccountManager(launcherDirectory), threadPool)
                .whenComplete(Main::announceCompletion);
        config = CompletableFuture
                .supplyAsync(()->new ConfigurationManager(launcherDirectory, options), threadPool)
                .whenComplete(Main::announceCompletion);
        settingsMenu = config
                .thenApply(Settings::new)
                .whenComplete(Main::announceCompletion);
        settingsMenu.thenRun(Update::checkForUpdates);
        CompletableFuture.runAsync(Main::postUpdateCleanup, threadPool);
        DiscordPresence.initPresence();

        launch(args);
    }

    public static ExecutorService getIOExecutor(){
        return threadPool;
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Platform.setImplicitExit(false);
        Home home = Main.home.join();
        home.getStage().show();
        home.update();
        Logger.INSTANCE.info("Launching @ " + (System.currentTimeMillis() - x) + "ms");
    }

    private static <T> void announceCompletion(T t, Throwable e){
        if (e != null) e.printStackTrace();
        if (!Constants.VERBOSE) return;
        final long ms = System.currentTimeMillis() - x;
        final String clazz = t.getClass().getSimpleName();
        System.out.printf("%s initialized @ %sms%n", clazz, ms);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void postUpdateCleanup(){
        boolean updated = false;
        try {
            File currentDirectory = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            String[] duds = currentDirectory.list((dir, name) -> name.matches("(?i)^.*Dirt-Bootstrap-Updater-[a-zA-Z0-9]+\\.jar$"));
            long bootstraps = Arrays.stream(duds == null? new String[0] : duds).map(File::new).peek(File::delete).count();
            if (bootstraps > 0) updated = true;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (updated) Logger.getInstance().verbose("Successfully updated launcher to version " + Constants.LAUNCHER_VERSION + "!!!");
    }

    private static Path getLauncherDirectory(List<String> options){
        if (options.contains("-installed") || options.contains("-portable"))
            try {
                return Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            } catch (Exception e){ throw new Error(e); }
        else if (SystemUtils.IS_OS_WINDOWS)// If the host OS is windows, use AppData
            return Paths.get(System.getenv("AppData"), "DirtCraft", "DirtLauncher");
        else if (SystemUtils.IS_OS_MAC)// If the host OS is mac, use the user's Application Support directory
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "DirtCraft", "DirtLauncher");
        else return Paths.get(System.getProperty("user.home"), "DirtCraft", "DirtLauncher");
        //Logger.INSTANCE.debug("Block Start @ " + (System.currentTimeMillis() - x) + "ms");
    }

    public static AccountManager getAccounts() {
        return accounts.join();
    }

    public static Settings getSettingsMenu() {
        return settingsMenu.join();
    }

    public static Home getHome() {
        return home.join();
    }

    public static ConfigurationManager getConfig() {
        return config.join();
    }

    public static List<String> getOptions(){
        return options;
    }

    public static Optional<String> getOption(String key) {
        int idx = options.indexOf(key);
        if (idx < 0) return Optional.empty();
        return Optional.of(options.get(idx + 1));
    }

    public static boolean hasOption(String key) {
        return options.contains(key);
    }

    public static Path getLauncherDirectory(){
        return launcherDirectory;
    }
}
