package v8_eco_lattice;

import battlecode.common.*;

import java.util.Random;

public class Bot {
    public static RobotController rc;
    public static boolean hqAttacked = false;
    public static boolean rushing = false;
    public static RobotType type;
    public static Team enemy;
    public static Team us;
    public static MapLocation hqLoc;
    public static int hqElevation = 0;
    public static boolean sensedHQElevation = false;
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
    public static MapLocation[] waterLocs;
    public static int numWaterLocs;
    public static boolean[] invalidWater;
    public static MapLocation[] enemyNetGunLocs;
    public static int numEnemyNetGuns;
    public static boolean[] invalidNetGun;
    public static Direction lastExploreDir;
    public static Random rand;
    public static MapLocation targetLoc;
    public static int[] unitCounts = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
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

    public static Direction[] cardinalDirs = {
            Direction.NORTH,
            Direction.EAST,
            Direction.WEST,
            Direction.SOUTH,
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
    public static boolean isWallComplete = false;

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
        if (round > 1) {
            comms.readMessages(1);
        }
        refineries = new MapLocation[100];
        designSchools = new MapLocation[100];
        soupClusters = new MapLocation[100];
        invalidCluster = new boolean[100];
        numSoupClusters = 0;
        waterLocs = new MapLocation[100];
        invalidWater = new boolean[100];
        numWaterLocs = 0;
        enemyNetGunLocs = new MapLocation[100];
        invalidNetGun = new boolean[100];
        numEnemyNetGuns = 0;
        rand = new Random();
        strat = new EcoLattice(this);
        for (RobotInfo e : enemies) {
            if (e.type == RobotType.HQ) {
                enemyHQLoc = e.location;
                break;
            }
        }
        for (RobotInfo e : friends) {
            if (e.type == RobotType.HQ) {
                hqLoc = e.location;
                hqElevation = rc.senseElevation(e.location);
                sensedHQElevation = true;
                break;
            }
        }
        if (hqLoc != null) {
            initializeEnemyHQLocs();
        }
    }

    public boolean canReach(MapLocation locA, MapLocation locB, boolean checkOccupied) throws GameActionException {
        return !rc.senseFlooding(locA)
                && Math.abs(rc.senseElevation(locA) - rc.senseElevation(locB)) <= 3
                && (!checkOccupied || !rc.isLocationOccupied(locA));
    }

    public boolean canReachAdj(MapLocation loc, boolean checkOccupied) throws GameActionException {
        if (canReach(loc, here, checkOccupied))
            return true;
        for (Direction dir : directions) {
            MapLocation to = loc.add(dir);
            Utils.log("checking " + to);
            if (rc.canSenseLocation(to) && canReach(to, here, checkOccupied))
                return true;
        }
        return false;
    }

    public void takeTurn() throws GameActionException {
        turnCount++;
        round = rc.getRoundNum();
        enemies = rc.senseNearbyRobots(-1, enemy);
        friends = rc.senseNearbyRobots(-1, us);
        if (round - 1 == comms.readRound)
            comms.readMessages();
    }

