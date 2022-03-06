package net.dirtcraft.dirtlauncher.lib.data.tasks;

import net.dirtcraft.dirtlauncher.lib.DirtLib;
import net.dirtcraft.dirtlauncher.lib.config.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class DownloadTask extends FileTask {
    protected URL src;

    public DownloadTask(URL src, File destination) {
        this(src, destination, -0, null);
    }

    public DownloadTask(URL src, File destination, long size) {
        this(src, destination, size, null);
    }

    public DownloadTask(URL src, File destination, String sha1) {
        this(src, destination, -0, sha1);
    }

    public DownloadTask(URL src, File destination, long size, String sha1) {
        super(destination, size, sha1);
        this.src = src;
    }

    public URL getSrc(){
        return src;
    }

    @Override
    public InputStream openSource() throws IOException {
        return src.openStream();
    }

    @Override
    public CompletableFuture<?> prepare() {
        if (this.completion > 0) return Constants.COMPLETED_FUTURE;
        return CompletableFuture.runAsync(()->{
            try {
                this.completion = src.openConnection().getContentLengthLong();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, DirtLib.THREAD_POOL);
    }

    @Override
    public String getType() {
        return "Downloading";
    }
}