/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foxandgeeseserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author ilicm
 */
public class ClientConnect implements Runnable{
  
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;
    private ArrayList<ClientConnect> allClients;
    private String username;
    
    private boolean availlable;
    private boolean isFox; 
    private boolean isGeese;
    private boolean myTurn;
    
    private int[][] boardMatrix;
    
    private String playerFox;
    private String playerGeese;
    private String stateOfGeese;
    
    private int currentGeeseRow;
    private int currentGeeseCol;
    private int currentFoxRow;
    private int currentFoxCol;
    
    private boolean agreeToPlayAgain;

      
    public ClientConnect(Socket socket,ArrayList<ClientConnect> allClients)
    {
        try {
            this.socket = socket;
            this.allClients = allClients;           
            this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
            this.pw = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()),true);
            
            this.username = "";
            this.availlable = true;
            
            this.isFox = false;
            this.isGeese = false;
            this.myTurn = false;
            this.boardMatrix = new int[8][8];
            this.playerFox = "";
            this.playerGeese = "";
            this.stateOfGeese = "GEESE_SELECT";
            this.agreeToPlayAgain = false;
        } catch (IOException ex) {
            System.out.println("Cannoot get data from server.");        }
    }

    // Prints out the board in console
    public void printBoardMatrix() {
        System.out.println("Board matrix:");
        for (int[] row : boardMatrix) {
            for (int cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
    }



    @Override
    public void run() {
    String line;
    while (true) {
        try {
            line = br.readLine();
        } catch (IOException ex) {
            Logger.getLogger(ClientConnect.class.getName()).log(Level.SEVERE, null, ex);
            continue;
        }

        if (line == null) continue;

        if (line.startsWith("Name:")) {
            handleNameMessage(line);
        } else if (line.startsWith("Challenge:")) {
            handleChallengeMessage(line);
        } else if (line.startsWith("Declined:")) {
            handleDeclineMessage(line);
        } else if (line.startsWith("Accepted:")) {
            handleAcceptMessage(line);
        } else if (line.startsWith("FoxPosition")) {
            handleFoxPositionMessage(line);
        } else if (line.startsWith("NewMove")) {
            handleNewMoveMessage(line);
        } else if (line.startsWith("DoNotWannaPlay")) {
            handleDontWannaPlayMessage();
        } else if (line.startsWith("RestartGame")) {
            handleRestartGameMessage();
        }
    }
    }

    private void handleNameMessage(String line) {
        String[] userToken = line.split(":");
        String newUsername = userToken[1];
        System.out.println("Received username message, new user: " + newUsername);
        this.username = newUsername;
        String allUsers = this.allClients.stream()
                                         .map(clnt -> clnt.username)
                                         .collect(Collectors.joining(","));
        this.allClients.forEach(clnt -> clnt.pw.println("NewList:" + allUsers));
        System.out.println("Sending new list: " + allUsers);
    }

    private void handleChallengeMessage(String line) {
        String[] challengeToken = line.split(":");
        String userToChallenge = challengeToken[1];
        String challenger = challengeToken[2];
        System.out.println("Player 1: " + challenger + " vs " + "Player 2:" + userToChallenge);
        if (!this.username.equals(userToChallenge)) {
            this.allClients.stream()
                           .filter(clnt -> clnt.username.equals(userToChallenge))
                           .findFirst()
                           .ifPresentOrElse(clnt -> {
                               if (clnt.availlable) {
                                   clnt.pw.println("Received challenge - User: " + challenger + " challenges: " + clnt.username);
                               } else {
                                   this.pw.println("UserNotAvailable");
                                   System.out.println("User is not available!");
                               }
                           }, () -> {
                               System.out.println("User challenged himself! This is not possible.");
                               this.pw.println("CantChallengeYourself");
                           });
        }
    }

    private void handleDeclineMessage(String line) {
        String[] declineToken = line.split(":");
        String declinedUser = declineToken[1];
        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(declinedUser))
                       .findFirst()
                       .ifPresent(clnt -> clnt.pw.println("Declined user"));
    }

    private void handleAcceptMessage(String line) {
        String[] acceptTokens = line.split(":");
        String player1 = acceptTokens[1];
        String player2 = acceptTokens[2];
        System.out.println("Fox: " + player1 + " geese: " + player2);

        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(player1) || clnt.username.equals(player2))
                       .forEach(clnt -> {
                           clnt.playerFox = player1;
                           clnt.playerGeese = player2;
                       });

        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(player2))
                       .forEach(clnt -> {
                           clnt.myTurn = false;
                           clnt.isGeese = true;
                           clnt.isFox = false;
                       });

        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(player1))
                       .forEach(clnt -> {
                           clnt.myTurn = true;
                           clnt.isFox = true;
                           clnt.isGeese = false;
                       });

        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(player1) || clnt.username.equals(player2))
                       .forEach(clnt -> {
                           clnt.availlable = false;
                           clnt.pw.println("Start");
                       });
    }

    private void handleFoxPositionMessage(String line) {
        String[] matrixToken = line.split(":");
        int foxPosition = Integer.parseInt(matrixToken[1].trim());
        for (int row = 0; row <= 7; row++) {
            for (int col = 0; col <= 7; col++) {
                this.boardMatrix[row][col] = (row % 2 == col % 2) ? 0 : 1;
                if (row == 0 && col % 2 == 0) {
                    this.boardMatrix[row][col] = 2;
                }
                if (row == 7 && col == foxPosition) {
                    this.boardMatrix[row][col] = 3;
                    this.allClients.stream()
                                   .filter(clnt -> clnt.username.equals(this.playerFox) || clnt.username.equals(this.playerGeese))
                                   .forEach(clnt -> {
                                       clnt.currentFoxRow = 7;
                                       clnt.currentFoxCol = foxPosition;
                                   });
                }
            }
        }
        printBoardMatrix();
    }

    private void handleNewMoveMessage(String line) {
        if (!this.myTurn) {
            System.out.println("Not my turn!");
            this.pw.println("NotYourTurn");
            return;
        }

        String gameState;
        String[] moveToken = line.split(":");
        String[] positionToken = moveToken[1].split(",");
        int newRow = Integer.parseInt(positionToken[0]);
        int newCol = Integer.parseInt(positionToken[1]);

        if (this.isFox) {
            handleFoxMove(newRow, newCol);
        } else if (this.isGeese) {
            handleGeeseMove(newRow, newCol);
        }
    }

    private void handleFoxMove(int newRow, int newCol) {
        if (isValidFoxMove(newRow, newCol)) {
            updateBoardForFox(newRow, newCol);
            String gameState = evaluateGameStateAfterFoxMove();
            notifyClientsAboutFoxMove(newRow, newCol, gameState);
            switchTurnToGeese();
        } else {
            System.out.println("Invalid fox move!");
            this.pw.println("InvalidFoxMove");
        }
    }

    private boolean isValidFoxMove(int newRow, int newCol) {
        return Math.abs(newRow - this.currentFoxRow) == 1 &&
               Math.abs(newCol - this.currentFoxCol) == 1 &&
               this.boardMatrix[newRow][newCol] == 0;
    }

    private void updateBoardForFox(int newRow, int newCol) {
        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(this.playerFox) || clnt.username.equals(this.playerGeese))
                       .forEach(clnt -> {
                           clnt.boardMatrix[this.currentFoxRow][this.currentFoxCol] = 0;
                           clnt.boardMatrix[newRow][newCol] = 3;
                           clnt.currentFoxRow = newRow;
                           clnt.currentFoxCol = newCol;
                       });
    }

    private String evaluateGameStateAfterFoxMove() {
        if (this.currentFoxRow == 0) {
            return "FoxWin";
        } else if (didGeeseWin()) {
            return "GeeseWin";
        } else {
            return "FoxUpdate";
        }
    }

    private void notifyClientsAboutFoxMove(int newRow, int newCol, String gameState) {
        String oldRowCol = this.currentFoxRow + ":" + this.currentFoxCol;
        String newRowCol = newRow + ":" + newCol;
        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(this.playerFox) || clnt.username.equals(this.playerGeese))
                       .forEach(clnt -> {
                           switch (gameState) {
                               case "FoxWin":
                                   System.out.println("Fox wins!");
                                   clnt.pw.println("FoxWin:" + oldRowCol + ":" + newRowCol);
                                   break;
                               case "GeeseWin":
                                   System.out.println("Geese wins!");
                                   clnt.pw.println("GeeseWin:" + oldRowCol + ":" + newRowCol);
                                   break;
                               case "FoxUpdate":
                                   System.out.println("Fox move valid, sending a request to update GUI!");
                                   printBoardMatrix();
                                   clnt.pw.println("UpdateBoardFox:" + oldRowCol + ":" + newRowCol);
                                   System.out.println("Update sent to GUI: " + oldRowCol + ":" + newRowCol);
                                   break;
                           }
                       });
    }

    private void switchTurnToGeese() {
        this.myTurn = false;
        this.allClients.stream()
                       .filter(clnt -> clnt.isGeese)
                       .findFirst()
                       .ifPresent(clnt -> clnt.myTurn = true);
    }

    private void handleGeeseMove(int newRow, int newCol) {
        if ("GEESE_SELECT".equals(this.stateOfGeese)) {
            selectGeese(newRow, newCol);
        } else if ("GEESE_MOVE".equals(this.stateOfGeese)) {
            moveGeese(newRow, newCol);
        }
    }

    private void selectGeese(int newRow, int newCol) {
        if (this.boardMatrix[newRow][newCol] == 2) {
            this.currentGeeseRow = newRow;
            this.currentGeeseCol = newCol;
            this.stateOfGeese = "GEESE_MOVE";
            this.pw.println("GeeseSelected:" + newRow + ":" + newCol);
        } else {
            System.out.println("Invalid geese selection!");
            this.pw.println("InvalidGeeseSelection");
        }
    }

    private void moveGeese(int newRow, int newCol) {
        if (isValidGeeseMove(newRow, newCol)) {
            updateBoardForGeese(newRow, newCol);
            String gameState = evaluateGameStateAfterGeeseMove();
            notifyClientsAboutGeeseMove(newRow, newCol, gameState);
            switchTurnToFox();
        } else {
            System.out.println("Invalid geese move!");
            this.pw.println("InvalidGeeseMove");
        }
    }

    private boolean isValidGeeseMove(int newRow, int newCol) {
        return Math.abs(newRow - this.currentGeeseRow) == 1 &&
               Math.abs(newCol - this.currentGeeseCol) == 1 &&
               this.boardMatrix[newRow][newCol] == 0;
    }

    private void updateBoardForGeese(int newRow, int newCol) {
        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(this.playerFox) || clnt.username.equals(this.playerGeese))
                       .forEach(clnt -> {
                           clnt.boardMatrix[this.currentGeeseRow][this.currentGeeseCol] = 0;
                           clnt.boardMatrix[newRow][newCol] = 2;
                           clnt.currentGeeseRow = newRow;
                           clnt.currentGeeseCol = newCol;
                       });
    }

    private String evaluateGameStateAfterGeeseMove() {
        if (didGeeseWin()) {
            return "GeeseWin";
        } else {
            return "GeeseUpdate";
        }
    }

    private void notifyClientsAboutGeeseMove(int newRow, int newCol, String gameState) {
        String oldRowCol = this.currentGeeseRow + ":" + this.currentGeeseCol;
        String newRowCol = newRow + ":" + newCol;
        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(this.playerFox) || clnt.username.equals(this.playerGeese))
                       .forEach(clnt -> {
                           switch (gameState) {
                               case "GeeseWin":
                                   System.out.println("Geese wins!");
                                   clnt.pw.println("GeeseWin:" + oldRowCol + ":" + newRowCol);
                                   break;
                               case "GeeseUpdate":
                                   System.out.println("Geese move valid, sending a request to update GUI!");
                                   printBoardMatrix();
                                   clnt.pw.println("UpdateBoardGeese:" + oldRowCol + ":" + newRowCol);
                                   System.out.println("Update sent to GUI: " + oldRowCol + ":" + newRowCol);
                                   break;
                           }
                       });
    }

    private void switchTurnToFox() {
        this.myTurn = false;
        this.stateOfGeese = "GEESE_SELECT";
        this.allClients.stream()
                       .filter(clnt -> clnt.isFox)
                       .findFirst()
                       .ifPresent(clnt -> clnt.myTurn = true);
    }
    
        
    // Function that checks if geese have won
    public boolean didGeeseWin() {
        int row = this.currentFoxRow;
        int col = this.currentFoxCol;

        switch (col) {
            case 0:
                // Fox is on the left edge
                return isBlocked(row - 1, col + 1) && isBlocked(row + 1, col + 1);
            case 7:
                // Fox is on the right edge
                return isBlocked(row - 1, col - 1) && isBlocked(row + 1, col - 1);
            default:
                // Fox is elsewhere on the board
                return isBlocked(row - 1, col - 1) && isBlocked(row + 1, col - 1) &&
                        isBlocked(row - 1, col + 1) && isBlocked(row + 1, col + 1);
        }
    }

    private boolean isBlocked(int row, int col) {
        return this.boardMatrix[row][col] != 0;
    }
