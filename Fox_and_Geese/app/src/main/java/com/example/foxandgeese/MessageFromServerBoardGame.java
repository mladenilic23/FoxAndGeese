package com.example.foxandgeese;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class MessageFromServerBoardGame implements Runnable {

    private final FoxAndGeeseGame parent;
    private final BufferedReader br;
    private final HashMap<String, ImageView> board;

    public MessageFromServerBoardGame(FoxAndGeeseGame parent) {
        this.parent = parent;
        this.br = parent.getBufferedReader();
        this.board = parent.board;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                handleServerMessage(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleServerMessage(String line) {
        parent.runOnUiThread(() -> {
            if (line.startsWith("UpdateBoardFox")) {
                updateBoard(line, R.drawable.black_circle_fox, "Geese turn!");
            } else if (line.startsWith("InvalidFoxMove")) {
                showToast("Invalid fox move!");
            } else if (line.startsWith("GeeseSelectedOk")) {
                showGeeseSelected(line);
            } else if (line.startsWith("DidNotSelectGeese")) {
                showToast("You did not select a geese!");
            } else if (line.startsWith("UpdateBoardGeese")) {
                updateBoard(line, R.drawable.red_circle_geese, "Fox turn!");
            } else if (line.startsWith("IllegalGeeseMove")) {
                showToast("Illegal geese move!");
            } else if (line.startsWith("NotYourTurn")) {
                showToast("Not your turn!");
                parent.getTurnText().setText("Not your turn!");
            } else if (line.startsWith("GeeseWin")) {
                handleWin(line, R.drawable.red_circle_geese, "Geese Win", "The geese have won!");
            } else if (line.startsWith("FoxWin")) {
                handleWin(line, R.drawable.black_circle_fox, "Fox Win", "The fox has won!");
            } else if (line.startsWith("TerminateMatch")) {
                terminateMatch();
            } else if (line.startsWith("GameToRestart")) {
                parent.reinitializeGame();
            }
        });
    }

    private void updateBoard(String line, int pieceResource, String turnInfo) {
        String[] tokens = line.split(":");
        String oldPosition = tokens[1] + "," + tokens[2];
        String newPosition = tokens[3] + "," + tokens[4];

        ImageView oldImageView = board.get(oldPosition);
        ImageView newImageView = board.get(newPosition);

        if (oldImageView != null && newImageView != null) {
            oldImageView.setImageResource(R.drawable.dark_brown_square);
            newImageView.setImageResource(pieceResource);
        }

        parent.getTurnText().setText(turnInfo);
    }

    private void showToast(String message) {
        Toast.makeText(parent, message, Toast.LENGTH_SHORT).show();
    }

    private void showGeeseSelected(String line) {
        String[] tokens = line.split(":");
        String geesePosition = tokens[1] + ":" + tokens[2];
        showToast("Geese with position: " + geesePosition + " selected!");
    }

    private void handleWin(String line, int pieceResource, String title, String message) {
        updateBoard(line, pieceResource, "");

        new AlertDialog.Builder(parent)
                .setTitle(title)
                .setMessage(message + " Do you want to play again?")
                .setPositiveButton("Yes", (dialog, which) -> parent.sendMessage("RestartGame"))
                .setNegativeButton("No", (dialog, which) -> parent.sendMessage("DoNotWannaPlay"))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void terminateMatch() {
        Intent resultIntent = new Intent();
        parent.setResult(RESULT_OK, resultIntent);
        parent.finish();
    }
}
