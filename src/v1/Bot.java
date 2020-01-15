package v1;

import battlecode.common.*;

public class Bot {
    public static RobotController rc;
    public static RobotType type;
    public static Team enemy;
    public static Team us;
    public static MapLocation hqLoc;
    public static MapLocation here;

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

    public static int turnCount = 0;
    public static int numMiners = 0;

    public Bot() {
        return;
    }

    public Bot(RobotController r) throws GameActionException {
        rc = r;
        type = rc.getType();
        us = rc.getTeam();
        enemy = rc.getTeam().opponent();
        findHQ();
    }

    public void takeTurn() throws GameActionException {
        turnCount++;
        here = rc.getLocation();
        return;
    }

    static void findHQ() throws GameActionException {
        if (hqLoc == null) {
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
        }
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
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
    // tries to move in the general direction of dir
    static boolean goTo(Direction dir) throws GameActionException {
        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
        for (Direction d : toTry){
            if(tryMove(d))
                return true;
        }
        return false;
    }

    // navigate towards a particular location
    static boolean goTo(MapLocation destination) throws GameActionException {
        return goTo(rc.getLocation().directionTo(destination));
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean explore() throws GameActionException {
        return goTo(randomDirection());
    }

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
    }

}
