package com.aibin.bluetoothkeyboard.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.aibin.bluetoothkeyboard.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UpdateManager handles checking for application updates from GitHub Releases,
 * downloading APK assets with redirect support, and prompting the Android package installer.
 */
public class UpdateManager {

    public static final String GITHUB_OWNER = "aibinjmca2527-dotcom";
    public static final String GITHUB_REPO = "BT-Laptop-Keyboard";
    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases/latest";

    public static final int REQUEST_CODE_INSTALL_PERMISSION = 7123;

    private static UpdateManager instance;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private File pendingInstallFile = null;
    private AlertDialog activeDialog = null;

    public static synchronized UpdateManager getInstance(Context context) {
        if (instance == null) {
            instance = new UpdateManager(context.getApplicationContext());
        }
        return instance;
    }

    private UpdateManager(Context context) {
        this.appContext = context;
    }

    public static class UpdateInfo {
        public final String latestVersion;
        public final String releaseTitle;
        public final String changelog;
        public final String apkDownloadUrl;
        public final long apkSizeBytes;
        public final String releasePageUrl;

        public UpdateInfo(String latestVersion, String releaseTitle, String changelog,
                          String apkDownloadUrl, long apkSizeBytes, String releasePageUrl) {
            this.latestVersion = latestVersion;
            this.releaseTitle = releaseTitle;
            this.changelog = changelog;
            this.apkDownloadUrl = apkDownloadUrl;
            this.apkSizeBytes = apkSizeBytes;
            this.releasePageUrl = releasePageUrl;
        }
    }

    public interface UpdateCheckCallback {
        void onUpdateAvailable(UpdateInfo info);
        void onAlreadyLatest(String currentVersion);
        void onNoReleases();
        void onError(String message);
    }

    /**
     * Check for app updates asynchronously.
     *
     * @param activity current active activity for dialog context
     * @param isManual true if triggered by user (show toasts/feedback), false if silent on app launch
     */
    public void checkForUpdates(Activity activity, boolean isManual) {
        checkForUpdates(activity, isManual, null);
    }

