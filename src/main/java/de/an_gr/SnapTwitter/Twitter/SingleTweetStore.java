package de.an_gr.SnapTwitter.Twitter;

import de.an_gr.SnapTwitter.SnapTwitterMain;

import java.util.concurrent.Semaphore;

/**
 * Class for storing only one tweet at a time.
 * @author Andreas Grillenberger, FAU
 */
public class SingleTweetStore {

    private String msgStore = "";
    private final Semaphore lock = new Semaphore(1, true);
    private int numStored = 0, numRead = 0;

    public void set(String msg) {
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        msgStore = msg;
        SnapTwitterMain.writeToCache(msg);
        if(msg != null)
            numStored++;
        lock.release();
    }

    public String get() {
        String msg = msgStore;
        if(msg != null)
            numRead++;
        set(null);
        return msg;
    }

    public int getNumStored() {
        return numStored;
    }

    public int getNumRead() {
        return numRead;
    }

    public void reset() {
        numStored = 0;
        numRead = 0;
        msgStore = null;
    }
}
