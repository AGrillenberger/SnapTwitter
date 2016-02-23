package de.an_gr.SnapTwitter.Twitter;

import de.an_gr.SnapTwitter.Debugger.Debugger;
import de.an_gr.SnapTwitter.SnapTwitter;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Created by Andreas on 04.02.2016.
 */
public class TwitterAuth {
    static String consumerKey, consumerSecret;
    static String
            requestTokenKey = null,
            requestSecret = null,
            oAuthVerifier = null,
            accessTokenKey = null,
            accessTokenSecret = null,
            authURL = null;
    static RequestToken rqt = null;

    private static boolean loginReady = false;

    public static void prepareAuth() {
        if(SnapTwitter.isOffline() || loginReady()) {
            return;
        }

        consumerKey = SnapTwitter.getProperty("consumerKey", "de/an_gr/SnapTwitter/apikeys.properties");
        consumerSecret = SnapTwitter.getProperty("consumerSecret", "de/an_gr/SnapTwitter/apikeys.properties");

        Twitter twc = TwitterFactory.getSingleton();
        twc.setOAuthConsumer(consumerKey, consumerSecret);

        try {
            rqt = twc.getOAuthRequestToken("http://" + SnapTwitter.getLocalAdress() + "/authCallback");
            requestTokenKey = rqt.getToken();
            requestSecret = rqt.getTokenSecret();
            authURL = rqt.getAuthenticationURL();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    public static void callback(String verifier) {
        if(!loginReady()) {
            oAuthVerifier = verifier;

            Twitter twc = TwitterFactory.getSingleton();
            try {
                AccessToken accessToken = twc.getOAuthAccessToken(rqt, verifier);
                accessTokenKey = accessToken.getToken();
                accessTokenSecret = accessToken.getTokenSecret();
            } catch (TwitterException e) {
                e.printStackTrace();
            }

            loginReady = true;
        }

        synchronized(SnapTwitter.snapRunning) {
            SnapTwitter.snapRunning.notify();
        }
    }

    public static boolean loginReady() {
        if(SnapTwitter.isOffline())
            synchronized(SnapTwitter.snapRunning) {
                SnapTwitter.snapRunning.notify();
            }
        return loginReady | SnapTwitter.isOffline();
    }

    public static String getAuthURL() {
        if(SnapTwitter.isOffline() | loginReady())
            return "//NOAUTH//";
        Debugger.log("TwitterAuth: authURL is " + authURL);
        return authURL;
    }
}
