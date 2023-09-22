package com.eye.ftp_connection;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FTP_connection extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private ExoPlayer player;
    ProgressBar progressBar;
    long playerPosition;
    boolean playWhenReady;
    private File downloadedFile;
    private String server;
    private String username;
    private String password;
    private String path;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_connection);

        progressBar = findViewById(R.id.progressbar_ftp);
        PlayerView playerView = findViewById(R.id.PlayerView);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        //get the value form MainActivity
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        server = extras.getString("server");
        username = extras.getString("username");
        password = extras.getString("password");
        path = extras.getString("path");
        filename = extras.getString("filename");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            startDownload();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                Toast.makeText(this, "Permission denied. Cannot download file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startDownload() {
        File downloadedFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "1.mp4");

        if(downloadedFile.exists()) {
            // File already exists, don't need to download again
            onDownloadComplete(downloadedFile);
        }
        else {
            // File doesn't exist, start download
            new DownloadFileTask().execute();
        }
    }
    //if download is completed before
    private void onDownloadComplete(File file) {
        // Initialize player with existing file
        MediaItem mediaItem = MediaItem.fromUri(file.getAbsolutePath());
        // Rest of initialization...
        progressBar.setVisibility(View.INVISIBLE);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
    private class DownloadFileTask extends AsyncTask<Void, Void, File> {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected File doInBackground(Void... voids) {

            // FTP credentials
            String ftpUrl = server;
            String ftpUser = username;
            String ftpPass = password;
            String ftpPath = path + filename;
            String ftpFileName = filename;
            Log.d(TAG, "doInBackground: path is:" + "; ftpUser=" + ftpUser + " ftpPass" + ftpPass + " ftpPath=" + ftpPath);

            // Video file details
//            String videoPath = "Files/1.mp4";

            try {
                FTPClient ftpClient = new FTPClient();
                ftpClient.connect(ftpUrl);
                // Create an instance of PrintWriter
                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

                // Create an instance of PrintCommandListener
                PrintCommandListener printCommandListener = new PrintCommandListener(printWriter, true);

                // Add protocol command listener
                ftpClient.addProtocolCommandListener(printCommandListener);

                ftpClient.login(ftpUser, ftpPass);
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();
                ftpClient.setBufferSize(1024 * 1024);

                File downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                    Toast.makeText(FTP_connection.this, "Creating the Local Folder", Toast.LENGTH_SHORT).show();
                }
                File downloadFile = new File(downloadDir, ftpFileName);

                OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(downloadFile.toPath()));
                ftpClient.retrieveFile(ftpPath, outputStream);
                outputStream.close();
                downloadedFile = downloadFile;
                return downloadFile;

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("FTP", "there is a problem", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File result) {
            super.onPostExecute(result);
            onDownloadComplete(result);
            progressBar.setVisibility(View.INVISIBLE);
            MediaItem mediaItem = MediaItem.fromUri(result.getAbsolutePath());
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
    }

    // Class fields to store state

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save playback position
        outState.putLong("position", playerPosition);

        // Save playback state
        outState.putBoolean("play_when_ready", playWhenReady);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore playback position
        playerPosition = savedInstanceState.getLong("position");

        // Restore playback state
        playWhenReady = savedInstanceState.getBoolean("play_when_ready");



        // When activity starts
        if(playerPosition > 0) {
            MediaItem mediaItem = MediaItem.fromUri(downloadedFile.getAbsolutePath());
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();

            player.setMediaItem(mediaItem, playerPosition);
            player.setPlayWhenReady(playWhenReady);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}