package v7_seeding;

import battlecode.common.*;

/*
    The first question of messaging is how to know that a message is from our team. The first 5 numbers of each message will be used for this purpose.
    If we're red, (The sum of the numbers in positions 0,1,3,4) % SECRET_NUM must equal the number in position 2.
    If we're blue, (The sum of the numbers in positions 0,2,3,4) % SECRET_NUM must equal the number in position 1.
    The sixth number is used for specifying message type.
    The last number is used for specifying the actual message.
    
    new system:
    1st int is a dumb hash
    2nd int is a smarter and slower hash
    everything else is the message
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
        UNIT_CREATED,
    }

    public Comms(Bot b) {
        bot = b;
        rc = b.rc;
        readRound = 1;
    }

    public void readMessages() throws GameActionException {
        readMessages(bot.round - 1);
    }

    public void readMessages(int upUntil) throws GameActionException {
        while(Clock.getBytecodesLeft() > 300 && readRound <= upUntil) {
            Transaction[] transactions = rc.getBlock(readRound);
            for(Transaction t: transactions) {
                int[] msg = t.getMessage();
                if(verifyOurs(msg, readRound)) {
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
                bot.refineries[bot.unitCounts[RobotType.REFINERY.ordinal()]] = rLoc;
                bot.unitCounts[RobotType.REFINERY.ordinal()]++;
                break;
            case DESIGN_SCHOOL_LOC:
                Utils.log("adding design school");
                MapLocation dsLoc = msgToLocation(msg[6]);
                bot.designSchools[bot.unitCounts[RobotType.DESIGN_SCHOOL.ordinal()]] = dsLoc;
                bot.unitCounts[RobotType.DESIGN_SCHOOL.ordinal()]++;
                break;
            case OUR_NET_GUN_LOC:
                Utils.log("adding out net gun loc");
                MapLocation ngLoc = msgToLocation(msg[6]);
                bot.unitCounts[RobotType.NET_GUN.ordinal()]++;
                break;
            case FULFILLMENT_CENTER_LOC:
                Utils.log("adding fc");
                MapLocation fcLoc = msgToLocation(msg[6]);
                bot.unitCounts[RobotType.FULFILLMENT_CENTER.ordinal()]++;
                break;
            case SOUP_CLUSTER_LOC:
                Utils.log("adding soup cluster");
                MapLocation scLoc = msgToLocation(msg[6]);
                bot.soupClusters[bot.numSoupClusters] = scLoc;
                bot.numSoupClusters++;
                break;
            case UNIT_CREATED:
                bot.unitCounts[msg[6]-MagicConstants.ORDINAL_SECRET_NUM]++;
        }
    }
    private int smear(int hashCode) {
        hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
        return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
      }
    private int generateHash(int[] msg) {
    	int hash = msg[2]^msg[3]^msg[4]^msg[5]^msg[6]^bot.round;
    	//Utils.log("The generate round is: " + bot.round);
    	return smear(hash);
    }
    private int verifyHash(int[] msg, int round) {
    	int hash = msg[2]^msg[3]^msg[4]^msg[5]^msg[6]^round;
    	return smear(hash);
    }
    private boolean verifyOurs(int[] msg, int round) {
    	if(msg[0]!= MagicConstants.FAST_SECRET_NUM+bot.us.ordinal())
    		return false;
    	//Utils.log("Trying to veryify a hash from round: " + round);
    	return msg[1]==verifyHash(msg, round);
    }

    // Used for broadcasting all the location type messages
    public void broadcastLoc(MessageType mt, MapLocation loc) throws GameActionException {
        if(rc.getTeamSoup() == 0)
            return;
        int[] message = new int[7];
        message[5] = mt.ordinal();
        message[6] = locationToMsg(loc);
        message[0] = MagicConstants.FAST_SECRET_NUM+bot.us.ordinal();
        message[1] = generateHash(message);
        rc.submitTransaction(message, 1);
        Utils.log("broadcasting purpose " + mt + " loc " + loc.x + " " + loc.y);
    }

    public void broadcastUnitCreated(RobotType rt) throws GameActionException {
        if(rc.getTeamSoup() == 0)
            return;
        int[] message = new int[7];
        message[5] = MessageType.UNIT_CREATED.ordinal();
        message[6] = rt.ordinal()+MagicConstants.ORDINAL_SECRET_NUM;
        message[0] = MagicConstants.FAST_SECRET_NUM+bot.us.ordinal();
        message[1] = generateHash(message);
        rc.submitTransaction(message, 1);
    }

    public void broadcastCreation(RobotType rt, MapLocation loc) throws GameActionException {
        switch(rt) {
            case DESIGN_SCHOOL: broadcastLoc(MessageType.DESIGN_SCHOOL_LOC, loc); break;
            case NET_GUN: broadcastLoc(MessageType.OUR_NET_GUN_LOC, loc); break;
            case REFINERY: broadcastLoc(MessageType.REFINERY_LOC, loc); break;
            case FULFILLMENT_CENTER: broadcastLoc(MessageType.FULFILLMENT_CENTER_LOC, loc); break;
            default: broadcastUnitCreated(rt);
        }
    }

    private int locationToMsg(MapLocation m) {
        return MagicConstants.LOCATION_SECRET_NUM * 11 + (m.x + m.y * 64);
    }

    private MapLocation msgToLocation(int msg) {
        return new MapLocation((msg % MagicConstants.LOCATION_SECRET_NUM) % 64, (msg % MagicConstants.LOCATION_SECRET_NUM) / 64);
    }


}
