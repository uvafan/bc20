package v14_final_bot;

import battlecode.common.*;

public class Unit extends Bot {
    public static boolean crunching = false;
    public static NavSafetyPolicy safe;
    public static NavSafetyPolicy crunch;

    public Unit(RobotController r) throws GameActionException {
        super(r);
        safe = new SafetyPolicyAvoidAllUnits();
        crunch = new SafetyPolicyCrunch();
    }

    @Override
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        here = rc.getLocation();
    }

    public void dealWithEnemyDrones() throws GameActionException {
        RobotInfo[] enemyDrones = new RobotInfo[100];
        int numEnemyDrones = 0;
        MapLocation closestDrone = null;
        int minDist = Integer.MAX_VALUE;
        for(RobotInfo e: enemies) {
            if(e.type == RobotType.DELIVERY_DRONE && !e.isCurrentlyHoldingUnit()) {
                int dist = here.distanceSquaredTo(e.location);
                enemyDrones[numEnemyDrones++] = e;
                if(dist < minDist) {
                    minDist = dist;
                    closestDrone = e.location;
                }
            }
        }
        Utils.log("I see " + numEnemyDrones + " drones.");
        Utils.log("My sight radius is " + rc.getCurrentSensorRadiusSquared());
        numNearbyEnemyDrones = numEnemyDrones;
        if(numEnemyDrones == 0)
            return;
        rc.setIndicatorLine(here, closestDrone, 0, 0, 255);
        if(MagicConstants.FLEE_BEFORE_BUILD) {
            if (fleeIfShould(enemyDrones, numEnemyDrones, closestDrone))
                buildNetGunIfShould(enemyDrones, numEnemyDrones, closestDrone);
        }
        else {
            if(!buildNetGunIfShould(enemyDrones, numEnemyDrones, closestDrone))
                fleeIfShould(enemyDrones, numEnemyDrones, closestDrone);
        }
    }

    public Direction[] getBuildDirections() throws GameActionException {
        if(strat instanceof Turtle) {
            return new Direction[]{hqLoc.directionTo(here), hqLoc.directionTo(here)};
        }
        else if(strat instanceof Rush) {
            return new Direction[]{here.directionTo(hqLoc), here.directionTo(hqLoc)};
        }
        else if(strat instanceof EcoLattice) {
            Direction[] buildDirs = new Direction[]{null, null};
            int minDist0 = Integer.MAX_VALUE;
            int minDist1 = Integer.MAX_VALUE;
            for(Direction dir: directions) {
                MapLocation loc = here.add(dir);
                if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
                    if(shouldBuildInLoc(loc, RobotType.DESIGN_SCHOOL)) {
                        int dist = -hqLoc.distanceSquaredTo(loc);
                        if (dist < minDist0) {
                            minDist0 = dist;
                            buildDirs[0] = dir;
                        }
                    }
                    if(shouldBuildInLoc(loc, RobotType.REFINERY)) {
                        int dist = -hqLoc.distanceSquaredTo(loc);
                        if (dist < minDist1) {
                            minDist1 = dist;
                            buildDirs[1] = dir;
                        }
                    }
                }
            }
            return buildDirs;
        }
        return new Direction[]{here.directionTo(hqLoc), here.directionTo(hqLoc)};
    }

    public boolean shouldBuildInLoc(MapLocation loc, RobotType type) throws GameActionException {
        if(loc.distanceSquaredTo(hqLoc) == 8 || (enemyHQLoc != null && loc.distanceSquaredTo(enemyHQLoc) <= 50 && loc.distanceSquaredTo(enemyHQLoc) < loc.distanceSquaredTo(hqLoc))) {
            return type == RobotType.NET_GUN;
        }
        if(loc.distanceSquaredTo(hqLoc) < 8) {
            return (loc.x + loc.y) % 2 == (hqLoc.x + hqLoc.y) % 2;
        }
        else if(type == RobotType.REFINERY) {
            return isOnLatticeIntersection(loc) && (!isOnWall(loc) || rc.senseElevation(loc) >= MagicConstants.LATTICE_HEIGHT);
        }
        else {
            return isOnLatticeIntersection(loc) && rc.senseElevation(loc) >= MagicConstants.LATTICE_HEIGHT;
        }
    }

    private boolean isOnWall(MapLocation loc) {
        int dist = hqLoc.distanceSquaredTo(loc);
        return dist <= 18 && dist != 16 && dist != 17;
    }

    private boolean buildNetGunIfShould(RobotInfo[] drones, int numDrones, MapLocation closestEnemyDrone) throws GameActionException {
        if(type != RobotType.MINER)
            return false;
        int soupNeeded = RobotType.NET_GUN.cost + 1;
        if(!isWallComplete) {
            int dist = here.distanceSquaredTo(hqLoc);
            if(dist > 18) {
                soupNeeded += MagicConstants.DIST_SOUP_MULTIPLIER * (dist-18);
            }
        }
        int numBuildings = 0;
        int distToEnemy = enemyHQLoc == null ? Integer.MAX_VALUE : here.distanceSquaredTo(enemyHQLoc);
        for(RobotInfo e: enemies) {
            if(Utils.isBuilding(e.type))
                numBuildings++;
        }
        boolean reallyWantToBuildNetGun = false;
        if(numBuildings > 0 && numBuildings + numDrones >= MagicConstants.MIN_NEARBY_DRONES_AND_BUILDINGS
                || (numDrones > 0 && distToEnemy <= MagicConstants.EXCITED_HQ_CLOSENESS))
            reallyWantToBuildNetGun = true;
        if( rc.getTeamSoup() < soupNeeded ||
            (!reallyWantToBuildNetGun && here.distanceSquaredTo(closestEnemyDrone) > MagicConstants.MAX_DIST_TO_BUILD_NET_GUN)
            || (numDrones == 1 && round < MagicConstants.BUILD_NET_GUN_ROUND))
            return false;
        if(!reallyWantToBuildNetGun) {
            int numNGs = unitCounts[RobotType.NET_GUN.ordinal()];
            int numVaps = unitCounts[RobotType.VAPORATOR.ordinal()];
            if (round < MagicConstants.CRUNCH_ROUND - 300 && (numNGs > 3 && numVaps < numNGs || numNGs > 1 && numVaps == 0))
                return false;
        }
        int numFriendlyNGs = 0;
        MapLocation[] friendlyNGs = new MapLocation[100];
        for(RobotInfo f: friends) {
            if(f.type == RobotType.NET_GUN)
                friendlyNGs[numFriendlyNGs++] = f.location;
        }
        if(closestEnemyDrone != null) {
            Direction bestDir = null;
            int maxScore = Integer.MIN_VALUE;
            for(Direction dir: directions) {
                MapLocation loc = here.add(dir);
                if(rc.canBuildRobot(RobotType.NET_GUN, dir)) {
                    int dist = hqLoc.distanceSquaredTo(loc);
                    if(dist < MagicConstants.MIN_NET_GUN_DIST_FROM_HQ)
                        continue;
                    int score = -Utils.manhattan(loc, closestEnemyDrone) + Utils.manhattan(loc, hqLoc);
                    if(!shouldBuildInLoc(loc, RobotType.NET_GUN)) {
                        continue;
                    }
                    boolean cont = false;
                    for(int i=0;i<numFriendlyNGs;i++) {
                        int min_closeness = reallyWantToBuildNetGun ? MagicConstants.MIN_EXCITED_NET_GUN_CLOSENESS : MagicConstants.MIN_NET_GUN_CLOSENESS;
                        if(friendlyNGs[i].distanceSquaredTo(loc) < min_closeness) {
                            cont = true;
                            break;
                        }
                    }
                    if(cont)
                        continue;
                    Utils.log("ng score for " + loc + " is " + score);
                    if (score > maxScore) {
                        maxScore = score;
                        bestDir = dir;
                    }
                }
            }
            if(bestDir != null && tryBuild(RobotType.NET_GUN, bestDir, false)) {
                return true;
            }
        }
        return false;
    }

    public boolean fleeIfShould(RobotInfo[] drones, int numDrones, MapLocation closestDrone) throws GameActionException {
        if(numDrones == 1 && here.distanceSquaredTo(closestDrone) > MagicConstants.MAX_DIST_TO_FLEE)
            return false;
        for(RobotInfo f: friends) {
            if((f.type == RobotType.NET_GUN || f.type == RobotType.HQ) && here.distanceSquaredTo(f.location) < 8) {
                return false;
            }
        }
        Direction bestDir = null;
        int maxScore = Integer.MIN_VALUE;
        for(Direction dir: directionsPlusCenter) {
            MapLocation loc = here.add(dir);
            if(rc.canMove(dir) && !rc.senseFlooding(loc)) {
                int minDist = Integer.MAX_VALUE;
                for(int i=0; i < numDrones; i++) {
                    RobotInfo d = drones[i];
                    minDist = Math.min(minDist, Utils.manhattan(d.location, loc));
                }
                // TODO: incorporate net guns into scoring
                int score = minDist * 2 - Utils.manhattan(loc, hqLoc);
                Utils.log("flee score for " + loc + " is " + score);
                if(score > maxScore) {
                   maxScore = score;
                   bestDir = dir;
                }
            }
        }
        if(bestDir != null && bestDir != Direction.CENTER && rc.canMove(bestDir)) {
            rc.move(bestDir);
            return true;
        }
        return false;
    }

    // tries to move in the general direction of dir
    static boolean moveInDir(Direction dir) throws GameActionException {
        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
        for (Direction d : toTry){
            if(tryMove(d))
                return true;
        }
        return false;
    }

    // navigate towards a particular location
    public boolean goTo(MapLocation destination) throws GameActionException {
        if(crunching)
            return Nav.goTo(destination, crunch);
        return Nav.goTo(destination, safe);
    }

    public static boolean goToOnLattice(MapLocation destination) throws GameActionException {
        return Nav.goTo(destination, new SafetyPolicyAvoidAllUnitsAndLattice());
    }

    public static boolean goToOnLattice(MapLocation destination, boolean inside, boolean checkElev) throws GameActionException {
        return Nav.goTo(destination, new SafetyPolicyAvoidAllUnitsAndLattice(inside, checkElev));
    }

    public boolean standingOnLattice() throws GameActionException {
        return isOnLattice(here) && rc.senseElevation(here) >= MagicConstants.LATTICE_HEIGHT;
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    public void explore() throws GameActionException {
        Nav.explore(safe);
    }

    public static void exploreOnLattice(boolean inside, boolean checkElev) throws GameActionException {
        Nav.explore(new SafetyPolicyAvoidAllUnitsAndLattice(inside, checkElev));
    }

}
