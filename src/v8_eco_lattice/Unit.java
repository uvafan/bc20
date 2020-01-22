package v8_eco_lattice;

import battlecode.common.*;

public class Unit extends Bot {
    public Unit(RobotController r) throws GameActionException {
        super(r);
        findHQ();
    }

    @Override
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        here = rc.getLocation();
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
        return Nav.goTo(destination, new SafetyPolicyAvoidAllUnits());
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static void explore() throws GameActionException {
        Nav.explore(new SafetyPolicyAvoidAllUnits());
    }

}
