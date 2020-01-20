package v3_lattice;

import battlecode.common.*;

public class Landscaper extends Unit {

    public Landscaper(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        nearbyRobots = rc.senseNearbyRobots();
        separateRobots();
        /*if(hqLoc != null && hqLoc.distanceSquaredTo(here) < 4) {
            if (rc.getDirtCarrying() == 0 ) {//< RobotType.LANDSCAPER.dirtLimit) {
                if (hqLoc != null)
                    tryDig(hqLoc.directionTo(here), true);
                else
                    tryDig(randomDirection(), true);
            }

            if (rc.getCooldownTurns() < 1 && hqLoc != null) {
                // find best place to build
                MapLocation bestPlaceToBuildWall = here;
                int lowestElevation = 9999999;
                for (Direction dir : directions) {
                    MapLocation tileToCheck = hqLoc.add(dir);
                    if (rc.getLocation().distanceSquaredTo(tileToCheck) < 4
                            && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
                        int elevation = rc.senseElevation(tileToCheck);
                        if (rc.senseElevation(tileToCheck) < lowestElevation && (round > 1000 ||
                                Utils.getRoundFlooded(elevation) - round < 10)) {
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
        else if(hqLoc != null){
            goTo(hqLoc);
        }*/
        if(rc.getCooldownTurns()<1) {
        	if(!formLattice()) {
        		Utils.log("Tryna bounce around");
        		bounceAround();
        	}
        }
        comms.readMessages();

    }
    public static void separateRobots() {
    	nearbyAllies = new RobotInfo[100];
    	nearbyEnemies = new RobotInfo[100];
    	int i = 0;
    	int j = 0;
    	for(RobotInfo x : nearbyRobots) {
    		if(x.team == us) {
    			nearbyAllies[i++] = x;
    		}
    		else {
    			nearbyEnemies[j++] = x;
    		}
    	}
    	nearbyAlliesLen = i;
    	nearbyEnemiesLen = j;
    }
    static boolean bounceAround() throws GameActionException{
    	tryMove(randomDirection());
		return true;
    	
    }
    //Assumptions: hq is known, location is within sensor radius
    static boolean badLatticeLoc(MapLocation loc) throws GameActionException{
    	if(loc.x < 0 || loc.x > mapWidth || loc.y < 0 || loc.y > mapHeight)
    		return true;
    	if(loc.distanceSquaredTo(hqLoc)<=8)
    		return true;
    	RobotInfo possiblyUs = rc.senseRobotAtLocation(loc);
    	if(possiblyUs != null && possiblyUs.team == us) {
    		return true;
    	}
    	return false;
    }
    static boolean putDirt() throws GameActionException {
    	if((rc.senseElevation(here) < 4 || (Utils.getRoundFlooded(rc.senseElevation(here)) < round+MagicConstants.LATTICE_BUFFER)) && !(hqLoc.x%2 == here.x%2 && hqLoc.y%2 == here.y%2) && !badLatticeLoc(here)){
    		if(rc.canDepositDirt(Direction.CENTER)) {
    			rc.depositDirt(Direction.CENTER);
    			return true;
    		}
    	}
    	for(Direction d : directions) {
    		MapLocation tryLoc = here.add(d);
    		if((rc.senseElevation(tryLoc) < 4 || (Utils.getRoundFlooded(rc.senseElevation(tryLoc)-1)) < round) && !(hqLoc.x%2 == tryLoc.x%2 && hqLoc.y%2 == tryLoc.y%2) && !badLatticeLoc(tryLoc)) {
    			if(rc.canDepositDirt(d)) {
        			rc.depositDirt(d);
        			return true;
        		}
    		}
    	}
    	return false;
    	
    }
    static boolean digAHole() throws GameActionException{//TODO: convert to a smart switch statement
    	if(hqLoc.x%2 == here.x%2 && hqLoc.y%2 == here.y%2) //we're standing in a spot thats gonna be dug up... GET OUT
    		return false;
    	for(Direction d : directions) {
    		MapLocation tryLoc = here.add(d);
    		if(hqLoc.x%2 == tryLoc.x%2 && hqLoc.y%2 == tryLoc.y%2 && !badLatticeLoc(tryLoc)) {
    			return tryDig(d, false);
    		}
    	}
    	return false;
    	
    }
    static boolean formLattice() throws GameActionException{
    	if(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
    		return digAHole();
    	}
    	else {
    		return putDirt();
    	}
    }

    static boolean tryDig(Direction dir, boolean tryOthers) throws GameActionException {
        if(rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        if(tryOthers) {
            Direction dirL = dir.rotateLeft();
            Direction dirR = dir.rotateRight();
            while(dirL != dir) {
                if (rc.canDigDirt(dirL)) {
                    rc.digDirt(dirL);
                    return true;
                }
                if (rc.canDigDirt(dirR)) {
                    rc.digDirt(dirR);
                    return true;
                }
                dirL = dirL.rotateLeft();
                dirR = dirR.rotateRight();
            }
        }
        return false;
    }

}
