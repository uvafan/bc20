package v10_robust_lattice;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
    static boolean goTo(MapLocation destination) throws GameActionException {
        if(crunching)
            return Nav.goTo(destination, crunch);
        return Nav.goTo(destination, safe);
    }
    static boolean goToOnLattice(MapLocation destination) throws GameActionException {
        return Nav.goTo(destination, new SafetyPolicyAvoidAllUnitsAndLattice());
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static void explore() throws GameActionException {
        Nav.explore(safe);
    }

}