    public boolean spotIsFreeAround(MapLocation loc) throws GameActionException {
        for (Direction dir : directions) {
            MapLocation loc2 = loc.add(dir);
            if (rc.canSenseLocation(loc2) && canReach(loc2, here, true)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastNetGuns() throws GameActionException {
        if(!comms.isCaughtUp() || round % 10 != rc.getID() % 10)
            return;
        for (RobotInfo e : enemies) {
            if (e.type == RobotType.NET_GUN) {
                boolean shouldAdd = true;
                for (int i = 0; i < numEnemyNetGuns; i++) {
                    if (!invalidNetGun[i] && enemyNetGunLocs[i].equals(e.location)) {
                        shouldAdd = false;
                        break;
                    }
                }
                if (shouldAdd) {
                    comms.broadcastLoc(Comms.MessageType.ENEMY_NET_GUN_LOC, e.location);
                }
            }
        }
        for (int i = 0; i < numEnemyNetGuns; i++) {
            // if(turnCount % 10 != i % 10)
            //    continue;
            if (invalidNetGun[i])
                continue;
            MapLocation ng = enemyNetGunLocs[i];
            Utils.log("checking net gun loc " + ng);
            if (rc.canSenseLocation(ng)) {
                if (!rc.isLocationOccupied(ng)) {
                    invalidNetGun[i] = true;
                    comms.broadcastLoc(Comms.MessageType.NET_GUN_REMOVED, ng);
                    continue;
                }
                RobotInfo ri = rc.senseRobotAtLocation(ng);
                if (ri.team != enemy || ri.type != RobotType.NET_GUN) {
                    invalidNetGun[i] = true;
                    comms.broadcastLoc(Comms.MessageType.NET_GUN_REMOVED, ng);
                }
            }
        }
    }


    public boolean updateOpponentHQs() throws GameActionException {
        if (hqLoc != null && enemyHqLocPossibilities == null) {
            initializeEnemyHQLocs();
        } else if (hqLoc == null) {
            Utils.log("rip");
            return false;
        }
        for (RobotInfo e : enemies) {
            if (e.type == RobotType.HQ) {
                Utils.log("found you!");
                enemyHQLoc = e.location;
                comms.broadcastLoc(Comms.MessageType.ENEMY_HQ_LOC, enemyHQLoc);
                return false;
            }
        }
        boolean removed = false;
        MapLocation toRemove = null;
        Utils.log("looking for removal");
        for (MapLocation loc : enemyHqLocPossibilities) {
            if (rc.canSenseLocation(loc)) {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if (ri == null || ri.type != RobotType.HQ) {
                    toRemove = loc;
                    removed = true;
                    break;
                }
            }
        }
        if (removed) {
            enemyHqLocPossibilities = Utils.removeElement(enemyHqLocPossibilities, toRemove);
        }
        if (enemyHqLocPossibilities.length == 1) {
            enemyHQLoc = enemyHqLocPossibilities[0];
            comms.broadcastLoc(Comms.MessageType.ENEMY_HQ_LOC, enemyHQLoc);
            return false;
        }
        return removed;
    }

    public boolean updateSymmetryAndOpponentHQs() throws GameActionException {
        boolean removed = updateOpponentHQs();
        if (enemyHqLocPossibilities.length > 1 && Clock.getBytecodesLeft() > 2500)
            removed |= doSymmetryDetection();
        return removed;
    }

    private MapLocation reflectX(MapLocation loc) {
        return new MapLocation(mapWidth - 1 - loc.x, loc.y);
    }

    private MapLocation reflectY(MapLocation loc) {
        return new MapLocation(loc.x, mapHeight - 1 - loc.y);
    }

    private MapLocation reflectR(MapLocation loc) {
        return new MapLocation(mapWidth - 1 - loc.x, mapHeight - 1 - loc.y);
    }

    private MapLocation reflect(Symmetry s, MapLocation loc) {
        switch (s) {
            case ROTATIONAL:
                return reflectR(loc);
            case VERTICAL:
                return reflectX(loc);
            case HORIZONTAL:
                return reflectY(loc);
        }
        return null;
    }

    private Symmetry getSymmetry(MapLocation locA, MapLocation locB) {
        if (reflectR(locA).equals(locB))
            return Symmetry.ROTATIONAL;
        if (reflectX(locA).equals(locB))
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
        for (Direction dir : directions) {
            MapLocation check = here;
            for (int i = 0; i < 2; i++) {
                check = check.add(dir);
                toCheck[idx++] = check;
            }
        }
        MapLocation toRemove = null;
        MapLocation toRemoveSecond = null;
        for (MapLocation loc : enemyHqLocPossibilities) {
            Symmetry s = getSymmetry(loc, hqLoc);
            for (MapLocation check : toCheck) {
                if (rc.canSenseLocation(check)) {
                    MapLocation reflected = reflect(s, check);
                    if (rc.canSenseLocation(reflected) && !checkSym(check, reflected)) {
                        if (toRemove != null) {
                            toRemoveSecond = null;
                            break;
                        } else {
                            Utils.log("Rip " + s + " sym!");
                            toRemove = loc;
                            break;
                        }
                    }
                }
            }
        }
        if (toRemove != null) {
            enemyHqLocPossibilities = Utils.removeElement(enemyHqLocPossibilities, toRemove);
            if (toRemoveSecond != null)
                enemyHqLocPossibilities = Utils.removeElement(enemyHqLocPossibilities, toRemoveSecond);
            return true;
        }
        return false;
    }

    public MapLocation pickTargetFromEnemyHQs(boolean closest) throws GameActionException {
        if (hqLoc != null && enemyHqLocPossibilities == null) {
            initializeEnemyHQLocs();
        } else if (hqLoc == null) {
            return center;
        }
        MapLocation ret = enemyHqLocPossibilities[0];
        if (!closest) {
            int maxDist = hqLoc.distanceSquaredTo(ret);
            for (int i = 1; i < enemyHqLocPossibilities.length; i++) {
                MapLocation loc = enemyHqLocPossibilities[i];
                if (here.distanceSquaredTo(loc) > maxDist) {
                    maxDist = hqLoc.distanceSquaredTo(loc);
                    ret = loc;
                }
            }
        } else {
            int minDist = here.distanceSquaredTo(ret);
            for (int i = 1; i < enemyHqLocPossibilities.length; i++) {
                MapLocation loc = enemyHqLocPossibilities[i];
                if (here.distanceSquaredTo(loc) < minDist) {
                    minDist = here.distanceSquaredTo(loc);
                    ret = loc;
                }
            }
        }
        Utils.log("picked " + ret);
        return ret;
    }

    public void initializeEnemyHQLocs() throws GameActionException {
        int x = hqLoc.x;
        int y = hqLoc.y;
        MapLocation a = reflectR(hqLoc);
        MapLocation b = reflectX(hqLoc);
        MapLocation c = reflectY(hqLoc);
        Utils.log("poss: " + a + b + c);
        enemyHqLocPossibilities = new MapLocation[]{a, b, c};
    }

    static boolean nearbyRobot(RobotType target) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots) {
            if (r.getType() == target) {
                return true;
            }
        }
        return false;
    }

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static boolean tryBuild(RobotType type, Direction dir, boolean tryOthers) throws GameActionException {
        if (rc.getCooldownTurns() >= 1 || rc.getTeamSoup() - 1 < type.cost)
            return false;
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            comms.broadcastCreation(type, here.add(dir));
            return true;
        }
        if (tryOthers) {
            Direction dirL = dir.rotateLeft();
            Direction dirR = dir.rotateRight();
            while (dirL != dir) {
                if (rc.canBuildRobot(type, dirL)) {
                    rc.buildRobot(type, dirL);
                    comms.broadcastCreation(type, here.add(dir));
                    return true;
                }
                if (rc.canBuildRobot(type, dirR)) {
                    rc.buildRobot(type, dirR);
                    comms.broadcastCreation(type, here.add(dir));
                    return true;
                }
                dirL = dirL.rotateLeft();
                dirR = dirR.rotateRight();
            }
        }
        return false;
    }


