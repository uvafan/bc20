package v2_better_turtle;

import battlecode.common.Clock;

public class Utils {

    public static boolean DEBUG = true;
    public static boolean DEBUG_BYTECODE = true;

    public static void log(String s){
        if(DEBUG) {
            System.out.println(s);
            if(DEBUG_BYTECODE){
                System.out.println(Clock.getBytecodesLeft() + " bytecodes left.");
            }
        }
    }

}
