package v14_final_bot;

import battlecode.common.*;

public class Landscaper extends Unit {

	public static MapLocation ourDesignSchool = null;
	public static boolean turtling = false;
	public static boolean defending = false;
	public static boolean doneDefending = false;

	public static MapLocation target = null;
	public static int patience = 0;

	public static boolean shouldRemoveDirt = false;
	public static boolean wouldDigFromLoc = false;
	
	public static boolean urgentlyNeedDirt = false;
	public static MapLocation urgentTile = null;
	
	public static boolean floodingNearMyHQ = false;

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
		if(!rushing && rc.getCooldownTurns() < 1 && round < MagicConstants.CRUNCH_ROUND) {
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
					rc.setIndicatorLine(here, buildingToBury.location, 255, 0, 0);
					buryEnemy(buildingToBury);
				}
				if (turtling && rc.getCooldownTurns() < 1)
					doTurtle();
				if(rc.getCooldownTurns() < 1)
					doLattice();
			}
		}
		comms.readMessages();
	}
	public static void updateFlooding() throws GameActionException {
		floodingNearMyHQ = false;
		int i = 0;
		while(i++ < 9) {
			MapLocation testTile = hqLoc.translate(nearbyXOffsets[i], nearbyYOffsets[i]);
			if(rc.canSenseLocation(testTile) && (rc.senseFlooding(testTile))) {
				floodingNearMyHQ = true;
				return;
			}
		}
	}
	public static boolean needsUrgentFixing(MapLocation loc) throws GameActionException {
        int roundsLeft = Utils.getRoundFlooded(rc.senseElevation(loc)) - round;
        return  roundsLeft <= 50 && rc.senseElevation(loc) > 0-MagicConstants.WATER_TOLERANCE && !isWallComplete;
    }
	private void checkUrgentDirt() throws GameActionException {
		if(here.distanceSquaredTo(hqLoc) <= 8 && rc.getDirtCarrying() == 0 && floodingNearMyHQ) {
			int i = 0;
			while(i++ < 9) {
				MapLocation testTile = hqLoc.translate(nearbyXOffsets[i], nearbyYOffsets[i]);
				if(rc.canSenseLocation(testTile) && (rc.senseFlooding(testTile) || needsUrgentFixing(testTile)) ) {
					RobotInfo possiblyUs = rc.senseRobotAtLocation(testTile);
					if(possiblyUs != null && possiblyUs.team == us && Utils.isBuilding(possiblyUs.type)) {
						continue;
					}
					Utils.log("I urgently need dirt so i can patch up: " + testTile);
					urgentTile = testTile;
					urgentlyNeedDirt = true;
					return;
				}
			}
		}
		urgentlyNeedDirt = false;
	}
	private int turnDist(MapLocation a, MapLocation b) {
		return Math.max(Math.abs(a.x-b.x), Math.abs(a.y-b.y))*10 + Math.min(Math.abs(a.x-b.x), Math.abs(a.y-b.y));
	}
	public void secretTurtle() throws GameActionException {
		if(here.distanceSquaredTo(hqLoc) >= 8 && !isActuallyOnWall(here)){
			goToOnLattice(hqLoc);
		}
		else if(here.distanceSquaredTo(hqLoc)<= 8) {
			if(enemyHQLoc != null && (here.distanceSquaredTo(hqLoc) <=2 || round < Utils.getRoundFlooded(MagicConstants.LATTICE_HEIGHT)))
				goToOnLattice(enemyHQLoc);
			else {
				if(rc.getDirtCarrying() == 0) {
					tryDig(Direction.CENTER,true);
					}
				else {
					int minHeight = Integer.MAX_VALUE;
					Direction bestDir = hqLoc.directionTo(here);
					for(Direction d : cardinalsPlusCenter) {
						MapLocation testLoc = here.add(d);
						if(isActuallyOnWall(testLoc)) {
							int h = rc.senseElevation(testLoc);
							if(h < minHeight && rc.canDepositDirt(d)) {
								minHeight = h;
								bestDir = d;
							}
						}
					}
					if(rc.canDepositDirt(bestDir)) {
						rc.depositDirt(bestDir);
					}
				}
			}
		}else {
				Direction myDirtDir = hqLoc.directionTo(here);
				MapLocation bestLoc = here.add(myDirtDir);
				for(Direction d : directions) {
					MapLocation testLoc = here.add(d);
					if(testLoc.x % 2 == hqLoc.x % 2 && testLoc.y % 2 == hqLoc.y % 2 && testLoc.distanceSquaredTo(hqLoc) >= 13 && testLoc.distanceSquaredTo(hqLoc) != 18) {
						bestLoc = testLoc;
						myDirtDir = d;
						break;
					}
				}
				if(rc.getDirtCarrying() == 0) {
				tryDig(myDirtDir,true);
				}

				boolean needToBuryBuilding = false;
				int minHeight = Integer.MAX_VALUE;
				Direction bestDir = Direction.CENTER;
				for(Direction d : cardinalsPlusCenter) {
					MapLocation testLoc = here.add(d);
					if(isActuallyOnWall(testLoc)) {
						int h = rc.senseElevation(testLoc);
						RobotInfo possiblyUs = rc.senseRobotAtLocation(testLoc);
						if(possiblyUs != null && Utils.isBuilding(possiblyUs.type)) {
							h-=25;
							needToBuryBuilding = true;
						}
						if(h < minHeight) {
							minHeight = h;
							bestDir = d;
						}
					}
				}
				if( rc.canDepositDirt(bestDir) && (needToBuryBuilding || (Utils.getRoundFlooded(minHeight-1) < round) || round > 1800)) {
					rc.depositDirt(bestDir);
				}
				else if (rc.getDirtCarrying() < 25) {
					tryDig(myDirtDir,true);
				}
				else {
					if(!moveAroundIfShould()) {
						rc.depositDirt(bestDir);
					}
				}
			}
		
	}
	private Direction getCounterClockwiseWallDir(MapLocation here) {
		Direction d = here.directionTo(hqLoc);
			for(int i = 0; i < 5; i++) {
				d = d.rotateRight();
				if(isActuallyOnWall(here.add(d))){
					return d;
				}
			}
		return Direction.CENTER;
	}
	private boolean moveAroundIfShould() throws GameActionException {
		MapLocation nextLoc = here.add(getCounterClockwiseWallDir(here));
		MapLocation nextNextLoc = nextLoc.add(getCounterClockwiseWallDir(nextLoc));
		if(!nextNextLoc.equals(here)) {
			RobotInfo r1 = rc.senseRobotAtLocation(nextLoc);
			RobotInfo r2 = rc.senseRobotAtLocation(nextLoc);
			if(r1 == null || r1.type != RobotType.LANDSCAPER  && r2 == null || r2.type != RobotType.LANDSCAPER) {
				if(rc.canMove(here.directionTo(nextLoc)) && rc.senseElevation(nextNextLoc) < rc.senseElevation(here)) {
					rc.move(here.directionTo(nextLoc));
					return true;
				}
			}
		}
		return false;
	}

	private void doLattice() throws GameActionException {
		updateFlooding();
		Utils.log("I'm a lattice landscaper!");
		//System.out.println(Clock.getBytecodesLeft() + " bytecodes left.");
		if(round > MagicConstants.CRUNCH_ROUND - MagicConstants.MOVE_OUT_OF_CRUNCH_WAY) {
			if(MagicConstants.suicideOnCrunch && here.distanceSquaredTo(enemyHQLoc) <= 25 && round == MagicConstants.CRUNCH_ROUND - 1){
				rc.disintegrate();
			}
			else {
				secretTurtle();
			}
		}
		int digging = rc.getDirtCarrying();
		if(digging == 0 || digging == RobotType.LANDSCAPER.dirtLimit) {
			checkUrgentDirt();
			Utils.log("Case 1");
			MapLocation bestDigLoc = null;
			int i = 0;
			while(i < nearbyXOffsets.length) {
				MapLocation testTile = here.translate(nearbyXOffsets[i], nearbyYOffsets[i]);
				int dist = here.distanceSquaredTo(testTile);
				if(dist > rc.getCurrentSensorRadiusSquared()) {
					break;
				}
				//Utils.log("Evaluating a tile..." + testTile);
				if(shouldDig(testTile, digging==0)) {
					Utils.log("A tile I should dig is..." + testTile);
					bestDigLoc = testTile;
					break;
				}
				i++;
			}
			if(bestDigLoc!=null) {
				Utils.log("Give me a dead tile at: " + bestDigLoc.x + ", " + bestDigLoc.y);
				if(here.distanceSquaredTo(bestDigLoc) <=2 ) {
					target = null;
					patience = 0;
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
			//System.out.println(Clock.getBytecodesLeft() + " bytecodes left.");
			MapLocation bestDirtLoc = null;
			MapLocation bestStockLoc = null;
			int minDist = Integer.MAX_VALUE;
			int minStockDist = Integer.MAX_VALUE;
			boolean wouldDigIfRenovate = false;
			int i = 0;
			if(urgentTile == null || (rc.canSenseLocation(urgentTile) && !needsUrgentFixing(urgentTile) && !rc.senseFlooding(urgentTile))){
				urgentTile = null;
				while(i < nearbyXOffsets.length) {
					MapLocation testTile = here.translate(nearbyXOffsets[i], nearbyYOffsets[i]);
					int dist = here.distanceSquaredTo(testTile);
					if(dist > rc.getCurrentSensorRadiusSquared() || Clock.getBytecodesLeft() < 2000) {
						break;
					}
					int realDist = turnDist(here,testTile);
					int distToHQ = Math.min(turnDist(hqLoc,testTile), MagicConstants.BUBBLE_AROUND_HQ)*3;
					int distToEnemy = enemyHQLoc == null ? 0 : turnDist(enemyHQLoc,testTile)*2;
					int dontNavMod = dist <= 2 ? 0 : 100;
					int mainWallMod = 200;
					int floodedMod = 500;
					if(hqLoc.distanceSquaredTo(testTile) <= 8  && here.distanceSquaredTo(hqLoc) <= 8 && (rc.canSenseLocation(testTile) &&(rc.senseFlooding(testTile) || needsUrgentFixing(testTile)))) {
						floodedMod = 0;
					}
					int maybeWall = testTile.distanceSquaredTo(hqLoc);
					if(maybeWall <= 18) {
						mainWallMod = 0;
					}
					switch(maybeWall) {
					case 16:
					case 17:
						mainWallMod = 200;
						break;
					default:
					}
					int value = realDist + distToHQ + mainWallMod + dontNavMod + distToEnemy + floodedMod;
					//Utils.log(testTile.x + ", " + testTile.y + ": " + value);
					if(shouldRenovate(testTile)) {
						if(!wouldDigFromLoc) {
							if(value < minDist) {
								wouldDigIfRenovate = shouldRemoveDirt;
								bestDirtLoc = testTile;
								minDist = value;
							}
						}
						else {
							if(dist < minStockDist) {
								bestStockLoc = testTile;
								minStockDist = dist;
							}
						}
					}
					i++;
				}
			}else {
				bestDirtLoc = urgentTile;
				wouldDigIfRenovate = false;
			}
			//System.out.println(Clock.getBytecodesLeft() + " bytecodes left.");
			Utils.log("best dirt Loc" + bestDirtLoc + ", " + minDist);
			Utils.log("Best stock loc" + bestStockLoc+ ", " + minStockDist);
			if(bestDirtLoc!=null && here.distanceSquaredTo(bestDirtLoc) <=2 ) {
				target = null;
				patience = 0;
				if(wouldDigIfRenovate) {
					tryDig(here.directionTo(bestDirtLoc),false);
				}
				else {
					if(rc.canDepositDirt(here.directionTo(bestDirtLoc))) {
						rc.depositDirt(here.directionTo(bestDirtLoc));
					}
				}

			}
			else if (!urgentlyNeedDirt && bestStockLoc != null && here.distanceSquaredTo(bestStockLoc) <= 2 && digging < 24) {
				target = null;
				patience = 0;
				tryDig(here.directionTo(bestStockLoc),false);
			}
			else{
				if(target == null) {
					target = bestDirtLoc;
				}
			}
		}

		if(rc.getCooldownTurns()<1) {
			Utils.log("Case 3");
			if(target != null) {
				if(turnDist(target,hqLoc) >= 50 && !isWallComplete) {
					target = hqLoc;
				}
				Utils.log("I'm REALLY WANT TO GO TO: " + target.x + ", " + target.y);
				Utils.log("I'm currenty at: " + here.x + ", " + here.y);
				if(here.distanceSquaredTo(target) <= 2 || patience >= MagicConstants.GIVE_UP_ON_TARGET) {
					target = null;
					patience = 0;
				}
				else {
					Utils.log("Well, hope I can get there");
					patience++;
					goToOnLattice(target, !target.equals(hqLoc), false);
				}
			}
			else if (enemyHQLoc != null) {
					Utils.log("BRING DOWN THAT WALL");
					goToOnLattice(enemyHQLoc);
			}
			else if (hqLoc != null){
				goToOnLattice(reflectR(hqLoc));
			}
		}
	}
	private boolean shouldRenovate(MapLocation testTile) throws GameActionException {
		//Utils.log("I'm trying to sense: " + testTile.x + ", " + testTile.y);
		// set wouldRenovateI
		//public static boolean shouldRemoveDirt = false;
		//public static boolean wouldDigFromLoc = false;
		if(!badLatticeLoc(testTile)) {
			int elev = rc.senseElevation(testTile);
			if(hqLoc.distanceSquaredTo(testTile) <= 8) {
				//Utils.log("her1e");
				if(rc.senseElevation(here) < MagicConstants.LATTICE_HEIGHT || hqLoc.distanceSquaredTo(testTile) >= 4) {
					RobotInfo possiblyUs = rc.senseRobotAtLocation(testTile);
					if(possiblyUs != null && possiblyUs.team == us && Utils.isBuilding(possiblyUs.type)) {
						//Utils.log("her1e2");
						if(possiblyUs.dirtCarrying > 0) {
							shouldRemoveDirt = true;
							wouldDigFromLoc = false;
							return true;
						}
						else {
							return false;
						}
					}
					if(sensedHQElevation && elev < hqElevation || rc.senseFlooding(testTile) && elev > (0- MagicConstants.WATER_TOLERANCE)) {
						//Utils.log("her13");
						shouldRemoveDirt = false;
						wouldDigFromLoc = false;
						return true;
					}
					if(Utils.getRoundFlooded(elev-1) >= round) {
						if(sensedHQElevation && elev > hqElevation +3 && elev <= hqElevation +3 + (testTile.distanceSquaredTo(hqLoc) < 100 ? MagicConstants.WATER_TOLERANCE : MagicConstants.LATTICE_TOLERANCE)) {
							//Utils.log("her14");
							shouldRemoveDirt = true;
							wouldDigFromLoc = false;
							return true;
						}
						if(sensedHQElevation && elev > hqElevation) {
							//Utils.log("her5e");
							shouldRemoveDirt = true;
							wouldDigFromLoc = true;
							return true;
						}
					}
				}
			}
			else {
				//Utils.log("Got here1");
				RobotInfo possiblyUs = rc.senseRobotAtLocation(testTile);
				if(possiblyUs != null && possiblyUs.team == us && Utils.isBuilding(possiblyUs.type)) {
					if(possiblyUs.dirtCarrying > 0) {
						shouldRemoveDirt = true;
						wouldDigFromLoc = false;
						return true;
					}
					else {
						return false;
					}
				}
				if(!(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2)) {
					//Utils.log("Got here4");
					//Utils.log(""+elev);
					if(elev < MagicConstants.LATTICE_HEIGHT && (elev > (turnDist(testTile,hqLoc) < 50 ? 0- MagicConstants.WATER_TOLERANCE : MagicConstants.LATTICE_HEIGHT - MagicConstants.LATTICE_TOLERANCE))) {
						//Utils.log("Got here2");
						shouldRemoveDirt = false;
						wouldDigFromLoc = false;
						return true;
					}
					else if (elev > MagicConstants.LATTICE_HEIGHT + 3 && (elev < MagicConstants.LATTICE_HEIGHT + 3 + (testTile.distanceSquaredTo(hqLoc) < 100 ? MagicConstants.WATER_TOLERANCE : MagicConstants.LATTICE_TOLERANCE))) {
						shouldRemoveDirt = true;
						wouldDigFromLoc = false;
						return true;
					}
					else if(elev <= MagicConstants.LATTICE_HEIGHT + 3 && elev > MagicConstants.LATTICE_HEIGHT) {
						shouldRemoveDirt = true;
						wouldDigFromLoc = true;
						return true;
					}
				}
				else {
					shouldRemoveDirt = true;
					wouldDigFromLoc = true;
					return true;
				}
			}
		}
		//Utils.log("Got here5");
		return false;
	}

	static boolean badLatticeLoc(MapLocation loc) throws GameActionException{
		if(loc.x < 0 || loc.x >= mapWidth || loc.y < 0 || loc.y >= mapHeight)
			return true;
		return false;
	}
	public boolean shouldDig(MapLocation testTile, boolean actuallyDigging) throws GameActionException {
		if(!badLatticeLoc(testTile)) {
			if(actuallyDigging) {
				RobotInfo possiblyUs = rc.senseRobotAtLocation(testTile);
				if(possiblyUs != null && possiblyUs.team == us && Utils.isBuilding(possiblyUs.type)) {
					return possiblyUs.dirtCarrying != 0;
				}
				if(hqLoc.distanceSquaredTo(testTile) <= 8) {
					if(rc.senseElevation(testTile) <= hqElevation || rc.senseFlooding(testTile) && rc.senseElevation(testTile) > 0- MagicConstants.WATER_TOLERANCE) {
						return false;
					}else {
						return true;
					}
				}
				else if(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2 || rc.senseElevation(testTile) > MagicConstants.LATTICE_HEIGHT)
					return true;
				else if (urgentlyNeedDirt && here.distanceSquaredTo(hqLoc)<= 8 && (Utils.getRoundFlooded(rc.senseElevation(testTile)-1) >= round || rc.senseFlooding(testTile))){
					return true;
				}
			}
			else {
				RobotInfo possiblyUs = rc.senseRobotAtLocation(testTile);
				if(possiblyUs != null && possiblyUs.team == us && Utils.isBuilding(possiblyUs.type)) {
					return false;
				}
				if(hqLoc.distanceSquaredTo(testTile) <= 8) {
					if(sensedHQElevation && rc.senseElevation(testTile) < hqElevation+3 || rc.senseFlooding(testTile) && rc.senseElevation(testTile) > 0- MagicConstants.WATER_TOLERANCE ) {
						return true;
					}else {
						return false;
					}
				}
				else if(hqLoc.x%2 == testTile.x%2 && hqLoc.y%2 == testTile.y%2 || rc.senseElevation(testTile) < MagicConstants.LATTICE_HEIGHT+3)
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
		//System.out.println("defending!");
		if(!rc.canSenseLocation(hqLoc)) {
			goTo(hqLoc);
			return;
		}
		RobotInfo ret = null;
		RobotInfo[] enemyBuildings = new RobotInfo[50];
		int numEnemyBuildings = 0;
		for(RobotInfo e: enemies) {
			if(Utils.isBuilding(e.type) && !(!here.isAdjacentTo(e.location) && here.distanceSquaredTo(e.location) <= 13 && !spotIsFreeAround(e.location))) {
				enemyBuildings[numEnemyBuildings++] = e;
			}
		}
		MapLocation adjFriendlyBuilding = null;
		RobotInfo[] friendlyBuildings = new RobotInfo[50];
		MapLocation closestFriendlyBuilding = null;
		int minFDist = Integer.MAX_VALUE;
		int numFriendlyBuildings = 0;
		for(RobotInfo f: friends) {
			if(Utils.isBuilding(f.type) && f.type != RobotType.HQ) {
				friendlyBuildings[numFriendlyBuildings++] = f;
				int dist = f.location.distanceSquaredTo(here);
				if(f.dirtCarrying > 0 || (landscaperDiff(f.location) < 0 || (dist <= 2 && landscaperDiff(f.location) < 1))) {
				    if(f.dirtCarrying > 0 && dist <= 2)
						adjFriendlyBuilding = f.location;
				    if(dist < minFDist) {
				    	closestFriendlyBuilding = f.location;
				    	minFDist = dist;
					}
				}
			}
		}
		int spotPriority = Integer.MIN_VALUE;
		MapLocation spot = null;
		int spotsFree = 0;
		int herePriority = Integer.MIN_VALUE;
		for(Direction dir: directions) {
			MapLocation loc = hqLoc.add(dir);
			if(!rc.canSenseLocation(loc) || (!loc.equals(here) && !canReach(loc, here, true))) {
				continue;
			}
			spotsFree += loc.equals(here) ? 0 : 1;
			int priority = -here.distanceSquaredTo(loc) * MagicConstants.SPOT_DIST_MULTIPLIER;
			priority += (loc.equals(here) ? MagicConstants.STAY_HERE_BONUS: 0);
			int minDist = MagicConstants.MAX_BUILDING_DIST;
			int friendlyMinDist = MagicConstants.MAX_BUILDING_DIST;
			for(int i=0; i<numEnemyBuildings; i++) {
				RobotInfo ri = enemyBuildings[i];
				minDist = Math.min(minDist, loc.distanceSquaredTo(ri.location));
			}
			for(int i=0; i<numFriendlyBuildings; i++) {
				RobotInfo ri = friendlyBuildings[i];
				friendlyMinDist = Math.min(minDist, loc.distanceSquaredTo(ri.location));
			}
			priority += (minDist <= 2 ? MagicConstants.NEXT_TO_BUILDING_BONUS : (MagicConstants.MAX_BUILDING_DIST - minDist) / 2);
			priority += (friendlyMinDist <= 2 ? MagicConstants.NEXT_TO_FRIENDLY_BUILDING_BONUS : (MagicConstants.MAX_BUILDING_DIST - minDist) / 2);
			if(loc.equals(here))
				herePriority = priority;
			if(priority > spotPriority) {
				spotPriority = priority;
				spot = loc;
			}
		}
		if(spot != null)
			spotPriority += spotsFree * MagicConstants.SPOTS_FREE_MULTIPLIER;
		if(here.isAdjacentTo(hqLoc))
			herePriority += 8 * MagicConstants.SPOTS_FREE_MULTIPLIER;
		if(herePriority > spotPriority) {
			spot = here;
			spotPriority = herePriority;
		}
		int myDirt = rc.getDirtCarrying();
		int dirtOnHQ = rc.senseRobotAtLocation(hqLoc).dirtCarrying;
		if(spot != null) {
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
		/*
		if(spot != null)
			System.out.println("spot is " + spot + " priority: " + spotPriority);
		if(building != null)
			System.out.println("building at " + building + " priority: " + buildingPriority);
		*/
		if(spot == null && building == null) {
			if(closestFriendlyBuilding == null)
				doneDefending = true;
			else if(here.isAdjacentTo(closestFriendlyBuilding)) {
				if(myDirt < 25)
					tryDig(here.directionTo(closestFriendlyBuilding), false);
				if(rc.senseRobotAtLocation(closestFriendlyBuilding).dirtCarrying > 0) {
					if(rc.canDepositDirt(closestFriendlyBuilding.directionTo(here)))
						rc.depositDirt(closestFriendlyBuilding.directionTo(here));
				}
			}
			else {
				goTo(closestFriendlyBuilding);
			}
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
				if(adjFriendlyBuilding != null)
					tryDig(here.directionTo(adjFriendlyBuilding), false);
				tryDig(adjBuilding.directionTo(here), true);
			}
			else if(adjFriendlyBuilding != null && myDirt < 25) {
				tryDig(here.directionTo(adjFriendlyBuilding), false);
			}
			else if (dirtOnHQ > 0 || adjFriendlyBuilding != null){
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
					if(adjFriendlyBuilding != null)
						tryDig(here.directionTo(adjFriendlyBuilding), false);
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

	public int getBuryingPriority(RobotType rt) {
		switch (rt) {
			case VAPORATOR: return MagicConstants.VAP_PRIORITY;
			case NET_GUN: return MagicConstants.NET_GUN_PRIORITY;
			case HQ: return MagicConstants.HQ_PRIORITY;
			default: return 0;
		}
	}

	private RobotInfo getBuildingToBury() throws GameActionException {
		RobotInfo ret = null;
		int maxPriority = Integer.MIN_VALUE;
		for(RobotInfo e: enemies) {
			if(!Utils.isBuilding(e.type))
				continue;
			int dist = here.distanceSquaredTo(e.location);
			int manhattan = Utils.manhattan(here, e.location);
			if(dist < 3)
				manhattan = 1;
			if(!hqAttacked && dist > 2 && !canReachAdj(e.location, true))
				continue;
			int priority = getBuryingPriority(e.type) - manhattan;
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
		boolean closerToUs = (enemyHQLoc == null || here.distanceSquaredTo(hqLoc) < here.distanceSquaredTo(enemyHQLoc));
		if(here.distanceSquaredTo(closestEnemyBuilding.location) <= 2) {
			if(rc.getDirtCarrying() == 0) {
				int maxScore = Integer.MIN_VALUE;
				Direction bestDir = Direction.CENTER;
				for(Direction d: directionsPlusCenter) {
					MapLocation loc = here.add(d);
					if(rc.canDigDirt(d)) {
					    int score = 0;
					    RobotInfo ri = rc.senseRobotAtLocation(loc);
					    if(ri != null) {
					    	if(Utils.isBuilding(ri.type) && ri.dirtCarrying > 0) {
					    		if(ri.team == us)
					    			score += 10000;
					    		else
									score -= 10000;
							}
					    	else {
								if(ri.team == enemy)
									score += 100;
								else
									score -= 100;
							}
						}
					    if(isActuallyOnWall(loc)) {
					    	score -= 100000;
					    }
					    if(closerToUs) {
					    	if(loc.x % 2 == hqLoc.x % 2 && loc.y % 2 == hqLoc.y % 2) {
					    		score += 500;
							}
						}
					    else {
					    	score -= Utils.manhattan(loc, enemyHQLoc);
					    	score -= (rc.senseFlooding(loc) ? 50 : 0);
						}
						if(score > maxScore) {
							maxScore = score;
							bestDir = d;
						}
					}
				}
				tryDig(bestDir, false);
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
		else if (round >= MagicConstants.CRUNCH_ROUND) {
			goTo(targetLoc);
		}
	}

	public void doRush() throws GameActionException {
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
