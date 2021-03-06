package com.afollestad.polarupgradetool;

import com.afollestad.polarupgradetool.jfx.UICallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Aidan Follestad (afollestad)
 */
class MainBase {

    private final static String ARCHIVE_URL = "https://github.com/afollestad/polar-dashboard/archive/master.zip";
    public final static int BUFFER_SIZE = 2048;

    protected static File EXTRACTED_ZIP_ROOT;
    protected static File CURRENT_DIR;
    private final static String ARCHIVE_ROOT = File.separator + "polar-dashboard-master";

    public static String cleanupPath(String from) {
        if (from.startsWith(CURRENT_DIR.getAbsolutePath())) {
            from = from.substring(CURRENT_DIR.getAbsolutePath().length());
        } else if (from.startsWith(EXTRACTED_ZIP_ROOT.getAbsolutePath())) {
            from = from.substring(EXTRACTED_ZIP_ROOT.getAbsolutePath().length());
        }
        return from;
    }

    public static void LOG(String msg, Object... args) {
        if (args != null)
            msg = String.format(msg, args);
        System.out.println(msg);
    }

    public static String PROGRESS(String label, long read, long total) {
        final int percent = (int) Math.ceil(((double) read / (double) total) * 100d);
        StringBuilder sb = new StringBuilder(13);
        sb.append('\r');
        if (label != null) {
            sb.append(label);
            sb.append("  ");
        }
        sb.append('[');
        final int numOfEqual = percent / 10;
        final int numOfSpace = 10 - numOfEqual;
        for (int i = 0; i < numOfEqual; i++) sb.append('=');
        for (int i = 0; i < numOfSpace; i++) sb.append(' ');
        sb.append("]");
        sb.append("   ");
        sb.append(Util.round(percent));
        sb.append("%  ");
        sb.append(Util.readableFileSizeMB(read));
        sb.append('/');
        sb.append(Util.readableFileSizeMB(total));
        System.out.print(sb.toString());
        return String.format("%s/%s (%s%%)", Util.readableFileSizeMB(read),
                Util.readableFileSizeMB(total), Util.round(percent));
    }

    public static int TRIES = 0;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected static boolean downloadArchive(UICallback uiCallback) {
        InputStream is = null;
        FileOutputStream os = null;

        if (TRIES == 0) {
            LOG("[INFO]: Contacting GitHub...");
            uiCallback.onStatusUpdate("Contacting GitHub...");
        }

        try {
            URL url = new URL(ARCHIVE_URL);
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();

            long contentLength;
            try {
                final String contentLengthStr = conn.getHeaderField("Content-Length");
                if (contentLengthStr == null || contentLengthStr.trim().isEmpty()) {
                    if (TRIES > 0) {
                        LOG("[ERROR]: No Content-Length header was returned by GitHub. Try running this app again.");
                        uiCallback.onErrorOccurred("GitHub did not report a Content-Length, please try again.");
                        return false;
                    }
                    TRIES++;
                    Thread.sleep(2000);
                    return downloadArchive(uiCallback);
                }
                contentLength = Long.parseLong(contentLengthStr);
            } catch (Throwable e) {
                e.printStackTrace();
                LOG("[ERROR]: Failed to get the size of Polar's latest code archive. Please try running this app again.", e.getMessage());
                uiCallback.onArchiveDownloadFailed("Failed to get the size of Polar's latest code archive. Please try running this app again.");
                return false;
            }

            final File destZip = new File(CURRENT_DIR, "PolarLatest.zip");
            if (destZip.exists()) destZip.delete();
            os = new FileOutputStream(destZip);

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            int totalRead = 0;

            LOG("[INFO]: Downloading a ZIP of Polar's latest code (%s)...", FileUtil.readableFileSize(contentLength));
            uiCallback.onArchiveDownloadStarted(FileUtil.readableFileSize(contentLength));

            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalRead += read;
                final String progressStr = PROGRESS(null, totalRead, contentLength);
                uiCallback.onArchiveDownloadProgress(progressStr);
            }

            PROGRESS(null, contentLength, contentLength);
            System.out.println();
            LOG("[INFO]: Download complete!");
            uiCallback.onArchiveDownloadSuccess();
            os.flush();

            Util.closeQuietely(is);
            Util.closeQuietely(os);

            EXTRACTED_ZIP_ROOT = new File(CURRENT_DIR, "PolarLatest");
            if (EXTRACTED_ZIP_ROOT.exists()) {
                final int removedCount = FileUtil.wipe(EXTRACTED_ZIP_ROOT);
                LOG("[INFO]: Removed %d files/folders from %s.", removedCount,
                        Main.cleanupPath(EXTRACTED_ZIP_ROOT.getAbsolutePath()));
                uiCallback.onStatusUpdate(String.format("Removed %d files/folders from %s.", removedCount, Main.cleanupPath(EXTRACTED_ZIP_ROOT.getAbsolutePath())));
            }

            LOG("[INFO]: Extracting %s to %s...", cleanupPath(destZip.getAbsolutePath()),
                    cleanupPath(EXTRACTED_ZIP_ROOT.getAbsolutePath()));
            uiCallback.onStatusUpdate(String.format("Extracting %s to %s...",
                    cleanupPath(destZip.getAbsolutePath()), cleanupPath(EXTRACTED_ZIP_ROOT.getAbsolutePath())));
            UnzipUtil.unzip(destZip.getAbsolutePath(), EXTRACTED_ZIP_ROOT.getAbsolutePath());
            LOG("[INFO]: Extraction complete!\n");
            uiCallback.onStatusUpdate("Extraction complete!");
            destZip.delete();
            EXTRACTED_ZIP_ROOT = new File(EXTRACTED_ZIP_ROOT, ARCHIVE_ROOT);
        } catch (Exception e) {
            LOG("[ERROR]: An error occurred during download or extraction: %s\n", e.getMessage());
            uiCallback.onErrorOccurred("An error occurred during download or extraction: " + e.getMessage());
            return false;
        } finally {
            Util.closeQuietely(is);
            Util.closeQuietely(os);
        }
        return true;
    }
}
