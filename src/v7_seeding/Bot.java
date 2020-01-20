package v7_seeding;

import battlecode.common.*;

import java.util.Random;

public class Bot {
    public static RobotController rc;
    public static boolean rushing = false;
    public static RobotType type;
    public static Team enemy;
    public static Team us;
    public static MapLocation hqLoc;
    public static MapLocation enemyHQLoc;
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
    public static Direction lastExploreDir;
    public static Random rand;
    public static MapLocation targetLoc;
    public static int[] unitCounts = {0,0,0,0,0,0,0,0,0,0};
    Strategy strat;
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
    public static MapLocation center;
    public static RobotInfo[] enemies;
    public static RobotInfo[] friends;

    public Bot() {
        return;
    }

    public Bot(RobotController r) throws GameActionException {
        rc = r;
        here = rc.getLocation();
        type = rc.getType();
        us = rc.getTeam();
        enemy = rc.getTeam().opponent();
        enemies = rc.senseNearbyRobots(-1, enemy);
        friends = rc.senseNearbyRobots(-1, us);
        comms = new Comms(this);
        mapHeight = rc.getMapHeight();
        mapWidth = rc.getMapWidth();
        center = new MapLocation(mapWidth / 2, mapHeight / 2);
        round = rc.getRoundNum();
        if(round > 1) {
            comms.readMessages(1);
        }
        refineries = new MapLocation[100];
        designSchools = new MapLocation[100];
        soupClusters = new MapLocation[500];
        invalidCluster = new boolean[500];
        numSoupClusters = 0;
        strat = new Rush(this);
        for(RobotInfo e: enemies) {
            if (e.type == RobotType.HQ) {
                enemyHQLoc = e.location;
                break;
            }
        }
        for(RobotInfo e: friends) {
            if (e.type == RobotType.HQ) {
                hqLoc = e.location;
                break;
            }
        }
        if(hqLoc != null) {
            initializeEnemyHQLocs();
        }
   }

    public void takeTurn() throws GameActionException {
        turnCount++;
        round = rc.getRoundNum();
        enemies = rc.senseNearbyRobots(-1, enemy);
        friends = rc.senseNearbyRobots(-1, us);
        if(round - 1 == comms.readRound)
            comms.readMessages();
    }

    public boolean spotIsFreeAround(MapLocation loc) throws GameActionException {
        for(Direction dir: directions) {
            MapLocation loc2 = loc.add(dir);
            if(rc.canSenseLocation(loc2) && !rc.isLocationOccupied(loc2)) {
                return true;
            }
        }
        return false;
    }

    public boolean updateSymmetryAndOpponentHQs() throws GameActionException {
        if(hqLoc != null && enemyHqLocPossibilities == null) {
            initializeEnemyHQLocs();
        }
        else if(hqLoc == null){
            Utils.log("rip");
            return false;
        }
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
        Utils.log("I see " + enemies.length + " enemies.");
        for(RobotInfo e: enemies) {
            if (e.type == RobotType.HQ) {
                Utils.log("found you!");
                enemyHQLoc = e.location;
                return false;
            }
        }
        boolean removed = false;
        MapLocation toRemove = null;
        Utils.log("looking for removal");
        for(MapLocation loc: enemyHqLocPossibilities) {
            if(rc.canSenseLocation(loc)) {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if(ri == null || ri.type != RobotType.HQ) {
                    toRemove = loc;
                    removed = true;
                    break;
                }
            }
        }
        if(removed) {
            enemyHqLocPossibilities = Utils.removeElement(enemyHqLocPossibilities, toRemove);
        }
        if(Clock.getBytecodesLeft() > 2500)
            removed |= doSymmetryDetection();
        if(enemyHqLocPossibilities.length == 1) {
            enemyHQLoc = enemyHqLocPossibilities[0];
            return false;
        }
        return removed;
    }

    private MapLocation reflectX(MapLocation loc) {
        return new MapLocation(mapWidth - 1 -loc.x, loc.y);
    }

    private MapLocation reflectY(MapLocation loc) {
        return new MapLocation(loc.x, mapHeight - 1 -loc.y);
    }

    private MapLocation reflectR(MapLocation loc) {
        return new MapLocation(mapWidth - 1 - loc.x, mapHeight - 1 - loc.y);
    }

    private MapLocation reflect(Symmetry s, MapLocation loc) {
        switch(s){
            case ROTATIONAL:return reflectR(loc);
            case VERTICAL:return reflectX(loc);
            case HORIZONTAL:return reflectY(loc);
        }
        return null;
    }

