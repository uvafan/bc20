package v11_tuned;

import battlecode.common.*;

public class Landscaper extends Unit {

    public static MapLocation ourDesignSchool = null;
    public static boolean shouldRemoveDirt;
    public static boolean turtling = false;
    public static boolean defending = false;
    public static boolean doneDefending = false;

    public static MapLocation target = null;

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
        RobotInfo buildingToBury = getBuildingToBury();
        if(!rushing && rc.getCooldownTurns() < 1 && !(round > MagicConstants.CRUNCH_ROUND && buildingToBury != null && here.distanceSquaredTo(buildingToBury.location) <= 2)) {
            dealWithEnemyDrones();
        }
        if(rushing) {
            doRush();
        }
        else if(rc.getCooldownTurns() < 1){
            if(defending && (rc.canSenseLocation(hqLoc) || buildingToBury == null))
                doDefense();
            else {
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
    	Utils.log("I'm a lattice landscaper!");
    	int digging = rc.getDirtCarrying();
    	if(digging == 0 || digging == RobotType.LANDSCAPER.dirtLimit) {
    		Utils.log("Case 1");
    		MapLocation bestDigLoc = null;
            int i = 0;
            while(i < nearbyXOffsets.length) {
            	MapLocation testTile = here.translate(nearbyXOffsets[i], nearbyYOffsets[i]);
            	int dist = here.distanceSquaredTo(testTile);
            	if(dist > rc.getCurrentSensorRadiusSquared()) {
            		break;
            	}
            	//Utils.log("Testing tile: " + testTile.x + ", " + testTile.y);
            	//Utils.log(""+dist);
            	if(shouldDig(testTile, digging==0)) {
            		bestDigLoc = testTile;
            		break;
            	}
            	i++;
            }
            if(bestDigLoc!=null) {
            	Utils.log("Give me a dead tile at: " + bestDigLoc.x + ", " + bestDigLoc.y);
            	if(here.distanceSquaredTo(bestDigLoc) <=2 ) {
            		target = null;
            		if(digging==0) {
            		tryDig(here.directionTo(bestDigLoc),false);
            		}else {
            			if(rc.canDepositDirt(here.directionTo(bestDigLoc))) {
                			rc.depositDirt(here.directionTo(bestDigLoc));
                		}
            		}
            	}
            	else {
            		if(target == null) {
            			target = bestDigLoc;
            		}
            	}
            }
    	}
    	else {
    		Utils.log("Case 2");
    		MapLocation bestDirtLoc = null;
            int minDist = Integer.MAX_VALUE;
            int i = 0;
            while(i < nearbyXOffsets.length) {
            	MapLocation testTile = here.translate(nearbyXOffsets[i], nearbyYOffsets[i]);
            	int dist = here.distanceSquaredTo(testTile);
            	if(dist > rc.getCurrentSensorRadiusSquared()) {
            		break;
            	}
            	int distToHQ = Math.min(hqLoc.distanceSquaredTo(testTile), MagicConstants.BUBBLE_AROUND_HQ)*10;
            	int distToEnemy = 0;
            	if(enemyHQLoc != null) {
            		distToEnemy = enemyHQLoc.distanceSquaredTo(testTile);
            	}
            	int dontNavMod = dist <= 2 ? 0 : 100000;
            	int mainWallMod = 1000000;
            	int maybeWall = testTile.distanceSquaredTo(hqLoc);
            	if(maybeWall <= 18) {
            		mainWallMod = 0;
            	}
            	switch(maybeWall) {
            	case 16:
            	case 17:
            		mainWallMod = 1000000;
            		break;
            	default:
            	}
            	//Utils.log(testTile.x + ", " + testTile.y + ": " + (dist + distToHQ + mainWallMod + dontNavMod) + ", " + shouldRenovate(testTile));
            	if(dist + distToHQ + mainWallMod + dontNavMod + distToEnemy < minDist  && shouldRenovate(testTile)) {
            		bestDirtLoc = testTile;
            		minDist = dist + distToHQ + mainWallMod + dontNavMod + distToEnemy;
            	}
            	i++;
            }
            if(bestDirtLoc!=null) {
            	Utils.log(bestDirtLoc.x + ", " + bestDirtLoc.y + ": " + minDist);
            	if(here.distanceSquaredTo(bestDirtLoc) <=2 ) {
            		target = null;
            		if(bestDirtLoc.distanceSquaredTo(hqLoc) <= 8) {
            			if(rc.senseElevation(bestDirtLoc)> hqElevation) {
            				tryDig(here.directionTo(bestDirtLoc),false);
            			}
            			else {
            				if(rc.canDepositDirt(here.directionTo(bestDirtLoc))) {
                				rc.depositDirt(here.directionTo(bestDirtLoc));
                			}
            			}
            		}
            		if(rc.senseElevation(bestDirtLoc) < MagicConstants.LATTICE_HEIGHT) {
            			if(rc.canDepositDirt(here.directionTo(bestDirtLoc))) {
            				rc.depositDirt(here.directionTo(bestDirtLoc));
            			}
            		}else {
            			tryDig(here.directionTo(bestDirtLoc),false);
            		}
            	}
            	else {
            		if(target == null) {
            			target = bestDirtLoc;
            		}
            	}
            }
    	}
    	if(rc.getCooldownTurns()<1) {
    		Utils.log("Case 3");
    		if(target != null) {
    			Utils.log("I'm REALLY WANT TO GO TO: " + target.x + ", " + target.y);
    			Utils.log("I'm currenty at: " + here.x + ", " + here.y);
    			if(here.distanceSquaredTo(target) <= 2) {
    				target = null;
    			}
    			else {
    				Utils.log("Well, hope I can get there");
    				goToOnLattice(target);
    			}
    		}
    		else if (enemyHQLoc != null) {
    			Utils.log("BRING DOWN THAT WALL");
    			goToOnLattice(enemyHQLoc);
    		}
    	}
    }
    private boolean shouldRenovate(MapLocation testTile) throws GameActionException {
    	//Utils.log("I'm trying to sense: " + testTile.x + ", " + testTile.y);
    	if(!badLatticeLoc(testTile,true, true)) {
    		int elev = rc.senseElevation(testTile);
    		if ((elev < MagicConstants.LATTICE_HEIGHT && (elev > MagicConstants.LATTICE_HEIGHT - MagicConstants.LATTICE_TOLERANCE || testTile.distanceSquaredTo(hqLoc) <= 8 && elev > 0- MagicConstants.WATER_TOLERANCE)|| (elev > MagicConstants.LATTICE_HEIGHT + 3 && elev < MagicConstants.LATTICE_HEIGHT + 3 + MagicConstants.LATTICE_TOLERANCE)) && (!(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2) || hqLoc.distanceSquaredTo(testTile) <= 8)) {
    			//Utils.log("I should be renovating: " + testTile.x + ", " + testTile.y);
    			return true;
    		}
    	}
    	return false;
	}

	static boolean badLatticeLoc(MapLocation loc, boolean amDigging, boolean renovating) throws GameActionException{
    	if(loc.x < 0 || loc.x >= mapWidth || loc.y < 0 || loc.y >= mapHeight)
    		return true;
    	RobotInfo possiblyUs = rc.senseRobotAtLocation(loc);
    	if(possiblyUs != null && possiblyUs.team == us && Utils.isBuilding(possiblyUs.type) && (!amDigging || possiblyUs.dirtCarrying == 0)) {
    		return true;
    	}
    	if(loc.distanceSquaredTo(hqLoc)<=8) {
    		if(sensedHQElevation) {
    			if(renovating) {
    				if(rc.senseElevation(loc)>hqElevation+3) {
    					return false;
    				}
    				if(rc.senseElevation(loc) < hqElevation || rc.senseFlooding(loc) && rc.senseElevation(loc) > 0- MagicConstants.WATER_TOLERANCE ) {
    					return false;
    				}
    			}
    			else {
    				if(amDigging) {
    					if(rc.senseElevation(loc)>hqElevation+3) {
    						return false;
    					}
    				}
    				else {
    					if(rc.senseElevation(loc) < hqElevation || rc.senseFlooding(loc) && rc.senseElevation(loc) > 0- MagicConstants.WATER_TOLERANCE ) {
    						return false;
    					}
    				}
    			}
        	}
    		return true;
    	}
    	return false;

	}
	public boolean shouldDig(MapLocation testTile, boolean actuallyDigging) throws GameActionException {
		if(actuallyDigging) {
			if(!badLatticeLoc(testTile,actuallyDigging, false)) {
				if(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2 || rc.senseElevation(testTile) > MagicConstants.LATTICE_HEIGHT) {
					return true;
				}
			}
		}
		else {
			if(!badLatticeLoc(testTile,actuallyDigging, false)) {
				if(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2 || rc.senseElevation(testTile) < MagicConstants.LATTICE_HEIGHT+3)
					return true;
			}
		}
		return false;
	}

	private int landscaperDiff(MapLocation loc) throws GameActionException{
        int diff = 0;
        for(Direction dir: directions) {
            MapLocation loc2 = loc.add(dir);
            if(rc.canSenseLocation(loc2)) {
                RobotInfo ri = rc.senseRobotAtLocation(loc2);
                if(ri != null && ri.type == RobotType.LANDSCAPER) {
                    if(ri.team == enemy)
                        diff--;
                    else
                        diff++;
                }
            }
        }
        return diff;
    }

	private boolean canLeaveHQ() throws GameActionException {
        return landscaperDiff(hqLoc) > 2;
    }

	private void doDefense() throws GameActionException {
        Utils.log("defending!");
        if(!rc.canSenseLocation(hqLoc)) {
            goTo(hqLoc);
            return;
        }
        RobotInfo ret = null;
        RobotInfo[] enemyBuildings = new RobotInfo[100];
        int numEnemyBuildings = 0;
        for(RobotInfo e: enemies) {
            if(Utils.isBuilding(e.type)) {
                enemyBuildings[numEnemyBuildings++] = e;
            }
        }
        int spotPriority = Integer.MIN_VALUE;
        MapLocation spot = null;
        int spotsFree = 0;
        for(Direction dir: directions) {
            MapLocation loc = hqLoc.add(dir);
            if(!rc.canSenseLocation(loc) || (!loc.equals(here) && !canReach(loc, here, true))) {
                continue;
            }
            spotsFree += loc.equals(here) ? 0 : 1;
            int priority = -here.distanceSquaredTo(loc) * MagicConstants.SPOT_DIST_MULTIPLIER;
            priority += (loc.equals(here) ? MagicConstants.STAY_HERE_BONUS: 0);
            int minDist = MagicConstants.MAX_BUILDING_DIST;
            for(int i=0; i<numEnemyBuildings; i++) {
                RobotInfo ri = enemyBuildings[i];
                minDist = Math.min(minDist, loc.distanceSquaredTo(ri.location));
            }
            priority += (minDist <= 2 ? MagicConstants.NEXT_TO_BUILDING_BONUS : (MagicConstants.MAX_BUILDING_DIST - minDist) / 2);
            if(priority > spotPriority) {
                spotPriority = priority;
                spot = loc;
            }
        }
        int myDirt = rc.getDirtCarrying();
        int dirtOnHQ = rc.senseRobotAtLocation(hqLoc).dirtCarrying;
        if(spot != null) {
            spotPriority += spotsFree * MagicConstants.SPOTS_FREE_MULTIPLIER;
            int diff = landscaperDiff(hqLoc);
            spotPriority -= diff * MagicConstants.LANDSCAPER_DIFF_MULTIPLIER;
            spotPriority += MagicConstants.HQ_DIRT_MULTIPLIER * 3;
            spotPriority -= myDirt * MagicConstants.MY_DIRT_MULTIPLIER;
        }
        int buildingPriority = Integer.MIN_VALUE;
        int adjBuildingPriority = Integer.MIN_VALUE;
        MapLocation building = null;
        MapLocation adjBuilding = null;
        for(int i=0; i<numEnemyBuildings; i++) {
            RobotInfo e = enemyBuildings[i];
            int dist = here.distanceSquaredTo(e.location);
            int priority = -dist * MagicConstants.BUILDING_DIST_MULTIPLIER;
            priority += e.dirtCarrying * MagicConstants.BUILDING_DIRT_MULTIPLIER;
            priority += e.type == RobotType.NET_GUN ? MagicConstants.NET_GUN_BONUS : 0;
            priority += dist <= 2 ? MagicConstants.BUILDING_ADJ_BONUS : 0;
            int bDiff = landscaperDiff(e.location);
            priority += bDiff * MagicConstants.BUILDING_DIFF_MULTIPLIER;
            if (priority > buildingPriority) {
                buildingPriority = priority;
                building = e.location;
            }
            if(dist <= 2 && priority > adjBuildingPriority) {
                adjBuildingPriority = priority;
                adjBuilding = e.location;
            }
        }
        if(spot != null)
            Utils.log("spot is " + spot + " priority: " + spotPriority);
        if(building != null)
            Utils.log("building at " + building + " priority: " + buildingPriority);
        if(spot == null && building == null) {
            doneDefending = true;
        }
        else if(spotPriority > buildingPriority) {
            rc.setIndicatorLine(here, spot, 255, 0, 0);
            if(!here.equals(spot)) {
                goTo(spot);
            }
            if(myDirt < 25 && dirtOnHQ > 0) {
                tryDig(here.directionTo(hqLoc), false);
            }
            else if(adjBuilding != null && myDirt > 0) {
                if(rc.canDepositDirt(here.directionTo(adjBuilding)))
                    rc.depositDirt(here.directionTo(adjBuilding));
            }
            else if (adjBuilding != null && myDirt < 25) {
                tryDig(adjBuilding.directionTo(here), true);
            }
            else if (dirtOnHQ > 0){
                Direction d = hqLoc.directionTo(here);
                Direction[] dirs = {d, d.rotateLeft(), d.rotateRight(), d.rotateLeft().rotateLeft(), d.rotateRight().rotateRight()};
                for(Direction dir: dirs) {
                    MapLocation loc = here.add(dir);
                    RobotInfo ri = rc.senseRobotAtLocation(loc);
                    if(ri != null && ri.team == us)
                        continue;
                    if(rc.canDepositDirt(dir)) {
                        rc.depositDirt(dir);
                        break;
                    }
                }
            }
        }
        else {
            rc.setIndicatorLine(here, building, 255, 0, 0);
            if(here.distanceSquaredTo(building) <= 2) {
                if(myDirt == 0) {
                    tryDig(building.directionTo(here), true);
                }
                else if(rc.canDepositDirt(here.directionTo(building))) {
                    rc.depositDirt(here.directionTo(building));
                }
            }
            else {
                goTo(building);
            }
        }
    }

    private RobotInfo getBuildingToBury() throws GameActionException {
        RobotInfo ret = null;
        int maxPriority = Integer.MIN_VALUE;
        for(RobotInfo e: enemies) {
            if(!Utils.isBuilding(e.type))
                continue;
            int dist = here.distanceSquaredTo(e.location);
            if(!hqAttacked && dist > 2 && !canReachAdj(e.location, true))
                continue;
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

/* old defense
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
        }*/
