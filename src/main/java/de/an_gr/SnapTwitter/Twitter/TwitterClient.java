package de.an_gr.SnapTwitter.Twitter;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.*;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import de.an_gr.SnapTwitter.Debugger.Debugger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andreas on 03.01.2016.
 */
public class TwitterClient implements Runnable {
    Client hosebirdClient;
    List<String> terms, langs;
    List<Location> geo;
    boolean onlyWithGeo = true;

    SingleTweetStore tweetStore = new SingleTweetStore();

    public void run() {
    }

    public void connect() {
        Debugger.log("twClient: Connect called");

        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);

        StreamingEndpoint hosebirdEndpoint;
        if (terms == null && langs == null && geo == null && !onlyWithGeo)
            hosebirdEndpoint = createSampleEndpoint();
        else
            hosebirdEndpoint = createFilterEndpoint();

        Authentication hosebirdAuth = new OAuth1(
                TwitterAuth.consumerKey,
                TwitterAuth.consumerSecret,
                TwitterAuth.accessTokenKey,
                TwitterAuth.accessTokenSecret
        );

        tweetStore.reset();

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new SingleTweetProcessor(tweetStore));

        hosebirdClient = builder.build();
        hosebirdClient.connect();
    }

    private StreamingEndpoint createFilterEndpoint() {
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        if(terms != null)
            hosebirdEndpoint.trackTerms(terms);
        if(langs != null)
            hosebirdEndpoint.languages(langs);
        if(geo != null)
            hosebirdEndpoint.locations(geo);
        else if (geo == null && onlyWithGeo)
            hosebirdEndpoint.locations(Lists.newArrayList(
                    new Location(
                            new Location.Coordinate(-179.99,-89.99),
                            new Location.Coordinate(179.99,89.99)
                    )));

        return hosebirdEndpoint;
    }

    private StreamingEndpoint createSampleEndpoint() {
        StatusesSampleEndpoint hosebirdEndpoint = new StatusesSampleEndpoint();

        return hosebirdEndpoint;
    }

    public void disconnect() {
        Debugger.log("twClient: Disconnect called");
        if(hosebirdClient != null)
            hosebirdClient.stop();
    }

    /**
     * Checks if streamer is running
     * @return
     */
    public boolean isDone() {
        if(hosebirdClient == null || hosebirdClient.isDone())
            return true;
        return false;
    }

    /**
     * Gets one Tweet from stream
     * @return
     */
    public String getTweet() {
        Debugger.log("twClient: GetTweet called");
        while (!isDone()) {
            String msg = tweetStore.get();
            return msg;
        }
        return null;
    }

    public String setLanguageFilter(String str) {
        Debugger.log("twClient: setLanguageFilter called (" + str + ")");
        if(!isDone())
            return "ERR: Cannot set filter while running";

        if(str == null || str == "" || str.startsWith("none")) {
            langs = null;
            return "OK: Removed language filter";
        }

        langs = Lists.newArrayList(str.split(","));
        System.out.println(langs);
        return "OK: Set language filter: " + str;
    }

    public String setKeywordFilter(String str) {
        Debugger.log("twClient: setKeywordFilter called (" + str + ")");
        if(!isDone())
            return "ERR: Cannot set filter while running";

        if(str == null || str == "" || str.startsWith("none")) {
            terms = null;
            return "OK: Removed keyword filter";
        }

        terms = Lists.newArrayList(str.split(","));
        return "OK: Set keyword filter: " + str;
    }

    /**
     *
     * @param str [southWestLat|southWestLong|northEastLat|northEastLong],[southWestLat|southWestLong|northEastLat|northEastLong]
     * @return
     */
    public String setGeoFilter(String str) {
        Debugger.log("twClient: setGeoFilter called (" + str + ")");
        if(!isDone())
            return "ERR: Cannot set filter while running";

        if(str == null || str == "" || str.startsWith("none")) {
            geo = null;
            onlyWithGeo = false;
            return "OK: Removed geo filter";
        }

        if(str.compareTo("allWithGeo") == 0) {
            onlyWithGeo = true;
            return "OK: Set location filter: " + str;
        }

        String[] areaStrings = str.split(",");
        ArrayList<Location> locs = new ArrayList<Location>();

        for(String s : areaStrings) {
            String[] sl = s.split("|");
            locs.add(new Location(
                    new Location.Coordinate(Integer.parseInt(sl[0]), Integer.parseInt(sl[1])),
                    new Location.Coordinate(Integer.parseInt(sl[2]), Integer.parseInt(sl[3]))
            ));
        }

        geo = locs;
        return "OK: Set location filter: " + str;
    }

    public String getFilters() {
        Debugger.log("twClient: getFilters called");
        String ret = "Languages: ";
        if(langs == null)
            ret += "none";
        else
            ret += String.join(", ", langs);

        ret += "\r\n";
        ret += "Keywords: ";
        if(terms == null)
            ret += "none";
        else
            ret += String.join(", ", terms);

        ret += "\r\n";
        ret += "Geo: ";
        if(geo == null && onlyWithGeo)
            ret += "all with geo information";
        else if(geo == null)
            ret += "none";
        else {
            String[] locs = new String[geo.size()];
            int cur = 0;
            for(Location l : geo) {
                locs[cur++] += "[" + l.southwestCoordinate().latitude() + "|" + l.southwestCoordinate().longitude() + l.northeastCoordinate().latitude() + l.northeastCoordinate().longitude() + "]";
            }
            ret += String.join(", ", locs);
        }

        return ret;
    }

    public int getNumStored() {
        return tweetStore.getNumStored();
    }

    public int getNumRead() {
        return tweetStore.getNumRead();
    }
}
