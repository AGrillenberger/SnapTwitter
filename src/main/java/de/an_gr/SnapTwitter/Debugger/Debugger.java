package de.an_gr.SnapTwitter.Debugger;

/**
 * Created by Andreas on 03.01.2016.
 */
public class Debugger {
    static boolean enabled = false;

    public static void log(String s) {
        if(enabled)
            System.err.println(s);
    }

    public static void enable() { enabled = true; }
    public static void disable() { enabled = false; }
}
