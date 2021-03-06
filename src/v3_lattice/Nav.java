package v3_lattice;
import battlecode.common.*;

interface NavSafetyPolicy {
	public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException;
}

class SafetyPolicyAvoidAllUnits extends Bot implements NavSafetyPolicy {

	public SafetyPolicyAvoidAllUnits() {
	}

	public boolean isSafeToMoveTo(MapLocation loc) throws GameActionException {
		switch(type) {
		case DELIVERY_DRONE:
			for (RobotInfo enemyNetGun:knownEnemyNetGuns) {
				if(loc.distanceSquaredTo(enemyNetGun.location)<= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
					return false;
				}
			}
			break;
		default:
				if(rc.senseFlooding(loc)) //change this to if the tile will flood next turn
					return false;
				for (RobotInfo enemyDrone: nearbyEnemyDrones) {
					if(loc.distanceSquaredTo(enemyDrone.location) <=2)
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
	private static int bugMovesSinceMadeProgress = 0;
	private static Direction lastRetreatDir;
	private static int boredom;
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

	private static void startBug() throws GameActionException {
		bugStartDistSq = here.distanceSquaredTo(dest);
		bugLastMoveDir = here.directionTo(dest);
		bugLookStartDir = here.directionTo(dest);
		bugRotationCount = 0;
		bugMovesSinceSeenObstacle = 0;
		bugMovesSinceMadeProgress = 0;
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
		if (bugMovesSinceSeenObstacle >= 4 || bugMovesSinceMadeProgress > MagicConstants.BUG_PATIENCE)
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
				bugMovesSinceMadeProgress = 0;
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
			bugMovesSinceMadeProgress++;
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
	public static void explore() throws GameActionException{
		if(lastExploreDir == null) {
			lastExploreDir = hqLoc.directionTo(here);
			boredom = 0;
		}
		if(boredom >= 5) {
			boredom = 0;
			lastExploreDir = (new Direction[] {
					lastExploreDir.rotateLeft(),
					lastExploreDir,
					lastExploreDir.rotateRight() })[rand.nextInt(3)];
		}
		if (canMove(lastExploreDir)) {
			move(lastExploreDir);
			return;
		}

		Direction[] dirs = new Direction[2];
		Direction dirLeft = lastExploreDir.rotateLeft();
		Direction dirRight = lastExploreDir.rotateRight();
			dirs[0] = dirLeft;
			dirs[1] = dirRight;
		for (Direction dir : dirs) {
			if (canMove(dir)) {
				move(dir);
				return;
			}
		}
		return;
	}
}