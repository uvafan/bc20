package v13_post_quals.v12_quals_bot;

import battlecode.common.GameActionException;
import battlecode.common.RobotType;

public class Turtle extends Strategy {
    public Turtle(Bot b) {
        super(b);
        soupPriorities[RobotType.MINER.ordinal()] = 0;
        soupPriorities[RobotType.LANDSCAPER.ordinal()] = 0;
        soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 0;
    }

    public void updatePriorities(int[] unitCounts) throws GameActionException {
        Utils.log("miners: " + unitCounts[RobotType.MINER.ordinal()]);
        if(unitCounts[RobotType.MINER.ordinal()] >= 10) {
            soupPriorities[RobotType.MINER.ordinal()] = Integer.MAX_VALUE;
        }
        if(unitCounts[RobotType.LANDSCAPER.ordinal()] >= 8) {
            soupPriorities[RobotType.LANDSCAPER.ordinal()] = Integer.MAX_VALUE;
        }
        if(unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] >= 1) {
            soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = Integer.MAX_VALUE;
        }
    }

}
