package de.an_gr.SnapTwitter;


import de.an_gr.SnapTwitter.Debugger.Debugger;
import de.an_gr.SnapTwitter.HttpServer.HttpServer;
import de.an_gr.SnapTwitter.Twitter.TwitterAuth;
import de.an_gr.SnapTwitter.Twitter.TwitterClient;
import de.an_gr.SnapTwitter.Twitter.TwitterClientOffline;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SnapTwitter Main Class
 *
 * @author Andreas Grillenberger, FAU
 */

public class SnapTwitter {
    /* References to the parts of the helper and the running threads */
    static TwitterClient twClient = null;
    static HttpServer httpServ = null;
    static Thread httpThread = null, twThread = null;

    /* Used for offline version */
    static boolean createCache = false;
    static List<String> cache;
    static private Boolean offline = false;

    /* Stores whether snap! is connected to the helper */
    static public Boolean snapRunning = false;

    /**
     * Main function
     * @param args --offline: get tweets from cache; --createCache: build new cache file
     */
    public static void main(String[] args) {
        if(loadBlockXML() == "") {
            return;
        }

        /* Running offline? */
        if(Arrays.asList(args).contains("--offline")) {
            offline = true;
            twClient = new TwitterClientOffline();

            FileInputStream fin = null;
            try {
                fin = new FileInputStream("tweetCache.ser");
                ObjectInputStream ois = new ObjectInputStream(fin);
                cache = (ArrayList<String>) ois.readObject();
                ois.close();
                fin.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            twClient = new TwitterClient();
        }

        /* create cache if --createCache is set */
        if(Arrays.asList(args).contains("--createCache")) {
            createCache = true;
            cache = new ArrayList<>();
        }

        /* Initialize the HTTP Server and wait until it is ready*/
        httpServ = new HttpServer(twClient, Integer.parseInt(SnapTwitter.getProperty("port")));
        httpThread = new Thread(httpServ);
        httpThread.start();
        while(!httpServ.isReady()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Debugger.log("Main: http is ready");

        /* Generate authentication tokens if not running offline */
        if(!offline)
            TwitterAuth.prepareAuth();

        /* Start the Twitter client */
        twThread = new Thread(twClient);
        twThread.start();

        /* Define shutdown hook, especially needed for creating a cache */
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if(twClient != null)
                    twClient.disconnect();

                if(createCache) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        FileOutputStream fout = new FileOutputStream("tweetCache.ser", false);
                        ObjectOutputStream oos = new ObjectOutputStream(fout);
                        oos.writeObject(cache);
                        oos.close();
                        fout.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println();
                    System.out.println("Cache file was written.");
                }
            }
        }, "CleanUp-thread"));

        /* Output for the user */
        System.out.println("----------------------------");
        System.out.println("--- SnapTwitter is ready ---");
        System.out.println("----------------------------");
        System.out.println();
        System.out.println("If Snap! does not start automatically, open your browser with the URL: http://snap.berkeley.edu/snapsource/snap.html#open:http://" + getLocalAdress() + "/getBlockXML");
        System.out.println();

        if(createCache) {
            System.out.println("Tweet cache is being generated");
            System.out.println("");
        }

        /* Now launch snap! */
        launchSnap();

        System.out.print("SnapTwitter was not initialized yet in Snap. Go to Snap and use the 'twitter: prepare' block.");

        /* Wait until snap! is running */
        synchronized (snapRunning) {
            try {
                snapRunning.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\rTo exit the SnapTwitter helper, use the corresponding block in Snap! or press CTRL+C.");
        System.out.println();

        /* Generate and show some statistics */
        long startTime = new Date().getTime();
        long curTime = 0;
        while(true) {
            int nr = twClient.getNumRead(), ns = twClient.getNumStored();
            if(!twClient.isDone())
                curTime = new Date().getTime();

            System.out.format("\rcurrently %sconnected to twitter | tweets received: %5d (%3.2f/s) | tweets read: %5d (%3.2f/s)",
                    (twClient.isDone() ? "not " : ""),
                    ns,
                    (float) ns / (curTime - startTime) * 1000,
                    nr,
                    (float) nr / (curTime - startTime) * 1000
            );

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ends the helper application
     */
    public static void endHelper() {
        System.exit(0);
    }

    /**
     * Writes a message to the cache
     * @param msg
     */
    public static void writeToCache(String msg) {
        if(!createCache)
            return;

        cache.add(msg);
    }

    /**
     * Reads a message from the cache
     * @return
     */
    public static String getCachedMessage() {
        if(cache == null)
            return null;
        String msg = cache.get(0);
        cache.remove(0);

        return msg;
    }

    /**
     * Is the Twitter client online or offline?
     * @return
     */
    public static boolean isOffline() {
        return offline;
    }

    /**
     * Launch Snap! in the default Browser
     */
    public static void launchSnap() {
        Runtime rt = Runtime.getRuntime();
        String url = getProperty("snapURL") + "#open:http://" + getLocalAdress() + "/getBlockXML";

        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.indexOf("win") >= 0) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.indexOf("mac") >= 0) {
                rt.exec("open " + url);
            } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
                rt.exec("xdg-open " + url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a value from the default property file
     * @param name
     * @return
     */
    public static String getProperty(String name) {
        return getProperty(name, "de/an_gr/SnapTwitter/SnapTwitter.properties");
    }

    /**
     * Reads a value from a defined property file
     * @param name
     * @param propertyFile
     * @return
     */
    public static String getProperty(String name, String propertyFile) {
        Properties properties = new Properties();
        try {
                ClassLoader cl = SnapTwitter.class.getClassLoader();
                InputStream is = cl.getResourceAsStream(propertyFile);
                if(is == null) {
                    System.err.println("Properties file not found: " + propertyFile);
                    System.exit(1);
                }
                BufferedInputStream stream = new BufferedInputStream(is);
                properties.load(stream);
                stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties.getProperty(name);
    }

    public static String getLocalAdress() {
        String hostName, canonicalHostName;
        try {
             hostName = InetAddress.getLocalHost().getHostName();
             canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostName = "localhost";
            canonicalHostName = "localhost";
        }

        if(hostName == null || hostName.length() <= 1)
            hostName = "localhost";

        return hostName + ":" + SnapTwitter.getProperty("port");
    }

    public static String loadResourceAsString(String filename) {
        ClassLoader classLoader = SnapTwitter.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(filename);
        if(is == null)
            return null;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line;

        try {
            while((line = br.readLine()) != null)
                sb.append(line);

            br.close();
            is.close();
        } catch (IOException e) { }

        return sb.toString();
    }

    public static String loadBlockXML() {
        String blocks = loadResourceAsString("de/an_gr/SnapTwitter/SnapTwitterBlocks.xml");

        // pattern for matching <block-definition s="twitter: helper url" type="reporter" category="other"><header></header><code></code><inputs></inputs><script><block s="doReport"><l>ADDRESS:PORT/</l>
        String regex = "(.*)twitter:\\shelper\\surl[^>]*><header></header><code></code><inputs></inputs><script><block\\ss=\"doReport\"><l>([^<\\/]*)";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(blocks);

        if (m.find()) {
            String addr = m.group(2);
            if(addr != null && addr.length() > 1)
                blocks = blocks.replaceAll(addr, getLocalAdress());
        }

        return blocks;
    }
}
