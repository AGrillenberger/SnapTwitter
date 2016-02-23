package de.an_gr.SnapTwitter.Twitter;

import de.an_gr.SnapTwitter.SnapTwitter;

/**
 * Created by Andreas on 10.02.2016.
 */
public class TwitterClientOffline extends TwitterClient {

    private int numRead = 0;

    public String getTweet() {
        numRead++;
        return SnapTwitter.getCachedMessage();
    }

    public int getNumRead() {
        return numRead;
    }

    // make the following methods do nothing
    public void connect() {
        // do nothing
    }

    public void disconnect() {
        // do nothing
    }

    private void createFilterEndpoint() {
        // do nothing
    }

    private void createSampleEndpoint() {
        // do nothing
    }

    public boolean isDone() {
        return false;
    }

    public String setLanguageFilter(String str) {
        return null;
    }

    public String setKeywordFilter(String str) {
        return null;
    }

    public String setGeoFilter(String str) {
        return null;
    }

    public String getFilters() {
        return "Running in offline mode, no filters active";
    }
}
