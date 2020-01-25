package v10_robust_lattice;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Rush extends Turtle {
    public Rush(Bot b) {
        super(b);
        soupPriorities[RobotType.MINER.ordinal()] = 0;
        soupPriorities[RobotType.LANDSCAPER.ordinal()] = 0;
        soupPriorities[RobotType.DELIVERY_DRONE.ordinal()] = 0;
        soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 450;
    }

    public void updatePriorities(int[] unitCounts) throws GameActionException {
        Utils.log("miners: " + unitCounts[RobotType.MINER.ordinal()]);
        Utils.log("drones: " + unitCounts[RobotType.DELIVERY_DRONE.ordinal()]);
        int numEnemies = bot.enemies.length;
        int numFriendlyDS = 0;
        for(RobotInfo ri: bot.friends) {
            if(ri.team == bot.enemy)
                numEnemies++;
            else if(ri.type == RobotType.DESIGN_SCHOOL)
                numFriendlyDS++;
        }
        Utils.log("friendly DS: " + numFriendlyDS);
        if(unitCounts[RobotType.MINER.ordinal()] >= 4) {
            soupPriorities[RobotType.MINER.ordinal()] = Integer.MAX_VALUE;
        }
        //if(unitCounts[RobotType.LANDSCAPER.ordinal()] >= 8) {
        //    soupPriorities[RobotType.LANDSCAPER.ordinal()] = Integer.MAX_VALUE;
        //}
        if(!bot.rushing && numEnemies <= unitCounts[RobotType.LANDSCAPER.ordinal()]) {
            soupPriorities[RobotType.LANDSCAPER.ordinal()] = 450;
        }
        if(!bot.rushing && numFriendlyDS > 0){
            soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = Integer.MAX_VALUE;
        }
        if(bot.rushing) {
            if(!bot.spotIsFreeAround(bot.enemyHQLoc))
                soupPriorities[RobotType.LANDSCAPER.ordinal()] = Integer.MAX_VALUE;
            else
                soupPriorities[RobotType.LANDSCAPER.ordinal()] = 0;
        }
        if(unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] >= 2) {
            soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = Integer.MAX_VALUE;
        }
        if(unitCounts[RobotType.DELIVERY_DRONE.ordinal()] >= 1) {
            soupPriorities[RobotType.DELIVERY_DRONE.ordinal()] = Integer.MAX_VALUE;
        }
    }

}
