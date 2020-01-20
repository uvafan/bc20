package v5_rush_op;

import battlecode.common.*;

public class Rush extends Strategy {
    public Rush(Bot b) {
        super(b);
        soupPriorities[RobotType.MINER.ordinal()] = 0;
        soupPriorities[RobotType.LANDSCAPER.ordinal()] = 0;
        soupPriorities[RobotType.DELIVERY_DRONE.ordinal()] = 0;
        soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 700;
    }

    public void updatePriorities(int[] unitCounts) {
        Utils.log("miners: " + unitCounts[RobotType.MINER.ordinal()]);
        Utils.log("drones: " + unitCounts[RobotType.DELIVERY_DRONE.ordinal()]);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, bot.enemy);
        if(enemies.length > 0)
            soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 0;
        if(unitCounts[RobotType.MINER.ordinal()] >= 3) {
            soupPriorities[RobotType.MINER.ordinal()] = Integer.MAX_VALUE;
        }
        //if(unitCounts[RobotType.LANDSCAPER.ordinal()] >= 8) {
        //    soupPriorities[RobotType.LANDSCAPER.ordinal()] = Integer.MAX_VALUE;
        //}
        if(unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] >= 2) {
            soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = Integer.MAX_VALUE;
        }
        if(unitCounts[RobotType.DELIVERY_DRONE.ordinal()] >= 1) {
            soupPriorities[RobotType.DELIVERY_DRONE.ordinal()] = Integer.MAX_VALUE;
        }
    }

}
