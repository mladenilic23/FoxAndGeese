package com.example.foxandgeese;

import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;

public class MessageFromServer implements Runnable {
    private final MainActivity parent;
    private final BufferedReader br;
    private boolean running;

    public MessageFromServer(MainActivity parent) {
        this.parent = parent;
        this.br = parent.getBr();
        this.running = true;
    }

    // Receive a message, parse the data and in runOnUiThread method modify necessary GUI changes
    @Override
    public void run() {
        while (running) {
            try {
                String line = this.br.readLine();
                if (line != null) {
                    handleServerMessage(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void handleServerMessage(String line) {
        if (line.startsWith("NewList")) {
            handleNewList(line);
        } else if (line.startsWith("Received challenge")) {
            handleChallenge(line);
        } else if (line.startsWith("CantChallengeYourself")) {
            showToast("Can not challenge yourself!");
        } else if (line.startsWith("UserNotAvailable")) {
            showToast("Player is't available right now!");
        } else if (line.startsWith("Declined user")) {
            updateMatchText("Declined, try again later!");
        } else if (line.startsWith("Start")) {
            launchGameActivity();
        }
    }

    private void handleNewList(String line) {
        String[] userTokens = line.split(":");
        String[] usernames = userTokens[1].split(",");

        parent.runOnUiThread(() -> {
            Spinner spinner = parent.getSpinnerPlayers();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(parent, android.R.layout.simple_spinner_item, usernames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            parent.getSaveButton().setEnabled(false);
            parent.getPlayerNameText().setEnabled(false);
            parent.getPlayButton().setEnabled(true);
            spinner.setEnabled(true);
        });
    }

    private void handleChallenge(String line) {
        String[] challengeToken = line.split(":");
        String challenger = challengeToken[1];
        String challengeMsg = "Challenge: " + challenger;
        parent.setRival(challenger);

        parent.runOnUiThread(() -> {
            parent.getMatchText().setText(challengeMsg);
            parent.getAcceptButton().setEnabled(true);
            parent.getCancelButton().setEnabled(true);
        });
    }

    private void showToast(String message) {
        parent.runOnUiThread(() -> Toast.makeText(parent, message, Toast.LENGTH_SHORT).show());
    }

    private void updateMatchText(String message) {
        parent.runOnUiThread(() -> parent.getMatchText().setText(message));
    }

    private void launchGameActivity() {
        this.running = false;
        Intent intent = new Intent(parent, FoxAndGeeseGame.class);
        parent.runOnUiThread(() -> {
            parent.getAcceptButton().setEnabled(false);
            parent.getCancelButton().setEnabled(false);
            parent.activity2Launcher.launch(intent);
        });
    }
    public void setRunning(boolean running) {
        this.running = running;
    }

}
