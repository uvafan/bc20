package v2_better_turtle;

import battlecode.common.*;

public class Bot {
    public static RobotController rc;
    public static RobotType type;
    public static Team enemy;
    public static Team us;
    public static MapLocation hqLoc;
    public static MapLocation here;
    public static Comms comms;
    public static int mapHeight;
    public static int mapWidth;
    public static MapLocation[] enemyHqLocPossibilities;
    public static Symmetry[] symmetryPossibilities;
    public static MapLocation[] refineries;
    public static int numRefineries;
    public static MapLocation[] designSchools;
    public static int numDesignSchools;
    public static MapLocation[] soupClusters;
    public static int numSoupClusters;
    public static boolean[] invalidCluster;

    public static enum Symmetry {
        VERTICAL,
        HORIZONTAL,
        ROTATIONAL,
    }

    public static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    public static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
    public static RobotInfo[] knownEnemyNetGuns = {};
    public static RobotInfo[] nearbyEnemyDrones = {};
    public static int turnCount = 0;
    public static int numMiners = 0;
    public static int round = 0;

    public Bot() {
        return;
    }

    public Bot(RobotController r) throws GameActionException {
        rc = r;
        type = rc.getType();
        us = rc.getTeam();
        enemy = rc.getTeam().opponent();
        comms = new Comms(this);
        mapHeight = rc.getMapHeight();
        mapWidth = rc.getMapWidth();
        round = rc.getRoundNum() - 1;
        refineries = new MapLocation[100];
        numRefineries = 0;
        designSchools = new MapLocation[100];
        numDesignSchools = 0;
        soupClusters = new MapLocation[500];
        invalidCluster = new boolean[500];
        numSoupClusters = 0;
    }

    public void takeTurn() throws GameActionException {
        turnCount++;
        round++;
    }

    static boolean nearbyRobot(RobotType target) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo r : robots) {
            if(r.getType() == target) {
                return true;
            }
        }
        return false;
    }

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

/*
    static MapLocation[] getLocationsWithinSensorRad() {
        int sensorRad = rc.getCurrentSensorRadiusSquared();
        Utils.log("sensorRad: " + sensorRad);
        MapLocation[] ret = new MapLocation[1000];
        int idx = 0;
        for(int i = 0; i*i+i*i <= sensorRad; i++)
            for(int j = i; i*i+j*j <= sensorRad; j++) {
                ret[idx] = here.translate(i,j);
                idx++;
                if(i > 0 && j > 0) {
                    ret[idx] = here.translate(-i, -j);
                    idx++;
                }
                if(i > 0) {
                    ret[idx] = here.translate(-i,j);
                    idx++;
                }
                if(j > 0){
                    ret[idx] = here.translate(i,-j);
                    idx++;
                }
                if(i==j)
                    continue;
                int temp = i;
                i = j;
                j = temp;
                ret[idx] = here.translate(i,j);
                idx++;
                if(i > 0 && j > 0) {
                    ret[idx] = here.translate(-i, -j);
                    idx++;
                }
                if(i > 0) {
                    ret[idx] = here.translate(-i,j);
                    idx++;
                }
                if(j > 0){
                    ret[idx] = here.translate(i,-j);
                    idx++;
                }
            }
        ret[idx] = null;
        return ret;
    }*/

}
