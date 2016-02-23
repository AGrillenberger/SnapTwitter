package de.an_gr.SnapTwitter.Twitter;

import com.twitter.hbc.core.processor.StringDelimitedProcessor;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * Class for processing a single tweet.
 * @author Andreas Grillenberger, FAU
 */
public class SingleTweetProcessor extends StringDelimitedProcessor {

    private SingleTweetStore tweetStore;

    public SingleTweetProcessor(BlockingQueue<String> queue) {
        super(queue);
    }

    public SingleTweetProcessor(BlockingQueue<String> queue, long offerTimeoutMillis) {
        super(queue, offerTimeoutMillis);
    }

    public SingleTweetProcessor(SingleTweetStore ts) {
        super(null);
        tweetStore = ts;
    }

    /**
     * Processes the message by just storing it in the tweetStore
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean process() throws IOException, InterruptedException {
        String msg = processNextMessage();
        while (msg == null)
            msg = processNextMessage();

        tweetStore.set(msg);
        return true;
    }

}
