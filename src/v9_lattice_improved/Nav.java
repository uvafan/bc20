package v9_lattice_improved;
import battlecode.common.*;

interface NavSafetyPolicy {
	public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException;
}

class SafetyPolicyCrunch extends Bot implements NavSafetyPolicy {

	public SafetyPolicyCrunch() {
	}

	public boolean isSafeToMoveTo(MapLocation loc) {
		return true;
	}

}

class SafetyPolicyAvoidAllUnitsAndLattice extends Bot implements NavSafetyPolicy {
	public SafetyPolicyAvoidAllUnitsAndLattice() {
	}
	
	public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException {
		if(rc.senseFlooding(loc)) //change this to if the tile will flood next turn
			return false;
		if(hqLoc.x%2 == loc.x%2 && hqLoc.y%2 == loc.y%2)
			return false;
		for (RobotInfo e: enemies) {
			if(e.type == RobotType.DELIVERY_DRONE && loc.distanceSquaredTo(e.location) <=2)
				return false;
		}
		return true;
	}
	
}

class SafetyPolicyAvoidAllUnits extends Bot implements NavSafetyPolicy {

	public SafetyPolicyAvoidAllUnits() {
	}

	public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException {
		switch(type) {
		case DELIVERY_DRONE:
			for (int i=0; i<numEnemyNetGuns; i++) {
				if(invalidNetGun[i])
					continue;
				MapLocation eLoc = enemyNetGunLocs[i];
				if(loc.distanceSquaredTo(eLoc)<= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
					if(Utils.DEBUG)
						rc.setIndicatorLine(here, eLoc, 255, 0, 0);
					return false;
				}
			}
			for(RobotInfo e: enemies) {
				if(e.type == RobotType.NET_GUN && e.cooldownTurns < 1 && loc.distanceSquaredTo(e.location) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
					return false;
				}
			}
			if(enemyHQLoc != null && loc.distanceSquaredTo(enemyHQLoc) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED)
				return false;
			break;
		default:
				if(rc.senseFlooding(loc)) //change this to if the tile will flood next turn
					return false;
				//if(hqLoc.x%2 == loc.x%2 && hqLoc.y%2 == loc.y%2)
				//	return false;
				for (RobotInfo e: enemies) {
					if(e.type == RobotType.DELIVERY_DRONE && loc.distanceSquaredTo(e.location) <=2)
						return false;
				}
				break;
		}
		return true;
}
}

public class Nav extends Bot {

	private static MapLocation dest;
	private static NavSafetyPolicy safety;

	private enum BugState {
		DIRECT, BUG
	}

	public enum WallSide {
		LEFT, RIGHT
	}

	private static BugState bugState;
	public static WallSide bugWallSide = null;
	private static int bugStartDistSq;
	private static Direction bugLastMoveDir;
	private static Direction bugLookStartDir;
	private static int bugRotationCount;
	private static int bugMovesSinceSeenObstacle = 0;
	private static Direction lastRetreatDir;
	private static int boredom = 0;
	private static MapLocation exploreTarget;
	private static boolean move(Direction dir) throws GameActionException {
			rc.move(dir);
			return true;
	}

	private static boolean canMove(Direction dir) throws GameActionException {
		return rc.canMove(dir) && safety.isSafeToMoveTo(here.add(dir));
	}

	private static boolean tryMoveDirect() throws GameActionException {
		Direction toDest = here.directionTo(dest);

		if (canMove(toDest)) {
			move(toDest);
			return true;
		}

		Direction[] dirs = new Direction[2];
		Direction dirLeft = toDest.rotateLeft();
		Direction dirRight = toDest.rotateRight();
		if (here.add(dirLeft).distanceSquaredTo(dest) < here.add(dirRight).distanceSquaredTo(dest)) {
			dirs[0] = dirLeft;
			dirs[1] = dirRight;
		} else {
			dirs[0] = dirRight;
			dirs[1] = dirLeft;
		}
		for (Direction dir : dirs) {
			if (canMove(dir)) {
				move(dir);
				return true;
			}
		}
		return false;
	}

	public static boolean tryMoveDirect(MapLocation loc) throws GameActionException {
		safety = new SafetyPolicyAvoidAllUnits();
		Direction toDest = here.directionTo(loc);

		if (canMove(toDest)) {
			move(toDest);
			return true;
		}

		Direction[] dirs = new Direction[2];
		Direction dirLeft = toDest.rotateLeft();
		Direction dirRight = toDest.rotateRight();
		if (here.add(dirLeft).distanceSquaredTo(loc) < here.add(dirRight).distanceSquaredTo(loc)) {
			dirs[0] = dirLeft;
			dirs[1] = dirRight;
		} else {
			dirs[0] = dirRight;
			dirs[1] = dirLeft;
		}
		for (Direction dir : dirs) {
			if (canMove(dir)) {
				move(dir);
				return true;
			}
		}
		return false;
	}

	private static void startBug() throws GameActionException {
		bugStartDistSq = here.distanceSquaredTo(dest);
		bugLastMoveDir = here.directionTo(dest);
		bugLookStartDir = here.directionTo(dest);
		bugRotationCount = 0;
		bugMovesSinceSeenObstacle = 0;
		if (bugWallSide == null) {
			// try to intelligently choose on which side we will keep the wall
			Direction leftTryDir = bugLastMoveDir.rotateLeft();
			for (int i = 0; i < 3; i++) {
				if (!canMove(leftTryDir))
					leftTryDir = leftTryDir.rotateLeft();
				else
					break;
			}
			Direction rightTryDir = bugLastMoveDir.rotateRight();
			for (int i = 0; i < 3; i++) {
				if (!canMove(rightTryDir))
					rightTryDir = rightTryDir.rotateRight();
				else
					break;
			}
			if (dest.distanceSquaredTo(here.add(leftTryDir)) < dest.distanceSquaredTo(here.add(rightTryDir))) {
				bugWallSide = WallSide.RIGHT;
			} else {
				bugWallSide = WallSide.LEFT;
			}
		}

	}