    static MapLocation[] getLocationsWithinSensorRad() {
        int sensorRad = rc.getCurrentSensorRadiusSquared();
        Utils.log("sensorRad: " + sensorRad);
        MapLocation[] ret = new MapLocation[1000];
        int idx = 0;
        for (int i = 0; i * i + i * i <= sensorRad; i++)
            for (int j = i; i * i + j * j <= sensorRad; j++) {
                ret[idx] = here.translate(i, j);
                idx++;
                if (i > 0 && j > 0) {
                    ret[idx] = here.translate(-i, -j);
                    idx++;
                }
                if (i > 0) {
                    ret[idx] = here.translate(-i, j);
                    idx++;
                }
                if (j > 0) {
                    ret[idx] = here.translate(i, -j);
                    idx++;
                }
                if (i == j)
                    continue;
                int temp = i;
                i = j;
                j = temp;
                ret[idx] = here.translate(i, j);
                idx++;
                if (i > 0 && j > 0) {
                    ret[idx] = here.translate(-i, -j);
                    idx++;
                }
                if (i > 0) {
                    ret[idx] = here.translate(-i, j);
                    idx++;
                }
                if (j > 0) {
                    ret[idx] = here.translate(i, -j);
                    idx++;
                }
            }
        ret[idx] = null;
        return ret;
    }

    public boolean isOnLattice(MapLocation loc) {
        return !(hqLoc.x % 2 == loc.x % 2 && hqLoc.y % 2 == loc.y % 2);
    }

    public boolean isOnLatticeIntersection(MapLocation loc) {
        return hqLoc.x % 2 != loc.x % 2 && hqLoc.y % 2 != loc.y % 2;
    }

}
