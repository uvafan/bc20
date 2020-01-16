package v2_better_turtle;

import battlecode.common.*;
import java.util.Random;

/*
    The first question of messaging is how to know that a message is from our team. The first 5 numbers of each message will be used for this purpose.
    If we're red, (The sum of the numbers in positions 0,1,3,4) % SECRET_NUM must equal the number in position 2.
    If we're blue, (The sum of the numbers in positions 0,2,3,4) % SECRET_NUM must equal the number in position 1.
    The sixth number is used for specifying message type.
    The last number is used for specifying the actual message.
 */


public class Comms {
    static Bot bot;
    static RobotController rc;
    public static int readRound;

    public static enum MessageType {
        HQ_LOC,
        SYMMETRY_RULED_OUT,
        REFINERY_LOC,
        SOUP_CLUSTER_LOC,
        OUR_NET_GUN_LOC,
        ENEMY_NET_GUN_LOC,
        DESIGN_SCHOOL_LOC,
        FULFILLMENT_CENTER_LOC,
        VAPORATOR_LOC,
    }

    static int SECRET_NUM = 155252936;

    public Comms(Bot b) {
        bot = b;
        rc = b.rc;
        readRound = 1;
    }

    public void readMessages() throws GameActionException {
        while(Clock.getBytecodesLeft() > 300 && readRound < bot.round) {
            Transaction[] transactions = rc.getBlock(readRound);
            for(Transaction t: transactions) {
                int[] msg = t.getMessage();
                if(verifyOurs(msg)) {
                    Utils.log("reading message from round " + readRound);
                    processMessage(msg);
                }
            }
            readRound++;
        }
    }

    private void processMessage(int[] msg) {
        switch(MessageType.values()[msg[5]]) {
            case HQ_LOC:
                bot.hqLoc = msgToLocation(msg[6]);
                break;
            case REFINERY_LOC:
                Utils.log("adding refinery");
                MapLocation rLoc = msgToLocation(msg[6]);
                bot.refineries[bot.numRefineries] = rLoc;
                bot.numRefineries++;
                break;
            case DESIGN_SCHOOL_LOC:
                Utils.log("adding design school");
                MapLocation dsLoc = msgToLocation(msg[6]);
                bot.designSchools[bot.numDesignSchools] = dsLoc;
                bot.numDesignSchools++;
                break;
            case SOUP_CLUSTER_LOC:
                Utils.log("adding soup cluster");
                MapLocation scLoc = msgToLocation(msg[6]);
                bot.soupClusters[bot.numSoupClusters] = scLoc;
                bot.numSoupClusters++;
                break;
        }
    }

    private boolean verifyOurs(int[] msg) {
        int e = 0;
        e += msg[0] % SECRET_NUM;
        e += msg[3] % SECRET_NUM;
        e += msg[4] % SECRET_NUM;
        switch(bot.us){
            case A:
                e += msg[1] % SECRET_NUM;
                return msg[2] == e;
            case B:
                e += msg[2] % SECRET_NUM;
                return msg[1] == e;
        }
        return false;
    }

    private int[] genFirstFive() {
        int[] message = new int[7];
        Random r = new Random();
        int e = 0;
        int a = r.nextInt(Integer.MAX_VALUE);
        e = a % SECRET_NUM;
        int b = r.nextInt(Integer.MAX_VALUE);
        e += b % SECRET_NUM;
        int c = r.nextInt(Integer.MAX_VALUE);
        e += c % SECRET_NUM;
        int d = r.nextInt(Integer.MAX_VALUE);
        e += d % SECRET_NUM;
        message[0] = a;
        message[3] = b;
        message[4] = c;
        switch(bot.us){
            case A:
                message[1] = d;
                message[2] = e;
                break;
            case B:
                message[2] = d;
                message[1] = e;
        }
        return message;
    }

    // Used for broadcasting all the location type messages
    public void broadcastLoc(MessageType mt, MapLocation loc) throws GameActionException {
        int[] message = genFirstFive();
        message[5] = mt.ordinal();
        message[6] = locationToMsg(loc);
        rc.submitTransaction(message, 1);
        Utils.log("broadcasting purpose " + mt + " loc " + loc.x + " " + loc.y);
    }

    public void broadcastSymRuledOut(Bot.Symmetry s) {
        int[] message = genFirstFive();
        message[5] = MessageType.SYMMETRY_RULED_OUT.ordinal();
        message[6] = s.ordinal();
    }

    private int locationToMsg(MapLocation m) {
        return SECRET_NUM * 11 + (m.x + m.y * 64);
    }

    private MapLocation msgToLocation(int msg) {
        return new MapLocation((msg % SECRET_NUM) % 64, (msg % SECRET_NUM) / 64);
    }


}