	private static Direction findBugMoveDir() throws GameActionException {
		bugMovesSinceSeenObstacle++;
		Direction dir = bugLookStartDir;
		for (int i = 8; i-- > 0;) {
			if (canMove(dir))
				return dir;
			dir = (bugWallSide == WallSide.LEFT ? dir.rotateRight() : dir.rotateLeft());
			bugMovesSinceSeenObstacle = 0;
		}
		return null;
	}

	private static int numRightRotations(Direction start, Direction end) {
		return (end.ordinal() - start.ordinal() + 8) % 8;
	}

	private static int numLeftRotations(Direction start, Direction end) {
		return (-end.ordinal() + start.ordinal() + 8) % 8;
	}

	private static int calculateBugRotation(Direction moveDir) {
		if (bugWallSide == WallSide.LEFT) {
			return numRightRotations(bugLookStartDir, moveDir) - numRightRotations(bugLookStartDir, bugLastMoveDir);
		} else {
			return numLeftRotations(bugLookStartDir, moveDir) - numLeftRotations(bugLookStartDir, bugLastMoveDir);
		}
	}

	private static void bugMove(Direction dir) throws GameActionException {
		if (move(dir)) {
			bugRotationCount += calculateBugRotation(dir);
			bugLastMoveDir = dir;
			if (bugWallSide == WallSide.LEFT)
				bugLookStartDir = dir.rotateLeft().rotateLeft();
			else
				bugLookStartDir = dir.rotateRight().rotateRight();
		}
	}

	private static boolean detectBugIntoEdge() throws GameActionException {
		if (bugWallSide == WallSide.LEFT) {
			return !rc.onTheMap(here.add(bugLastMoveDir.rotateLeft()));
		} else {
			return !rc.onTheMap(here.add(bugLastMoveDir.rotateRight()));
		}
	}

	private static void reverseBugWallFollowDir() throws GameActionException {
		bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT : WallSide.LEFT);
		startBug();
	}

	private static void bugTurn() throws GameActionException {
		if (detectBugIntoEdge()) {
			reverseBugWallFollowDir();
		}
		Direction dir = findBugMoveDir();
		if (dir != null) {
			bugMove(dir);
		}
	}

	private static boolean canEndBug() {
		if (bugMovesSinceSeenObstacle >= 4)
			return true;
		return (bugRotationCount <= 0 || bugRotationCount >= 8) && here.distanceSquaredTo(dest) <= bugStartDistSq;
	}

	private static void bugMove() throws GameActionException {
		// Check if we can stop bugging at the *beginning* of the turn
//		rc.setIndicatorString(2, "I've been bugging for " +bugMovesSinceMadeProgress+ "turns.");
//		rc.setIndicatorString(1, "bugMovesSinceSeenObstacle = " +
//				 bugMovesSinceSeenObstacle + "; bugRotatoinCount = " +
//				 bugRotationCount);
		if (bugState == BugState.BUG) {
			if (canEndBug()) {
				bugState = BugState.DIRECT;
			}
		}

		// If DIRECT mode, try to go directly to target

		if (bugState == BugState.DIRECT) {
			if (!tryMoveDirect()) {
					bugState = BugState.BUG;
					startBug();
			}
		
		}
		// If that failed, or if bugging, bug
		if (bugState == BugState.BUG) {
			bugTurn();
		}
	}


	public static boolean goTo(MapLocation theDest, NavSafetyPolicy theSafety) throws GameActionException {
		if (!theDest.equals(dest)) {
			dest = theDest;
			bugState = BugState.DIRECT;
		}

		if (here.equals(dest))
			return false;

		safety = theSafety;

		bugMove();
		return true;
	}
	//exploring is fearless! if there are nearby enemies we should flee instead (also implement fleeing)
	public static void explore(NavSafetyPolicy theSafety) throws GameActionException{
		Utils.log("exploring!");
		safety = theSafety;
		if(lastExploreDir == null) {
		    //if(hqLoc != null)
			//	lastExploreDir = hqLoc.directionTo(here);
		    //else
			lastExploreDir = randomDirection();
			boredom = 0;
		}
		if(boredom >= MagicConstants.EXPLORE_BOREDOM) {
			boredom = 0;
			lastExploreDir = (new Direction[] {
					lastExploreDir.rotateLeft().rotateLeft(),
					lastExploreDir.rotateLeft(),
					lastExploreDir,
					lastExploreDir.rotateRight(),
					lastExploreDir.rotateRight().rotateRight()})[rand.nextInt(5)];
		}
		boredom++;
		if(!rc.onTheMap(here.add(lastExploreDir))) {
			lastExploreDir = lastExploreDir.opposite();
		}
		if (canMove(lastExploreDir)) {
			move(lastExploreDir);
			return;
		}
		Direction[] dirs = new Direction[4];
		Direction dirLeft = lastExploreDir.rotateLeft();
		Direction dirRight = lastExploreDir.rotateRight();
		Direction dirLeftLeft = dirLeft.rotateLeft();
		Direction dirRightRight = dirRight.rotateRight();
		dirs[0] = dirLeft;
		dirs[1] = dirRight;
		dirs[2] = dirLeftLeft;
		dirs[3] = dirRightRight;
		for (Direction dir : dirs) {
			if (canMove(dir)) {
				move(dir);
				return;
			}
		}
		return;
	}
}