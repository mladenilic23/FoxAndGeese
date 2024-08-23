package com.example.foxandgeese;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

public class FoxAndGeeseGame extends AppCompatActivity {
    private static final int NUM_ROWS = 8;
    private static final int NUM_OF_COLUMNS = 8;
    private static final int[] FOX_VARIANTS = {1, 3, 5, 7};
    private static final long FIXED_SEED = 1234;

    public HashMap<String, ImageView> board;
    private TextView turnText;
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;
    private int positionsOfFox;
    private Random rand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectToServer();
        EdgeToEdge.enable(this);
        setContentView(R.layout.game_activity);

        board = new HashMap<>();
        LinearLayout linearLayout = findViewById(R.id.board_layout);
        turnText = findViewById(R.id.player_turn);
        rand = new Random(FIXED_SEED);

        // Start listener thread
        new Thread(new MessageFromServerBoardGame(this)).start();

        // Initialize game board
        positionsOfFox = getRandomFoxPosition();
        initializeBoard(linearLayout);
        sendMessage("FoxPosition:" + positionsOfFox);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.blank_area), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private int getRandomFoxPosition() {
        return FOX_VARIANTS[rand.nextInt(FOX_VARIANTS.length)];
    }

    private void initializeBoard(LinearLayout linearLayout) {
        for (int row = 0; row < NUM_ROWS; row++) {
            LinearLayout linearLayoutRow = new LinearLayout(this);
            linearLayoutRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
            linearLayoutRow.setLayoutParams(rowLayoutParams);

            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                ImageView iv = new ImageView(this);
                iv.setTag(row + "," + col);
                board.put(row + "," + col, iv);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                iv.setLayoutParams(layoutParams);

                setSquareBackground(iv, row, col);
                setInitialPieces(iv, row, col);

                iv.setOnClickListener(v -> sendMessage("NewMove:" + v.getTag().toString()));
                linearLayoutRow.addView(iv);
            }
            linearLayout.addView(linearLayoutRow);
        }
    }

    private void setSquareBackground(ImageView iv, int row, int col) {
        boolean isDarkSquare = (row % 2 == 0 && col % 2 == 0) || (row % 2 != 0 && col % 2 != 0);
        iv.setImageResource(isDarkSquare ? R.drawable.dark_brown_square : R.drawable.light_brown_square);
    }

    private void setInitialPieces(ImageView iv, int row, int col) {
        if (row == 0 && col % 2 == 0) {
            iv.setImageResource(R.drawable.red_circle_geese);
        } else if (row == NUM_ROWS - 1 && col == positionsOfFox) {
            iv.setImageResource(R.drawable.black_circle_fox);
        }
    }

    public void reinitializeGame() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.game_activity);

        board = new HashMap<>();
        LinearLayout linearLayout = findViewById(R.id.board_layout);
        turnText = findViewById(R.id.player_turn);

        positionsOfFox = getRandomFoxPosition();
        initializeBoard(linearLayout);
        sendMessage("FoxPosition:" + positionsOfFox);
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                Singleton singleton = Singleton.getInstance();
                if (singleton != null) {
                    FoxAndGeeseGame.this.socket = singleton.socket;
                    FoxAndGeeseGame.this.br = singleton.br;
                    FoxAndGeeseGame.this.pw = singleton.pw;
                } else {
                    System.out.println("Problem with socket, pw and br!");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            if (FoxAndGeeseGame.this.pw != null) {
                FoxAndGeeseGame.this.pw.println(message);
            }
        }).start();
    }

    public BufferedReader getBufferedReader() {
        return this.br;
    }

    public TextView getTurnText() {
        return this.turnText;
    }
}