    public void checkForUpdates(Activity activity, boolean isManual, UpdateCheckCallback customCallback) {
        if (isManual) {
            Toast.makeText(activity, R.string.update_checking, Toast.LENGTH_SHORT).show();
        }

        executor.execute(() -> {
            try {
                URL url = new URL(GITHUB_RELEASES_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "BT-Laptop-Keyboard-Android");

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    mainHandler.post(() -> {
                        if (customCallback != null) {
                            customCallback.onNoReleases();
                        } else if (isManual && !activity.isFinishing()) {
                            showInfoDialog(activity, "GitHub Releases",
                                    "No releases published on GitHub repository (" + GITHUB_OWNER + "/" + GITHUB_REPO + ") yet.\n\nOnce a release is added with an APK asset, updates will appear here automatically.");
                        }
                    });
                    return;
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("GitHub server returned HTTP " + responseCode);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                JSONObject releaseJson = new JSONObject(sb.toString());
                String tagName = releaseJson.optString("tag_name", "");
                String releaseTitle = releaseJson.optString("name", tagName);
                String body = releaseJson.optString("body", "");
                String releasePageUrl = releaseJson.optString("html_url", "https://github.com/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases");

                String apkDownloadUrl = null;
                long apkSizeBytes = 0;

                JSONArray assets = releaseJson.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.optString("name", "");
                        if (name.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", null);
                            apkSizeBytes = asset.optLong("size", 0);
                            break;
                        }
                    }
                }

                String currentVersion = getCurrentVersionName();
                String cleanRemoteVersion = cleanVersionString(tagName);

                boolean isNewer = isVersionNewer(cleanRemoteVersion, currentVersion);

                UpdateInfo info = new UpdateInfo(
                        cleanRemoteVersion,
                        releaseTitle,
                        body.isEmpty() ? "• Bug fixes and stability improvements." : body,
                        apkDownloadUrl,
                        apkSizeBytes,
                        releasePageUrl
                );

                mainHandler.post(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    if (isNewer) {
                        if (customCallback != null) {
                            customCallback.onUpdateAvailable(info);
                        } else {
                            showUpdateAvailableDialog(activity, info, currentVersion);
                        }
                    } else {
                        if (customCallback != null) {
                            customCallback.onAlreadyLatest(currentVersion);
                        } else if (isManual) {
                            Toast.makeText(activity, activity.getString(R.string.update_latest_version, currentVersion), Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    if (customCallback != null) {
                        customCallback.onError(e.getMessage());
                    } else if (isManual) {
                        Toast.makeText(activity, R.string.update_error_network, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Show modern dialog prompting the user that a new release is available.
     */
    public void showUpdateAvailableDialog(Activity activity, UpdateInfo info, String currentVersion) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        dismissActiveDialog();

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_update_available, null);

        TextView tvNewVersionTag = dialogView.findViewById(R.id.tvNewVersionTag);
        TextView tvCurrentVersionTag = dialogView.findViewById(R.id.tvCurrentVersionTag);
        TextView tvChangelog = dialogView.findViewById(R.id.tvChangelog);
        TextView tvApkSize = dialogView.findViewById(R.id.tvApkSize);
        MaterialButton btnLater = dialogView.findViewById(R.id.btnUpdateLater);
        MaterialButton btnUpdateNow = dialogView.findViewById(R.id.btnUpdateNow);

        tvNewVersionTag.setText("v" + info.latestVersion);
        tvCurrentVersionTag.setText("(Current: v" + currentVersion + ")");
        tvChangelog.setText(info.changelog);

        if (info.apkSizeBytes > 0) {
            tvApkSize.setVisibility(View.VISIBLE);
            tvApkSize.setText("Download size: " + formatFileSize(info.apkSizeBytes));
        } else {
            tvApkSize.setVisibility(View.GONE);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnLater.setOnClickListener(v -> dialog.dismiss());

        btnUpdateNow.setOnClickListener(v -> {
            dialog.dismiss();
            if (info.apkDownloadUrl != null && !info.apkDownloadUrl.isEmpty()) {
                startDownloadAndInstall(activity, info);
            } else {
                // If release has no direct APK asset, open GitHub release page in browser
                openWebUrl(activity, info.releasePageUrl);
            }
        });

        activeDialog = dialog;
        dialog.show();
    }

    /**
     * Download the APK asset and automatically launch the package installer.
     */
    private void startDownloadAndInstall(Activity activity, UpdateInfo info) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_update_downloading, null);
        TextView tvDownloadStatus = dialogView.findViewById(R.id.tvDownloadStatus);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBarDownload);
        TextView tvProgressPercent = dialogView.findViewById(R.id.tvProgressPercent);
        TextView tvProgressBytes = dialogView.findViewById(R.id.tvProgressBytes);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelDownload);

        AlertDialog downloadDialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (downloadDialog.getWindow() != null) {
            downloadDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        AtomicBoolean isCancelled = new AtomicBoolean(false);

        btnCancel.setOnClickListener(v -> {
            isCancelled.set(true);
            downloadDialog.dismiss();
        });

        downloadDialog.show();
        activeDialog = downloadDialog;

        executor.execute(() -> {
            File targetApkFile = null;
            try {
                File downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir == null) {
                    downloadDir = new File(activity.getCacheDir(), "updates");
                }
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                targetApkFile = new File(downloadDir, "BT_Laptop_Keyboard_v" + info.latestVersion + ".apk");
                if (targetApkFile.exists()) {
                    targetApkFile.delete();
                }

                // Connect following redirects
                HttpURLConnection conn = openConnectionWithRedirects(info.apkDownloadUrl);
                int fileLength = conn.getContentLength();

                mainHandler.post(() -> {
                    if (downloadDialog.isShowing()) {
                        tvDownloadStatus.setText("Downloading v" + info.latestVersion + "...");
                        if (fileLength > 0) {
                            progressBar.setIndeterminate(false);
                            progressBar.setMax(100);
                        }
                    }
                });

                InputStream input = conn.getInputStream();
                FileOutputStream output = new FileOutputStream(targetApkFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                long lastUiUpdate = 0;

                while ((count = input.read(data)) != -1) {
                    if (isCancelled.get()) {
                        output.close();
                        input.close();
                        conn.disconnect();
                        targetApkFile.delete();
                        return;
                    }

                    total += count;
                    output.write(data, 0, count);

                    long now = System.currentTimeMillis();
                    if (now - lastUiUpdate > 100 || count == -1) {
                        lastUiUpdate = now;
                        final long currentTotal = total;
                        final int progress = fileLength > 0 ? (int) ((currentTotal * 100) / fileLength) : 0;

                        mainHandler.post(() -> {
                            if (downloadDialog.isShowing()) {
                                if (fileLength > 0) {
                                    progressBar.setProgress(progress);
                                    tvProgressPercent.setText(progress + "%");
                                    tvProgressBytes.setText(formatFileSize(currentTotal) + " / " + formatFileSize(fileLength));
                                } else {
                                    tvProgressPercent.setText("");
                                    tvProgressBytes.setText(formatFileSize(currentTotal));
                                }
                            }
                        });
                    }
                }

                output.flush();
                output.close();
                input.close();
                conn.disconnect();

                final File downloadedFile = targetApkFile;
                mainHandler.post(() -> {
                    if (downloadDialog.isShowing()) {
                        downloadDialog.dismiss();
                    }
                    if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0) {
                        installApk(activity, downloadedFile);
                    } else {
                        Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_SHORT).show();
                        openWebUrl(activity, info.releasePageUrl);
                    }
                });

            } catch (Exception e) {
                if (targetApkFile != null && targetApkFile.exists()) {
                    targetApkFile.delete();
                }
                mainHandler.post(() -> {
                    if (downloadDialog.isShowing()) {
                        downloadDialog.dismiss();
                    }
                    if (!isCancelled.get()) {
                        Toast.makeText(activity, R.string.update_download_failed, Toast.LENGTH_SHORT).show();
                        openWebUrl(activity, info.releasePageUrl);
                    }
                });
            }
        });
    }

    /**
     * Follow HTTP redirects (301, 302, 307, 308) across domains such as GitHub to AWS S3.
     */
    private HttpURLConnection openConnectionWithRedirects(String urlString) throws IOException {
        String currentUrl = urlString;
        int redirects = 0;

        while (redirects < 6) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "BT-Laptop-Keyboard-Android");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER
                    || status == 307
                    || status == 308) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null && !newUrl.isEmpty()) {
                    currentUrl = newUrl;
                    redirects++;
                    conn.disconnect();
                    continue;
                }
            }
            return conn;
        }
        throw new IOException("Too many redirects: " + urlString);
    }

    /**
     * Launch the system installer for the downloaded APK using FileProvider.
     */
    public void installApk(Activity activity, File apkFile) {
        if (apkFile == null || !apkFile.exists() || apkFile.length() == 0) {
            Toast.makeText(activity, "Update package not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // On Android 8.0+ (Oreo, API 26+), verify UNKNOWN_APP_SOURCES permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                pendingInstallFile = apkFile;
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.update_install_permission_title)
                        .setMessage(R.string.update_install_permission_msg)
                        .setPositiveButton(R.string.update_open_settings, (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            intent.setData(Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivityForResult(intent, REQUEST_CODE_INSTALL_PERMISSION);
                        })
                        .setNegativeButton(R.string.action_dismiss, null)
                        .show();
                return;
            }
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".provider",
                    apkFile
            );

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(installIntent);

        } catch (Exception e) {
            Toast.makeText(activity, "Failed to launch installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Check if there is an installation waiting after returning from the permission settings screen.
     */
    public void resumePendingInstall(Activity activity) {
        if (pendingInstallFile != null && pendingInstallFile.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.getPackageManager().canRequestPackageInstalls()) {
                File file = pendingInstallFile;
                pendingInstallFile = null;
                installApk(activity, file);
            }
        }
    }

    public String getCurrentVersionName() {
        try {
            PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0.0";
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    /**
     * Robust semver comparison.
     * Returns true if remoteVersion > localVersion.
     */
    public static boolean isVersionNewer(String remoteVersion, String localVersion) {
        if (remoteVersion == null || localVersion == null) return false;

        String cleanRemote = cleanVersionString(remoteVersion);
        String cleanLocal = cleanVersionString(localVersion);

        String[] remoteParts = cleanRemote.split("[.-]");
        String[] localParts = cleanLocal.split("[.-]");

        int maxLength = Math.max(remoteParts.length, localParts.length);
        for (int i = 0; i < maxLength; i++) {
            int rPart = 0;
            int lPart = 0;

            if (i < remoteParts.length) {
                try {
                    rPart = Integer.parseInt(remoteParts[i].replaceAll("\\D+", ""));
                } catch (Exception ignored) {
                    rPart = 0;
                }
            }

            if (i < localParts.length) {
                try {
                    lPart = Integer.parseInt(localParts[i].replaceAll("\\D+", ""));
                } catch (Exception ignored) {
                    lPart = 0;
                }
            }

            if (rPart > lPart) {
                return true;
            } else if (rPart < lPart) {
                return false;
            }
        }

        return false;
    }

    public static String cleanVersionString(String version) {
        if (version == null) return "0.0.0";
        String clean = version.trim();
        if (clean.startsWith("v") || clean.startsWith("V")) {
            clean = clean.substring(1).trim();
        }
        return clean;
    }

    @SuppressLint("DefaultLocale")
    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1.0) {
            return String.format("%.1f MB", mb);
        }
        double kb = bytes / 1024.0;
        return String.format("%.0f KB", kb);
    }

    private void openWebUrl(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open browser: " + url, Toast.LENGTH_SHORT).show();
        }
    }

    private void showInfoDialog(Activity activity, String title, String message) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.action_dismiss, null)
                .show();
    }

    private void dismissActiveDialog() {
        if (activeDialog != null && activeDialog.isShowing()) {
            try {
                activeDialog.dismiss();
            } catch (Exception ignored) {}
        }
        activeDialog = null;
    }
}
