package v11_smarter_miners;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

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
        if(unitCounts[RobotType.MINER.ordinal()] >= MagicConstants.NUM_NON_BUILD_MINERS + MagicConstants.INITIAL_BUILD_MINERS || bot.hqAttacked && bot.unitCounts[RobotType.MINER.ordinal()] >= MagicConstants.NUM_RUSH_DEFENSE_MINERS) {
            soupPriorities[RobotType.MINER.ordinal()] = Integer.MAX_VALUE;
        }
        if(bot.isWallComplete && bot.type == RobotType.HQ) {
            int expectedMiners = MagicConstants.NUM_NON_BUILD_MINERS + MagicConstants.INITIAL_BUILD_MINERS + (bot.round - bot.wallCompletionRound) / MagicConstants.NEW_BUILD_MINER_FREQ;
            if(bot.numMiners < expectedMiners)
                soupPriorities[RobotType.MINER.ordinal()] = RobotType.MINER.cost + 1;
        }
        Utils.log("seesEnemyNetGun: " + seesEnemyNetGun);
        if(bot.hqAttacked) {
            if(bot.hqLoc != null && bot.here.distanceSquaredTo(bot.hqLoc) <= MagicConstants.RUSH_DEFENSE_DIST) {
                if ((!seesEnemyNetGun || unitCounts[RobotType.LANDSCAPER.ordinal()] > 3) && unitCounts[RobotType.FULFILLMENT_CENTER.ordinal()] == 0)
                    soupPriorities[RobotType.FULFILLMENT_CENTER.ordinal()] = RobotType.FULFILLMENT_CENTER.cost + 1;
                else if (numFriendlyDS == 0 && unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] == 0 && (unitCounts[RobotType.DELIVERY_DRONE.ordinal()] > 0 || seesEnemyNetGun || bot.numEnemyNetGuns > 0))
                    soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = RobotType.DESIGN_SCHOOL.cost + 1;
                else if (seesEnemyNetGun)
                    soupPriorities[RobotType.LANDSCAPER.ordinal()] = RobotType.LANDSCAPER.cost + 1;
                else
                    updateBasedOnDesiredComp(unitCounts,
                            MagicConstants.RUSH_DEFENSE_ARMY_COMP,
                            MagicConstants.RUSH_DEFENSE_COMP_TYPES);
            }
            else {
                // nothing for now
            }
            if(MagicConstants.BUILD_REFINERY && unitCounts[RobotType.REFINERY.ordinal()] == 0) {
                // leave buffer for refineries
                for(RobotType type: MagicConstants.RUSH_DEFENSE_COMP_TYPES) {
                    if(soupPriorities[type.ordinal()] < Integer.MAX_VALUE) {
                        soupPriorities[type.ordinal()] += MagicConstants.REFINERY_BUFFER;
                    }
                }
            }
            Utils.log("Drone priority: " + soupPriorities[RobotType.DELIVERY_DRONE.ordinal()]);
            Utils.log("Landscaper priority: " + soupPriorities[RobotType.LANDSCAPER.ordinal()]);
        }
        else {
            if (unitCounts[RobotType.FULFILLMENT_CENTER.ordinal()] == 0)
                soupPriorities[RobotType.FULFILLMENT_CENTER.ordinal()] = 0;
            else if (unitCounts[RobotType.DESIGN_SCHOOL.ordinal()] == 0)
                soupPriorities[RobotType.DESIGN_SCHOOL.ordinal()] = 0;
            else {
                int[] comp = MagicConstants.LATTICE_ARMY_COMP;
                if(bot.round + MagicConstants.TURNS_FOR_VAP_TO_PAY >= MagicConstants.CRUNCH_ROUND) {
                    for (int i = 0; i < MagicConstants.LATTICE_ARMY_COMP.length; i++) {
                        if (MagicConstants.LATTICE_COMP_TYPES[i] == RobotType.VAPORATOR) {
                            comp[i] = 0;
                            break;
                        }
                    }
                }
                updateBasedOnDesiredComp(unitCounts,
                        comp,
                        MagicConstants.LATTICE_COMP_TYPES);
            }
        }
    }
}
