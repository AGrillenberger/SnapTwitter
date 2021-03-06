package de.an_gr.SnapTwitter;

import de.an_gr.SnapTwitter.Debugger.Debugger;
import de.an_gr.SnapTwitter.HttpServer.HttpServer;
import de.an_gr.SnapTwitter.Twitter.TwitterAuth;
import de.an_gr.SnapTwitter.Twitter.TwitterClient;
import de.an_gr.SnapTwitter.Twitter.TwitterClientOffline;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Andreas on 03.03.2016.
 */
public class SnapTwitterMain {
    private JButton btnStartSnap, btnExitSnapTwitter;;
    private JPanel pnl;
    private JLabel lblHeading, lblMessage, lblProcessed, lblReceived, lblConnected, lblOnline;
    private static int fontSize = 14, windowWidth = 300, windowHeight = 300;

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

    public SnapTwitterMain() {
        btnStartSnap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launchSnap();
            }
        });
        btnExitSnapTwitter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                endHelper();
            }
        });
    }

    public static void main(String[] args) {
        fontSize = UIManager.getFont("Label.font").getSize() + 1;

        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        double scaling = dpi/96d;
        fontSize *= scaling;
        windowHeight *= scaling;
        windowWidth *= scaling;

        UIManager.put("Label.font", new FontUIResource(new Font("Dialog", Font.PLAIN, fontSize)));
        UIManager.put("Button.font", new FontUIResource(new Font("Dialog", Font.PLAIN, fontSize)));
        UIManager.put("TextField.font", new FontUIResource(new Font("Dialog", Font.PLAIN, fontSize)));

        JFrame frame = new JFrame("SnapTwitterMain");
        SnapTwitterMain stm = new SnapTwitterMain();
        frame.setContentPane(stm.pnl);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        frame.setSize(new Dimension(windowWidth, windowHeight));

        stm.lblHeading.setText("SnapTwitter loading...");
        stm.lblMessage.setText("");
        stm.lblConnected.setText("");
        stm.lblOnline.setText("");
        stm.lblReceived.setText("");
        stm.lblProcessed.setText("");

        frame.setVisible(true);

        if (loadBlockXML() == "") {
            return;
        }

        /* Running offline? */
        int runOffline = JOptionPane.showOptionDialog(frame, "Should SnapTwitter run online or offline?", "Online or Offline?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Online", "Offline"}, 0);
        System.err.print("" + runOffline);
        if (runOffline == 1) {
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
        if (Arrays.asList(args).contains("--createCache")) {
            createCache = true;
            cache = new ArrayList<>();
        }

        /* Initialize the HTTP Server and wait until it is ready*/
        httpServ = new HttpServer(twClient, Integer.parseInt(SnapTwitterMain.getProperty("port")));
        httpThread = new Thread(httpServ);
        httpThread.start();
        while (!httpServ.isReady()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Debugger.log("Main: http is ready");

        /* Generate authentication tokens if not running offline */
        if (!offline)
            TwitterAuth.prepareAuth();

        /* Start the Twitter client */
        twThread = new Thread(twClient);
        twThread.start();

        /* Define shutdown hook, especially needed for creating a cache */
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (twClient != null)
                    twClient.disconnect();

                if (createCache) {
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
        String headingTxt = "SnapTwitter is ready!";
        String messageTxt = "Start Snap using the button below or<br />open your browser with the following URL:<br />" +
                "http://" + getLocalAdress() + "/snap/start.html";

        if (createCache) {
            messageTxt += "<br /><br />Tweet cache is being generated";
        }

        stm.lblHeading.setText(headingTxt);
        stm.lblMessage.setText("<html>" + messageTxt + "<br /><br />SnapTwitter was not initialized yet in Snap.<br />Go to Snap and use the 'twitter: prepare' block.");

    /* Now launch snap! */
        /* Wait until snap! is running */
        synchronized (snapRunning) {
            try {
                snapRunning.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        stm.lblMessage.setText("SnapTwitter is now ready to use.");

    /* Generate and show some statistics */
        long startTime = new Date().getTime();
        long curTime = 0;
        while (true) {
            int nr = twClient.getNumRead(), ns = twClient.getNumStored();
            if (!twClient.isDone())
                curTime = new Date().getTime();

            stm.lblOnline.setText((twClient.isDone() ? "Offline" : "Online"));
            stm.lblReceived.setText("" + ns + " (" + ((float) ns / (curTime - startTime) * 1000) + "/s)");
            stm.lblProcessed.setText("" + nr + " (" + ((float) nr / (curTime - startTime) * 1000) + "/s)");

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
     *
     * @param msg
     */
    public static void writeToCache(String msg) {
        if (!createCache)
            return;

        cache.add(msg);
    }

    /**
     * Reads a message from the cache
     *
     * @return
     */
    public static String getCachedMessage() {
        if (cache == null)
            return null;

        String msg = "";
        do {
            msg = cache.get(0);
            cache.remove(0);
        } while(msg == "" || msg.length() < 100);

        return msg;
    }

    /**
     * Is the Twitter client online or offline?
     *
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
        String url = "http://" + getLocalAdress() + "/snap/start.html";//getProperty("snapURL") + "#open:http://" + getLocalAdress() + "/getBlockXML";

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
     *
     * @param name
     * @return
     */
    public static String getProperty(String name) {
        return getProperty(name, "de/an_gr/SnapTwitter/SnapTwitter.properties");
    }

    /**
     * Reads a value from a defined property file
     *
     * @param name
     * @param propertyFile
     * @return
     */
    public static String getProperty(String name, String propertyFile) {
        Properties properties = new Properties();
        try {
            ClassLoader cl = SnapTwitterMain.class.getClassLoader();
            InputStream is = cl.getResourceAsStream(propertyFile);
            if (is == null) {
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

        if (hostName == null || hostName.length() <= 1)
            hostName = "localhost";

        return hostName + ":" + SnapTwitterMain.getProperty("port");
    }

    public static String loadResourceAsString(String filename) {
        ClassLoader classLoader = SnapTwitterMain.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(filename);
        if (is == null)
            return null;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line;

        try {
            while ((line = br.readLine()) != null)
                sb.append(line);

            br.close();
            is.close();
        } catch (IOException e) {
        }

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
            if (addr != null && addr.length() > 1)
                blocks = blocks.replaceAll(addr, getLocalAdress() );
        }
        return blocks;
    }
}
