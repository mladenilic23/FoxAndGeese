package com.example.foxandgeese;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class Singleton {

    // Static variable reference of single_instance
    // of type Singleton
    private static Singleton single_instance = null;

    public static Socket socket;
    public static BufferedReader br;
    public static PrintWriter pw;

    public static String ipAddress = "10.0.2.2";

    public static void setIpAddress(String ipAddress) {
        Singleton.ipAddress = ipAddress;
    }

    private Singleton() throws IOException {
        //10.0.2.2  //192.168.0.24//
        this.socket = new Socket(ipAddress, 6001);
        this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.pw = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()), true);

    }

    // Static method to create instance of Singleton class
    public static synchronized Singleton getInstance() throws IOException
    {
        if (single_instance == null)
        {
            System.out.println("Creating singleton");

            single_instance = new Singleton();
        }

        return single_instance;
    }

}
