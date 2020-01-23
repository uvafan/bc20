package v8_eco_lattice;

import battlecode.common.*;

public class EcoLattice extends Strategy {

    // Java's (approximate) build order: 5 miners, fc, miner then:
    // not defending: vap, ds
    // defending: ds, vap
    // then keep an equal amount of drones, landscapers, vaps
    // Stop building vaps 250 rounds before RUSH ROUND
    // Build ds and fc at approximately 1/10 rate of drones/landscapers

    public EcoLattice(Bot b) {
        super(b);
    }

    public void resetPriorities() {
        soupPriorities = new int[]{
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
        };
        soupPriorities[RobotType.MINER.ordinal()] = 0;
    }


    public void updatePriorities(int[] unitCounts) throws GameActionException {
        resetPriorities();
        Utils.log("miners: " + unitCounts[RobotType.MINER.ordinal()]);
        Utils.log("drones: " + unitCounts[RobotType.DELIVERY_DRONE.ordinal()]);
        Utils.log("fulfillment centers: " + unitCounts[RobotType.FULFILLMENT_CENTER.ordinal()]);
        int numEnemies = bot.enemies.length;
        int numFriendlyDS = 0;
        boolean canSeeFriendlyHQ = false;
        for(RobotInfo ri: bot.friends) {
            if(ri.type == RobotType.DESIGN_SCHOOL)
                numFriendlyDS++;
            if(ri.type == RobotType.HQ)
                canSeeFriendlyHQ = true;
        }
        boolean seesEnemyNetGun = false;
        for(RobotInfo ri: bot.enemies) {
            if(ri.type == RobotType.NET_GUN)
                seesEnemyNetGun = true;
        }
        if(unitCounts[RobotType.MINER.ordinal()] >= 6 || numEnemies > 0) {
            soupPriorities[RobotType.MINER.ordinal()] = Integer.MAX_VALUE;
        }
        Utils.log("seesEnemyNetGun: " + seesEnemyNetGun);
        if(bot.hqAttacked) {
            if(bot.hqLoc != null && bot.here.distanceSquaredTo(bot.hqLoc) <= MagicConstants.RUSH_DEFENSE_DIST) {
                if ((!seesEnemyNetGun || unitCounts[RobotType.LANDSCAPER.ordinal()] > 0) && unitCounts[RobotType.FULFILLMENT_CENTER.ordinal()] == 0)
                    soupPriorities[RobotType.FULFILLMENT_CENTER.ordinal()] = 0;
                else if (numFriendlyDS == 0 && unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] == 0)
                    soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 0;
                else if (seesEnemyNetGun)
                    soupPriorities[RobotType.LANDSCAPER.ordinal()] = 0;
                else
                    updateBasedOnDesiredComp(unitCounts,
                            MagicConstants.RUSH_DEFENSE_ARMY_COMP,
                            MagicConstants.RUSH_DEFENSE_COMP_TYPES);
            }
            else {
                // nothing for now
            }
            Utils.log("Drone priority: " + soupPriorities[RobotType.DELIVERY_DRONE.ordinal()]);
            Utils.log("Landscaper priority: " + soupPriorities[RobotType.LANDSCAPER.ordinal()]);
        }
        else {
            if (unitCounts[RobotType.FULFILLMENT_CENTER.ordinal()] == 0)
                soupPriorities[RobotType.FULFILLMENT_CENTER.ordinal()] = 0;
            else if (unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] == 0)
                soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 0;
            else
                updateBasedOnDesiredComp(unitCounts,
                    MagicConstants.LATTICE_ARMY_COMP,
                    MagicConstants.LATTICE_COMP_TYPES);
        }
    }
}
