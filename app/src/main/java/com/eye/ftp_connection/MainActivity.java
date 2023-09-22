package com.eye.ftp_connection;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    AutoCompleteTextView urlAddress, userName, password, path, fileName;
    ConstraintLayout connectBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        urlAddress = findViewById(R.id.autoCompleteAddress);
        userName = findViewById(R.id.autoCompleteUser);
        password = findViewById(R.id.autoCompletePass);
        path = findViewById(R.id.autoCompleteLoc);
        fileName = findViewById(R.id.autoCompleteName);
        connectBtn = findViewById(R.id.connect_Btn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onCreate: " + urlAddress.getText());
                if (urlAddress.getText().length() > 0 && userName.getText().length() > 0) {
                    // working for android 8 >=
                    new ConnectTask().execute();
                }else {
                    Toast.makeText(MainActivity.this, "fill the required fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                FTPClient ftpClient = new FTPClient();
                ftpClient.connect(String.valueOf(urlAddress.getText()));
                // Create an instance of PrintWriter
                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

                // Create an instance of PrintCommandListener
                PrintCommandListener printCommandListener = new PrintCommandListener(printWriter, true);

                // Add protocol command listener
                ftpClient.addProtocolCommandListener(printCommandListener);

                ftpClient.login(String.valueOf(userName.getText()), String.valueOf(password.getText()));
                ftpClient.changeWorkingDirectory("/");

                Log.d(TAG, "doInBackground: successful");
                return true;

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "there is a problem", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(aBoolean) {
                // Credentials valid
                showSuccessMessage();
                startIntent();
            } else {
                // Invalid credentials
                showErrorMessage();
            }
        }

        private void showErrorMessage() {
            Toast.makeText(MainActivity.this, "Error Check Your Internet or Check Server Config", Toast.LENGTH_LONG).show();
        }

        private void showSuccessMessage() {
            Toast.makeText(MainActivity.this, "Connected Successfully", Toast.LENGTH_SHORT).show();
        }
        private void startIntent() {
            Intent intent = new Intent(MainActivity.this , FTP_connection.class);
            intent.putExtra("server", urlAddress.getText().toString());
            intent.putExtra("username",userName.getText().toString());
            intent.putExtra("password", password.getText().toString());
            intent.putExtra("path", path.getText().toString());
            intent.putExtra("filename", fileName.getText().toString());
            startActivity(intent);
            Log.d(TAG, "startIntent: " + urlAddress.getText() +
                    " " + userName.getText() +
                    " " + password.getText() +
                    " " + path.getText() +
                    " " + fileName.getText()
            );
        }
    }
}