/*
    private boolean didGeeseWin() {
        return this.allClients.stream()
                              .filter(clnt -> clnt.isFox)
                              .noneMatch(clnt -> canFoxMove(clnt.currentFoxRow, clnt.currentFoxCol));
    }
*/
    private boolean canFoxMove(int foxRow, int foxCol) {
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        return Arrays.stream(directions)
                     .anyMatch(dir -> {
                         int newRow = foxRow + dir[0];
                         int newCol = foxCol + dir[1];
                         return newRow >= 0 && newRow <= 7 && newCol >= 0 && newCol <= 7 && this.boardMatrix[newRow][newCol] == 0;
                     });
    }

    private void handleDontWannaPlayMessage() {
        this.myTurn = false;
        this.availlable = true;
        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(this.playerFox) || clnt.username.equals(this.playerGeese))
                       .forEach(clnt -> {
                           clnt.playerFox = "";
                           clnt.playerGeese = "";
                           clnt.isGeese = false;
                           clnt.isFox = false;
                           clnt.pw.println("Game over");
                       });
    }

    private void handleRestartGameMessage() {
        this.myTurn = false;
        this.availlable = true;

        // Reset all player states and board matrix
        this.allClients.stream()
                       .filter(clnt -> clnt.username.equals(this.playerFox) || clnt.username.equals(this.playerGeese))
                       .forEach(clnt -> {
                           clnt.playerFox = "";
                           clnt.playerGeese = "";
                           clnt.isGeese = false;
                           clnt.isFox = false;
                           clnt.myTurn = false;
                           clnt.availlable = true;
                           clnt.currentFoxRow = -1;
                           clnt.currentFoxCol = -1;
                           clnt.currentGeeseRow = -1;
                           clnt.currentGeeseCol = -1;
                           clnt.stateOfGeese = "GEESE_SELECT";
                           resetBoardMatrix(clnt);
                           clnt.pw.println("GameRestarted");
                       });

        // Reset the server's board matrix
        resetBoardMatrix(this);
        printBoardMatrix();
    }

    private void resetBoardMatrix(ClientConnect clnt) {
        for (int row = 0; row <= 7; row++) {
            for (int col = 0; col <= 7; col++) {
                clnt.boardMatrix[row][col] = (row % 2 == col % 2) ? 0 : 1;
                if (row == 0 && col % 2 == 0) {
                    clnt.boardMatrix[row][col] = 2;
                }
            }
        }
    }
}

