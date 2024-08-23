/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foxandgeeseserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author ilicm
 */
public class FoxAndGeeseServer {
    
    private ServerSocket ssocket;
    private int port;
    private ArrayList<ClientConnect> clients;

    public ServerSocket getSsocket() {
        return ssocket;
    }

    public void setSsocket(ServerSocket ssocket) {
        this.ssocket = ssocket;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void acceptClients() {
        Socket client = null;
        Thread thrClients;
        
        while (true) {
            try {
                System.out.println("Waiting for new clients...");
                client = this.ssocket.accept();
            } catch (IOException ex) {
                System.out.println("Problem with client's socket!");
            }
            if (client != null) {
                System.out.println("Client is successfully connected.");
                //Create thread to serve client
                ClientConnect servedClient = new ClientConnect(client, clients);

                clients.add(servedClient);
                thrClients = new Thread(servedClient);
                thrClients.start();
            } else {
                System.out.println("Client connection failed.");
                break;
            }
        }
    }

    public FoxAndGeeseServer(int port) {
        this.clients = new ArrayList<>();
        try {
            this.port = port;
            this.ssocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("Cannot create server");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        FoxAndGeeseServer server = new FoxAndGeeseServer(6001);

        System.out.println("Server is running and listening on port 6001.");

        server.acceptClients();

    }
}
