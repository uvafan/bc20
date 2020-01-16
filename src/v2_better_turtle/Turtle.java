package v2_better_turtle;

import battlecode.common.RobotType;

public class Turtle extends Strategy {
    public Turtle(Bot b) {
        super(b);
        soupPriorities[RobotType.MINER.ordinal()] = 0;
        soupPriorities[RobotType.LANDSCAPER.ordinal()] = 0;
        soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 0;
        soupPriorities[RobotType.VAPORATOR.ordinal()] = 500;
    }

    public void updatePriorities(int[] unitCounts) {
        Utils.log("miners: " + unitCounts[RobotType.MINER.ordinal()]);
        if(unitCounts[RobotType.MINER.ordinal()] >= 10) {
            soupPriorities[RobotType.MINER.ordinal()] = 1000;
        }
        if(unitCounts[RobotType.LANDSCAPER.ordinal()] >= 8) {
            soupPriorities[RobotType.LANDSCAPER.ordinal()] = 1000;
        }
        if(unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] >= 1) {
            soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 1000;
        }
    }

}
