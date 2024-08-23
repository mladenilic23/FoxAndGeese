package com.example.foxandgeese;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private MessageFromServer message;
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;
    private String rival;

    // UI components
    private Button connectButton, saveButton, acceptButton, cancelButton, playButton;
    private EditText ipAddressText, playerNameText;
    private Spinner spinnerPlayers;
    private TextView matchText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_activity);

        initUIComponents();
        setupButtonListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.blank_area), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initUIComponents() {
        connectButton = findViewById(R.id.connect_button);
        playButton = findViewById(R.id.play_button);
        acceptButton = findViewById(R.id.accept_button);
        cancelButton = findViewById(R.id.cancel_button);
        saveButton = findViewById(R.id.save_button);
        ipAddressText = findViewById(R.id.ip_address);
        playerNameText = findViewById(R.id.player_name);
        spinnerPlayers = findViewById(R.id.spinner);
        matchText = findViewById(R.id.match_title);

        playButton.setEnabled(false);
        acceptButton.setEnabled(false);
        cancelButton.setEnabled(false);
        saveButton.setEnabled(false);
        playerNameText.setEnabled(false);
        spinnerPlayers.setEnabled(false);
    }

    private void setupButtonListeners() {
        connectButton.setOnClickListener(v -> {
            connectToServer();
            saveButton.setEnabled(true);
            playerNameText.setEnabled(true);
            connectButton.setEnabled(false);
            ipAddressText.setEnabled(false);
        });

        saveButton.setOnClickListener(v -> {
            String playerName = playerNameText.getText().toString();
            if (!playerName.isEmpty()) {
                sendMessage("Name:" + playerName);
                message = new MessageFromServer(MainActivity.this);
                new Thread(message).start();
            }
        });

        playButton.setOnClickListener(v -> {
            String userToChallenge = spinnerPlayers.getSelectedItem().toString();
            String rival = playerNameText.getText().toString().trim();
            sendMessage("Challenge:" + userToChallenge + ":" + rival);
        });

        cancelButton.setOnClickListener(v -> {
            String[] declineToken = matchText.getText().toString().split(":");
            sendMessage("Declined:" + declineToken[1]);
            getAcceptButton().setEnabled(false);
            getCancelButton().setEnabled(false);
        });

        acceptButton.setOnClickListener(v -> {
            String[] contestantTokens = matchText.getText().toString().split(":");
            sendMessage("Accepted:" + contestantTokens[1] + ":" + playerNameText.getText().toString());
        });
    }

    private void connectToServer() {
        new Thread(() -> {
            if (socket == null) {
                try {
                    String ip_address = ipAddressText.getText().toString();
                    if (!ip_address.isEmpty()) {
                        Singleton.setIpAddress(ip_address);
                        Singleton singleton = Singleton.getInstance();
                        socket = singleton.socket;
                        br = singleton.br;
                        pw = singleton.pw;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendMessage(String message) {
        new Thread(() -> {
            if (pw != null) {
                pw.println(message);
            }
        }).start();
    }

    ActivityResultLauncher<Intent> activity2Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && message != null) {
                    message.setRunning(true);
                    new Thread(message).start();
                }
            }
    );
    // Getter and Setter methods
    public String getRival() {
        return rival;
    }

    public void setRival(String rival) {
        this.rival = rival;
    }

    public BufferedReader getBr() {
        return br;
    }

    public Spinner getSpinnerPlayers() {
        return spinnerPlayers;
    }

    public EditText getPlayerNameText() {
        return playerNameText;
    }

    public EditText getIpAddressText() {
        return ipAddressText;
    }

    public Button getConnectButton() {
        return connectButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getAcceptButton() {
        return acceptButton;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public Button getPlayButton() {
        return playButton;
    }

    public TextView getMatchText() {
        return matchText;
    }
}
