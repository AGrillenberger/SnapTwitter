package de.an_gr.SnapTwitter.HttpServer;

import de.an_gr.SnapTwitter.Debugger.Debugger;
import de.an_gr.SnapTwitter.SnapTwitter;
import de.an_gr.SnapTwitter.Twitter.TwitterAuth;
import de.an_gr.SnapTwitter.Twitter.TwitterClient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Created by Andreas on 03.01.2016.
 */
public class RequestHandler implements Runnable {
    final static String CRLF = "\r\n";
    Socket sock;
    InputStream is;
    OutputStream os;
    BufferedReader br;
    TwitterClient twitterClient;

    public RequestHandler(Socket sock, TwitterClient twc) throws IOException {
        this.sock = sock;
        is = sock.getInputStream();
        os = sock.getOutputStream();
        br = new BufferedReader(new InputStreamReader(is));
        twitterClient = twc;
    }

    public void run() {
        try {
            process();
        } catch (IOException e) {

        }
    }

    public void process() throws IOException {
        String line = br.readLine();
        if(line == null)
            return;
        StringTokenizer s = new StringTokenizer(line);

        if(!s.nextToken().equals("GET"))
            return;

        String command = s.nextToken();
        String answer = runCommand(command);

        if(answer == null) {
            out("HTTP/1.0 404 Not Found" + CRLF);
            out("text/plain" + CRLF);
            out(CRLF);
            out("Unknown command" + CRLF);
        } else {
            out("HTTP/1.0 200 OK" + CRLF);
            out("Access-Control-Allow-Origin: *" + CRLF);
            out("Content-Type: text/html; charset=utf-8" + CRLF);
            out(CRLF);
            out(answer);
        }

        os.close();
        br.close();
        sock.close();
    }

    private void out(String s) throws IOException {
        os.write(s.getBytes());
    }

    private String runCommand(String c) throws IOException {
        Debugger.log("RequestHandler: Command: " + c);
        String[] parts = c.split("/");

        if(parts.length < 2)
            return "<html><head><title>SnapTwitter</title></head><body><p><strong>You will be redirected to Snap with the SnapTwitter blocks in a few seconds or by clicking <a href=\"" + (SnapTwitter.getProperty("snapURL") + "#open:http://" + SnapTwitter.getLocalAdress() + "/getBlockXML") + "\">here</a>.</strong></p><script type=\"text/javascript\">window.location.href=\"" + (SnapTwitter.getProperty("snapURL") + "#open:http://" + SnapTwitter.getLocalAdress() + "/getBlockXML") + "\";</script></body></html>.";

        if(parts.length==2 && parts[1].startsWith("authCallback"))
            return twitterLogin(c);

        if(parts.length == 2) {
            switch (parts[1]) {
                case "getTweet":
                    if(twitterClient.isDone())
                        return "SnapTwitter is not connected to the stream";
                    return twitterClient.getTweet();
                case "connect":
                    if(twitterClient.isDone())
                        twitterClient.connect();
                    return "OK";
                case "disconnect":
                    if(!twitterClient.isDone())
                        twitterClient.disconnect();
                    return "OK";
                case "getActiveFilters":
                    return twitterClient.getFilters();
                case "auth":
                    return TwitterAuth.getAuthURL();
                case "loginReady":
                    return "" + TwitterAuth.loginReady();
                case "endHelper":
                    SnapTwitter.endHelper();
                    return "OK";
                case "getBlockXML":
                    return SnapTwitter.loadBlockXML();
                case "getExampleXML":
                    return SnapTwitter.loadResourceAsString("de/an_gr/SnapTwitter/SnapTwitterExample.xml");
                case "getMap":
                    return SnapTwitter.loadResourceAsString("de/an_gr/SnapTwitter/mercator-map-base64.txt");
                case "loadSnap":
                    return "<html><head><title>SnapTwitter</title></head><body><p><strong>You will be redirected to Snap with the SnapTwitter blocks in a few seconds or by clicking <a href=\"" + (SnapTwitter.getProperty("snapURL") + "#open:http://" + SnapTwitter.getLocalAdress() + "/getBlockXML") + "\">here</a>.</strong></p><script type=\"text/javascript\">window.location.href=\"" + (SnapTwitter.getProperty("snapURL") + "#open:http://" + SnapTwitter.getLocalAdress() + "/getBlockXML") + "\";</script></body></html>.";
                case "loadExample":
                    return "<html><head><title>SnapTwitter</title></head><body><p><strong>You will be redirected to Snap with the SnapTwitter blocks in a few seconds or by clicking <a href=\"" + (SnapTwitter.getProperty("snapURL") + "#open:http://" + SnapTwitter.getLocalAdress() + "/getExampleXML") + "\">here</a>.</strong></p><script type=\"text/javascript\">window.location.href=\"" + (SnapTwitter.getProperty("snapURL") + "#open:http://" + SnapTwitter.getLocalAdress() + "/getExampleXML") + "\";</script></body></html>.";
            }
        }

        if(parts.length == 3) {
            switch (parts[1]) {
                case "setLanguageFilter":
                    return twitterClient.setLanguageFilter(parts[2]);
                case "setKeywordFilter":
                    return twitterClient.setKeywordFilter(parts[2]);
                case "setGeoFilter":
                    return twitterClient.setGeoFilter(parts[2]);
            }
        }

        return null;
    }

    private String twitterLogin(String c) {
        String[] parts = c.split("\\?");
        if(parts.length < 2)
            return "Login error";

        parts = parts[1].split("&");
        if(parts.length < 2)
            return "Login error";

        TwitterAuth.callback(parts[1].split("=")[1]);

        return "<html><head><title>SnapTwitter is ready</title></head><body><p><strong>SnapTwitter is ready. You can now close this window if it doesn't close automatically.</strong></p><script type=\"text/javascript\">window.close();</script></body></html>.";
    }
}
