package de.an_gr.SnapTwitter.HttpServer;

import de.an_gr.SnapTwitter.Debugger.Debugger;
import de.an_gr.SnapTwitter.Twitter.TwitterClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Andreas on 03.01.2016.
 */
public class HttpServer implements Runnable {
    int port = 13337;
    TwitterClient twitterClient;
    boolean ready = false;

    public HttpServer(TwitterClient twc, int port) {
        this.twitterClient = twc;
        this.port = port;
    }

    @Override
    public void run() {
        startServer();
    }

    public void startServer() {
        ServerSocket serversocket;

        // Listening socket
        try {
            serversocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Debugger.log("HttpServer: Serversocket created");

        // Server loop
        while(true) {
            try {
                ready = true;
                Socket sock = serversocket.accept();
                Debugger.log("Connection accepted");

                // Construct request handler and create thread
                RequestHandler rh = new RequestHandler(sock, twitterClient);
                Thread t = new Thread(rh);
                t.start();

            } catch(IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public boolean isReady() {
        return ready;
    }
}