    private Symmetry getSymmetry(MapLocation locA, MapLocation locB) {
        if(reflectR(locA).equals(locB))
            return Symmetry.ROTATIONAL;
        if(reflectX(locA).equals(locB))
            return Symmetry.VERTICAL;
        return Symmetry.HORIZONTAL;
    }

    private boolean checkSym(MapLocation locA, MapLocation locB) throws GameActionException {
        Utils.log("checking sym of " + locA + " and " + locB);
        return rc.senseElevation(locA) == rc.senseElevation(locB) && rc.senseSoup(locA) == rc.senseSoup(locB);
    }

    private boolean doSymmetryDetection() throws GameActionException {
        //TODO improve
        MapLocation[] toCheck = new MapLocation[17];
        int idx = 1;
        toCheck[0] = here;
        for(Direction dir: directions) {
            MapLocation check = here;
            for(int i=0;i<2;i++) {
                check = check.add(dir);
                toCheck[idx++] = check;
            }
        }
        MapLocation toRemove = null;
        MapLocation toRemoveSecond = null;
        for(MapLocation loc: enemyHqLocPossibilities) {
            Symmetry s = getSymmetry(loc, hqLoc);
            for(MapLocation check: toCheck) {
                if(rc.canSenseLocation(check)) {
                    MapLocation reflected = reflect(s, check);
                    if(rc.canSenseLocation(reflected) && !checkSym(check, reflected)) {
                        if(toRemove != null){
                            toRemoveSecond = null;
                            break;
                        }
                        else {
                            Utils.log("Rip " + s + "sym!");
                            toRemove = loc;
                            break;
                        }
                    }
                }
            }
        }
        if(toRemove != null){
            enemyHqLocPossibilities = Utils.removeElement(enemyHqLocPossibilities, toRemove);
            if(toRemoveSecond != null)
                enemyHqLocPossibilities = Utils.removeElement(enemyHqLocPossibilities, toRemoveSecond);
            return true;
        }
        return false;
    }

    public MapLocation pickTargetFromEnemyHQs(boolean closest) throws GameActionException {
        if(hqLoc != null && enemyHqLocPossibilities == null) {
            initializeEnemyHQLocs();
        }
        else if(hqLoc == null){
            return center;
        }
        MapLocation ret = enemyHqLocPossibilities[0];
        if(!closest) {
            int maxDist = hqLoc.distanceSquaredTo(ret);
            for (int i = 1; i < enemyHqLocPossibilities.length; i++) {
                MapLocation loc = enemyHqLocPossibilities[i];
                if (here.distanceSquaredTo(loc) > maxDist) {
                    maxDist = hqLoc.distanceSquaredTo(loc);
                    ret = loc;
                }
            }
        }
        else {
            int minDist = here.distanceSquaredTo(ret);
            for (int i = 1; i < enemyHqLocPossibilities.length; i++) {
                MapLocation loc = enemyHqLocPossibilities[i];
                if (here.distanceSquaredTo(loc) < minDist) {
                    minDist = here.distanceSquaredTo(loc);
                    ret = loc;
                }
            }
        }
        return ret;
    }

    public void initializeEnemyHQLocs() throws GameActionException{
        int x = hqLoc.x;
        int y = hqLoc.y;
        MapLocation a = reflectR(hqLoc);
        MapLocation b = reflectX(hqLoc);
        MapLocation c = reflectY(hqLoc);
        Utils.log("poss: "+ a + b + c);
        enemyHqLocPossibilities = new MapLocation[]{a,b,c};
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

    static boolean tryBuild(RobotType type, Direction dir, boolean tryOthers) throws GameActionException {
        if(rc.getCooldownTurns() >= 1 || rc.getTeamSoup() - 1 < type.cost)
            return false;
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            comms.broadcastCreation(type, here.add(dir));
            return true;
        }
        if(tryOthers) {
            Direction dirL = dir.rotateLeft();
            Direction dirR = dir.rotateRight();
            while(dirL != dir) {
                if (rc.canBuildRobot(type,dirL)) {
                    rc.buildRobot(type,dirL);
                    comms.broadcastCreation(type, here.add(dir));
                    return true;
                }
                if (rc.canBuildRobot(type,dirR)) {
                    rc.buildRobot(type,dirR);
                    comms.broadcastCreation(type, here.add(dir));
                    return true;
                }
                dirL = dirL.rotateLeft();
                dirR = dirR.rotateRight();
            }
        }
        return false;
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
