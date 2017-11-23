package de.an_gr.SnapTwitter.HttpServer;

import de.an_gr.SnapTwitter.Debugger.Debugger;
import de.an_gr.SnapTwitter.SnapTwitterMain;
import de.an_gr.SnapTwitter.Twitter.TwitterAuth;
import de.an_gr.SnapTwitter.Twitter.TwitterClient;

import java.io.*;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

import org.json.*;

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
        if (line == null)
            return;
        StringTokenizer s = new StringTokenizer(line);

        String mode = s.nextToken();
        String command = s.nextToken();
        Debugger.log("HTTP Command: " + command);

        if(!command.startsWith("/snap/")) {
            String answer = runCommand(command);

            if (answer == null) {
                out("HTTP/1.0 404 Not Found" + CRLF);
                out("text/plain" + CRLF);
                out(CRLF);
                out("Unknown command" + CRLF);
            }
            else {
                out("HTTP/1.0 200 OK" + CRLF);
                out("Access-Control-Allow-Origin: *" + CRLF);
                out("Content-Type: text/html; charset=utf-8" + CRLF);
                out(CRLF);
                out(answer);
            }
        } else {
            switch (mode) {
                case "GET":
                    getCommand(command);
                    break;
                case "POST":
                    postCommand(command);
                    break;
            }
        }

        os.close();
        br.close();
        sock.close();
    }

    private void getCommand(String command) throws IOException {
        String fn = command.substring(5);
        Debugger.log("Reading file " + fn);
        Debugger.log("PWD: " + (new java.io.File(".").getCanonicalPath()));

        Path path = FileSystems.getDefault().getPath("resources", fn);
        if(!Files.exists(path)) {
            Debugger.log("file " + fn + " does not exist.");
            out("HTTP/1.0 404 Not Found" + CRLF);
            out("text/plain" + CRLF);
            out(CRLF);
            out("File not found" + CRLF);
        } else {
            byte[] filearray = Files.readAllBytes(path);

            Debugger.log("Result is " + filearray.length + " bytes long");

            out("HTTP/1.0 200 OK" + CRLF);
            out("Access-Control-Allow-Origin: *" + CRLF);
            out("Content-Type: " + Files.probeContentType(path) + "; charset=utf-8" + CRLF);
            out(CRLF);
            os.write(filearray);
        }
    }

    private void postCommand(String command) throws IOException {
        Debugger.log("POST COMMAND");
        String data = "";
        while (br.ready())
            data += (char)br.read();

        data = data.substring(data.indexOf("\r\n\r\n")+4);

        String[] parts = command.split("/");

        String res = "";
        if(parts.length == 4 && parts[2].equals("getAttrib")) {
            if(parts[3].contains(".")) {
                Debugger.log("MULTIPLE ATTRIBUTES FOR getAttrib: " + parts[3]);
                String[] attribs = parts[3].split("\\.");
                Debugger.log("Attributes: " + java.util.Arrays.toString(attribs));
                String curData = data;
                for(String attrib : attribs) {
                    Debugger.log("Attribute: " + attrib);
                    curData = getAttribute(attrib, curData);
                }
                res = curData;
            } else {
                res = getAttribute(parts[3], data);
            }
        }

        out("HTTP/1.0 200 OK" + CRLF);
        out("Access-Control-Allow-Origin: *" + CRLF);
        out("Content-Type: text/plain;" + CRLF);
        out(CRLF);
        out(res);
    }

    private void out(String s) throws IOException {
        os.write(s.getBytes());
    }

    private String runCommand(String c) throws IOException {
        Debugger.log("RequestHandler: Command: " + c);
        String[] parts = c.split("/");

        if(parts.length < 2)
            return "<html><head><title>SnapTwitter</title></head><body><p><strong>You will be redirected to Snap with the SnapTwitter blocks in a few seconds or by clicking <a href=\"" + (SnapTwitterMain.getProperty("snapURL") + "#open:http://" + SnapTwitterMain.getLocalAdress() + "/getBlockXML") + "\">here</a>.</strong></p><script type=\"text/javascript\">window.location.href=\"" + (SnapTwitterMain.getProperty("snapURL") + "#open:http://" + SnapTwitterMain.getLocalAdress() + "/getBlockXML") + "\";</script></body></html>.";

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
                    SnapTwitterMain.endHelper();
                    return "OK";
                case "getBlockXML":
                    return SnapTwitterMain.loadBlockXML();
                case "getExampleXML":
                    return SnapTwitterMain.loadResourceAsString("de/an_gr/SnapTwitter/SnapTwitterExample.xml");
                case "getMap":
                    return SnapTwitterMain.loadResourceAsString("de/an_gr/SnapTwitter/mercator-map-base64.txt");
                case "loadSnap":
                    return "<html><head><title>SnapTwitter</title></head><body><p><strong>You will be redirected to Snap with the SnapTwitter blocks in a few seconds or by clicking <a href=\"" + (SnapTwitterMain.getProperty("snapURL") + "#open:http://" + SnapTwitterMain.getLocalAdress() + "/getBlockXML") + "\">here</a>.</strong></p><script type=\"text/javascript\">window.location.href=\"" + (SnapTwitterMain.getProperty("snapURL") + "#open:http://" + SnapTwitterMain.getLocalAdress() + "/getBlockXML") + "\";</script></body></html>.";
                case "loadExample":
                    return "<html><head><title>SnapTwitter</title></head><body><p><strong>You will be redirected to Snap with the SnapTwitter blocks in a few seconds or by clicking <a href=\"" + (SnapTwitterMain.getProperty("snapURL") + "#open:http://" + SnapTwitterMain.getLocalAdress() + "/getExampleXML") + "\">here</a>.</strong></p><script type=\"text/javascript\">window.location.href=\"" + (SnapTwitterMain.getProperty("snapURL") + "#open:http://" + SnapTwitterMain.getLocalAdress() + "/getExampleXML") + "\";</script></body></html>.";
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

    private String getAttribute(String attr, String data) {
        Debugger.log("Get Attribute");

        JSONObject obj = null;

        try {
            obj = new JSONObject(data);
        } catch(JSONException e) {
            return e.getMessage() + CRLF + "data was: " + data;
        }
        Debugger.log("JSON: " + obj.toString());

        String res = "";
        try {
            res = obj.getString(attr);
        } catch(org.json.JSONException e) {
            try {
                res = obj.get(attr).toString();
            } catch(org.json.JSONException e2) {
                return e2.getMessage();
            }
        }

        Debugger.log("Result: " + res);
        return res;
    }
}
