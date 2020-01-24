package v9_lattice_improved;

import battlecode.common.*;

public class Landscaper extends Unit {

    public static MapLocation ourDesignSchool = null;
    public static boolean shouldRemoveDirt;
    public static boolean turtling = false;
    public static boolean defending = false;
    public static boolean doneDefending = false;
    //public static int[] xDifferentials = {-1,0,0,1,-1,-1,1,1,-2,0,0}
    //public static int[] yDifferentials = {0,1,-1,0,1,-1,1,-1}

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
        if((strat instanceof Rush) && enemyHQLoc != null) {
            rushing = true;
            RobotInfo[] friends = rc.senseNearbyRobots(2, us);
            for(RobotInfo f: friends) {
                if(f.type == RobotType.DESIGN_SCHOOL) {
                    ourDesignSchool = f.location;
                    break;
                }
            }
        }
        else if (strat instanceof Turtle) {
            turtling = true;
        }
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        defending = hqAttacked && !doneDefending;
        if(rushing) {
            doRush();
        }
        else if(rc.getCooldownTurns() < 1){
            if(defending)
                doDefense();
            else {
                RobotInfo buildingToBury = getBuildingToBury();
                if (buildingToBury != null) {
                    buryEnemy(buildingToBury);
                }
                else if (turtling)
                    doTurtle();
                else
                    doLattice();
            }
        }
        comms.readMessages();
    }

    private void doLattice() throws GameActionException {
        Utils.log("latticing!");
    	int digging = rc.getDirtCarrying();
    	if(digging == 0 || digging == RobotType.LANDSCAPER.dirtLimit) {
    		MapLocation bestDigLoc = null;
            int minDist = Integer.MAX_VALUE;
            int i = 0;
            MapLocation[] nearbyTiles = getLocationsWithinSensorRad();
            while(true) {
            	MapLocation testTile = nearbyTiles[i++];
            	if(testTile == null)
            		break;
            	int dist = here.distanceSquaredTo(testTile);
            	if(dist < minDist && shouldDig(testTile, digging==0)) {
            		bestDigLoc = testTile;
            		minDist = dist;
            	}
            }
            if(bestDigLoc!=null) {
            	if(here.distanceSquaredTo(bestDigLoc) <=2 ) {
            		if(digging==0) {
            		tryDig(here.directionTo(bestDigLoc),false);
            		}else {
            			if(rc.canDepositDirt(here.directionTo(bestDigLoc))) {
                			rc.depositDirt(here.directionTo(bestDigLoc));
                		}
            		}
            	}
            	else {
            		goToOnLattice(bestDigLoc);
            	}
            }
            //else do something with comms
    	}
    	else {
    		MapLocation bestDirtLoc = null;
            int minDist = Integer.MAX_VALUE;
            int i = 0;
            MapLocation[] nearbyTiles = getLocationsWithinSensorRad();
            while(true) {
            	MapLocation testTile = nearbyTiles[i++];
            	if(testTile == null)
            		break;
            	int dist = here.distanceSquaredTo(testTile);
            	int distToHQ = hqLoc.distanceSquaredTo(testTile);
            	if(dist <= 2) {
            		distToHQ = 0;
            	}
            	if(dist + distToHQ < minDist && shouldRenovate(testTile)) {
            		bestDirtLoc = testTile;
            		minDist = dist + distToHQ;
            	}
            }
            if(bestDirtLoc!=null) {
            	if(here.distanceSquaredTo(bestDirtLoc) <=2 ) {
            		if(rc.senseElevation(bestDirtLoc) < MagicConstants.LATTICE_HEIGHT)
            		if(rc.canDepositDirt(here.directionTo(bestDirtLoc))) {
            			rc.depositDirt(here.directionTo(bestDirtLoc));
            		}else {
            			tryDig(here.directionTo(bestDirtLoc),false);
            		}
            	}
            	else {
            		goToOnLattice(bestDirtLoc);
            	}
            }
            //else something with comms
    	}
    }
    private boolean shouldRenovate(MapLocation testTile) throws GameActionException {
		if(!badLatticeLoc(testTile,true) && (rc.senseElevation(testTile) < MagicConstants.LATTICE_HEIGHT || rc.senseElevation(testTile) > MagicConstants.LATTICE_HEIGHT + 3) && !(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2)) {
			return true;
		}
		return false;
	}

	static boolean badLatticeLoc(MapLocation loc, boolean amDigging) throws GameActionException{
    	if(loc.x < 0 || loc.x >= mapWidth || loc.y < 0 || loc.y >= mapHeight)
    		return true;
    	RobotInfo possiblyUs = rc.senseRobotAtLocation(loc);
    	if(possiblyUs != null && possiblyUs.team == us && Utils.isBuilding(possiblyUs.type)) {
    		return true;
    	}
    	if(loc.distanceSquaredTo(hqLoc)<=8) {
    		if(sensedHQElevation) {
        		if(rc.senseElevation(loc) > hqElevation && amDigging || rc.senseElevation(loc) < hqElevation && !amDigging) {
        			return false;
        		}
        	}
    		return true;
    	}
    	return false;

	}
	public boolean shouldDig(MapLocation testTile, boolean actuallyDigging) throws GameActionException {
		if(actuallyDigging) {
			if(!badLatticeLoc(testTile,actuallyDigging)) {
				if(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2 || rc.senseElevation(testTile) > MagicConstants.LATTICE_HEIGHT) {
					return true;
				}
			}
		}
		else {
			if(!badLatticeLoc(testTile,actuallyDigging)) {
				if(rc.senseElevation(testTile) < MagicConstants.LATTICE_HEIGHT+3)
					return true;
			}
		}
		return false;
	}

	private boolean canLeaveHQ() throws GameActionException {
        int enemyDiff = 0;
        for(Direction dir: directions) {
            MapLocation loc = hqLoc.add(dir);
            if(rc.canSenseLocation(loc)) {
                RobotInfo ri = rc.senseRobotAtLocation(loc);
                if(ri != null && ri.type == RobotType.LANDSCAPER) {
                    if(ri.team == enemy)
                        enemyDiff++;
                    else
                        enemyDiff--;
                }
            }
        }
        return enemyDiff < -2;
    }

	private void doDefense() throws GameActionException {
        Utils.log("defending!");
        RobotInfo buildingToBury = getBuildingToBury();
        int minToBury = Integer.MAX_VALUE;
        if(buildingToBury != null)
            minToBury = Math.min(type.dirtLimit, buildingToBury.type.dirtLimit);
        boolean hqHasDirt = rc.canSenseLocation(hqLoc) && rc.senseRobotAtLocation(hqLoc).dirtCarrying > 0;
        if(hqHasDirt && here.distanceSquaredTo(hqLoc) <= 2)
            minToBury = type.dirtLimit;
        if(hqHasDirt && spotIsFreeAround(hqLoc) && here.distanceSquaredTo(hqLoc) > 2) {
            goTo(hqLoc);
        }
        else if(buildingToBury != null && here.distanceSquaredTo(buildingToBury.location) <= 2) {
            if(rc.getDirtCarrying() < minToBury && (buildingToBury.dirtCarrying == 0 || rc.getDirtCarrying() == 0)) {
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, buildingToBury.location, 0, 0, 255);
                if(hqLoc.distanceSquaredTo(here) <= 2)
                    tryDig(here.directionTo(hqLoc), false);
                if(rc.getCooldownTurns() < 1)
                    tryDig(hqLoc.directionTo(here), true);
            }
            else if(rc.canDepositDirt(here.directionTo(buildingToBury.location))) {
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, buildingToBury.location, 0, 255, 0);
                rc.depositDirt(here.directionTo(buildingToBury.location));
            }
        }
        else {
            if(buildingToBury != null && rc.getDirtCarrying() >= minToBury) {
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, buildingToBury.location, 255, 0, 0);
                goTo(buildingToBury.location);
            }
            else if (hqLoc != null) {
                if(!rc.canSenseLocation(hqLoc))
                    goTo(hqLoc);
                else if(here.distanceSquaredTo(hqLoc) > 2 && spotIsFreeAround(hqLoc) && hqHasDirt) {
                    goTo(hqLoc);
                }
                else if(here.distanceSquaredTo(hqLoc) <= 2 && rc.getDirtCarrying() < minToBury && (hqHasDirt || !canLeaveHQ())) {
                    if(rc.senseRobotAtLocation(hqLoc).dirtCarrying > 0) {
                        if(rc.canDigDirt(here.directionTo(hqLoc)))
                            rc.digDirt(here.directionTo(hqLoc));
                    }
                }
                else if(buildingToBury != null) {
                    if(Utils.DEBUG)
                        rc.setIndicatorLine(here, buildingToBury.location, 255, 0, 0);
                    goTo(buildingToBury.location);
                }
                else if (rc.canSenseLocation(hqLoc) && rc.senseRobotAtLocation(hqLoc).dirtCarrying > 0) {
                    goTo(hqLoc);
                }
                else {
                    doneDefending = true;
                }
            }
            else {
                doneDefending = true;
            }
        }
    }

    private RobotInfo getBuildingToBury() {
        RobotInfo ret = null;
        int maxPriority = Integer.MIN_VALUE;
        for(RobotInfo e: enemies) {
            if(!Utils.isBuilding(e.type))
                continue;
            int dist = here.distanceSquaredTo(e.location);
            int priority = (e.type == RobotType.NET_GUN ? 25 : 0) - dist;
            priority += ((dist <= 2 && here.distanceSquaredTo(hqLoc) <= 2) ? 100: 0);
            if(priority > maxPriority) {
                maxPriority = priority;
                ret = e;
            }
        }
        return ret;
    }

    private void doTurtle() throws GameActionException {
        if (hqLoc != null && hqLoc.distanceSquaredTo(here) < 4) {
            if (rc.senseRobotAtLocation(hqLoc).dirtCarrying > 0 && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
                tryDig(here.directionTo(hqLoc), false);
            }
            if (rc.getCooldownTurns() < 1 && (rc.getDirtCarrying() == 0 || rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && round < 250)) {
                MapLocation maybe = digLoc();
                if (maybe != null) {
                    Utils.log("I'm trying to dig in a smart place");
                    if (!tryDig(here.directionTo(maybe), false)) {
                        Utils.log("JK it didn't work");
                        tryDig(hqLoc.directionTo(here), true);
                    }
                }
            }
            if (rc.getCooldownTurns() < 1 && hqLoc != null) {
                // find best place to build
                MapLocation bestPlaceToBuildWall = here;
                int lowestElevation = 9999999;
                for (Direction dir : directions) {
                    MapLocation tileToCheck = hqLoc.add(dir);
                    if (here.distanceSquaredTo(tileToCheck) < 4
                            && rc.canDepositDirt(here.directionTo(tileToCheck))) {
                        int elevation = rc.senseElevation(tileToCheck);
                        if (rc.senseElevation(tileToCheck) < lowestElevation && (round > 1000 ||
                                Utils.getRoundFlooded(elevation - 1) < round)) {
                            lowestElevation = rc.senseElevation(tileToCheck);
                            bestPlaceToBuildWall = tileToCheck;
                        }
                    }
                }
                // build the wall
                Direction dir = here.directionTo(bestPlaceToBuildWall);
                if (rc.canDepositDirt(dir))
                    rc.depositDirt(dir);
                Utils.log("building a wall at location " + bestPlaceToBuildWall);
            }
        }
        // otherwise try to get to the hq
        else if (hqLoc != null) {
            if ((!spotIsFreeAround(hqLoc) || round > 1000) && inSecondLayer()) {
                if (rc.getCooldownTurns() < 1 && (rc.getDirtCarrying() == 0)) {
                    tryDig(hqLoc.directionTo(here), true);
                }

                if (rc.getCooldownTurns() < 1 && hqLoc != null) {
                    // find best place to build
                    MapLocation bestPlaceToBuildWall = here;
                    if (!(Utils.getRoundFlooded(rc.senseElevation(here) - 1) < round)) {
                        Utils.log("I'm an altruistic dirt placer");
                        int lowestElevation = 9999999;
                        for (Direction dir : directions) {
                            MapLocation tileToCheck = hqLoc.add(dir);
                            if (here.distanceSquaredTo(tileToCheck) < 4
                                    && rc.canDepositDirt(here.directionTo(tileToCheck))) {
                                int elevation = rc.senseElevation(tileToCheck);
                                if (rc.senseElevation(tileToCheck) < lowestElevation) {
                                    lowestElevation = rc.senseElevation(tileToCheck);
                                    bestPlaceToBuildWall = tileToCheck;
                                }
                            }
                        }
                    }
                    Direction dir = here.directionTo(bestPlaceToBuildWall);
                    if (rc.canDepositDirt(dir))
                        rc.depositDirt(dir);
                    Utils.log("building a wall at location " + bestPlaceToBuildWall);
                }
            } else {
                goTo(hqLoc);
            }
        }
    }

    private void buryEnemy(RobotInfo closestEnemyBuilding) throws GameActionException {
        MapLocation targetLoc = closestEnemyBuilding.location;
        if(here.distanceSquaredTo(closestEnemyBuilding.location) <= 2) {
            if(rc.getDirtCarrying() == 0) {
                if(hqLoc != null && rc.canSenseLocation(hqLoc)) {
                    if (rc.senseRobotAtLocation(hqLoc).dirtCarrying > 0 && here.distanceSquaredTo(hqLoc) == 2) {
                        tryDig(here.directionTo(hqLoc), false);
                    }
                    if (rc.getCooldownTurns() < 1) {//< RobotType.LANDSCAPER.dirtLimit) {
                        tryDig(hqLoc.directionTo(here), true);
                    }
                }
                else {
                    tryDig(randomDirection(), true);
                }
            }
            else if(rc.canDepositDirt(here.directionTo(targetLoc))) {
                if(closestEnemyBuilding.dirtCarrying + 1 == closestEnemyBuilding.type.dirtLimit) {
                    // rip them
                    if(closestEnemyBuilding.type == RobotType.NET_GUN) {
                        comms.broadcastLoc(Comms.MessageType.NET_GUN_REMOVED, targetLoc);
                    }
                }
                rc.depositDirt(here.directionTo(targetLoc));
            }
        }
        else if (hqLoc == null || here.distanceSquaredTo(hqLoc) > 2 || rc.senseRobotAtLocation(hqLoc).dirtCarrying == 0) {
            if(spotIsFreeAround(targetLoc))
                goTo(targetLoc);
        }
    }

    static void doRush() throws GameActionException {
        if(here.distanceSquaredTo(enemyHQLoc) > 2) {
            if(rc.getCooldownTurns() < 1)
                goTo(enemyHQLoc);
        }
        else {
            if (rc.getDirtCarrying() == 0) {
                if(here.distanceSquaredTo(ourDesignSchool) <= 2)
                    tryDig(here.directionTo(ourDesignSchool), false);
                tryDig(enemyHQLoc.directionTo(here), true);
            } else if (rc.getCooldownTurns() < 1) {
                if (rc.canDepositDirt(here.directionTo(enemyHQLoc))) {
                    rc.depositDirt(here.directionTo(enemyHQLoc));
                }
            }
        }
    }

    //assumes hqloc is not null
    static boolean inSecondLayer() {
        return here.distanceSquaredTo(hqLoc)<=8 && here.distanceSquaredTo(hqLoc)>=5;
    }
    static MapLocation digLoc() throws GameActionException {
    	MapLocation possibleLoc1 = new MapLocation(hqLoc.x-2, hqLoc.y);
    	if(rc.onTheMap(possibleLoc1) && here.distanceSquaredTo(possibleLoc1) <=2 && rc.canDigDirt(here.directionTo(possibleLoc1))){
    		return possibleLoc1;
    	}
    	possibleLoc1 = new MapLocation(hqLoc.x, hqLoc.y-2);
    	if(rc.onTheMap(possibleLoc1) && here.distanceSquaredTo(possibleLoc1) <=2&& rc.canDigDirt(here.directionTo(possibleLoc1))){
    		return possibleLoc1;
    	}
    	possibleLoc1 = new MapLocation(hqLoc.x+2, hqLoc.y);
    	if(rc.onTheMap(possibleLoc1) && here.distanceSquaredTo(possibleLoc1) <=2&& rc.canDigDirt(here.directionTo(possibleLoc1))){
    		return possibleLoc1;
    	}
    	possibleLoc1 = new MapLocation(hqLoc.x, hqLoc.y+2);
    	if(rc.onTheMap(possibleLoc1) && here.distanceSquaredTo(possibleLoc1) <=2&& rc.canDigDirt(here.directionTo(possibleLoc1))){
    		return possibleLoc1;
    	}
    			return null;
    }

    static boolean tryDig(Direction dir, boolean tryOthers) throws GameActionException {
        if(rc.canDigDirt(dir) && shouldDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        if(tryOthers) {
            Direction dirL = dir.rotateLeft();
            Direction dirR = dir.rotateRight();
            while(dirL != dir) {
                if (rc.canDigDirt(dirL) && shouldDigDirt(dirL)) {
                    rc.digDirt(dirL);
                    return true;
                }
                if (rc.canDigDirt(dirR) && shouldDigDirt(dirR)) {
                    rc.digDirt(dirR);
                    return true;
                }
                dirL = dirL.rotateLeft();
                dirR = dirR.rotateRight();
            }
        }
        return false;
    }

    private static boolean shouldDigDirt(Direction dir) throws GameActionException {
        MapLocation loc = here.add(dir);
        if(!rc.isLocationOccupied(loc))
            return true;
        RobotInfo ri = rc.senseRobotAtLocation(loc);
        return ri.team == us || !Utils.isBuilding(ri.type);
    }

}
