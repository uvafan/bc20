package v11_tuned;

import battlecode.common.*;

public class DesignSchool extends Building {
	
    public DesignSchool(RobotController r) throws GameActionException {
        super(r);
        if(enemyHQLoc != null && strat instanceof Rush)
            rushing = true;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

    public Direction getBuildDirection() {
        if(rushing)
            return here.directionTo(enemyHQLoc);
        else if(hqLoc != null && hqAttacked) {
            int minScore = Integer.MAX_VALUE;
            Direction ret = null;
            for(Direction dir: directions) {
                MapLocation loc = here.add(dir);
                int score = loc.distanceSquaredTo(hqLoc);
                if(score == 2)
                    score = 1;
                if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
                    for (RobotInfo ri : enemies) {
                        if (Utils.isBuilding(ri.type) && loc.distanceSquaredTo(ri.location) <= 2) {
                            score -= 1;
                            break;
                        }
                    }
                }
                if(score < minScore) {
                    minScore = score;
                    ret = dir;
                }
            }
            if(ret != null)
                return ret;
        }
        else if(here.distanceSquaredTo(hqLoc) == 2)
            return here.directionTo(hqLoc);
        return hqLoc.directionTo(here);
    }

}
