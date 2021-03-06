package v8_eco_lattice;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Strategy {
    static Bot bot;
    static RobotController rc;
    static int[] buildingIndices = {
            RobotType.DESIGN_SCHOOL.ordinal(),
            RobotType.REFINERY.ordinal(),
            RobotType.NET_GUN.ordinal(),
            RobotType.FULFILLMENT_CENTER.ordinal(),
            RobotType.VAPORATOR.ordinal()
    };
    static int[] soupPriorities;

    public Strategy(Bot b) {
        bot = b;
        rc = b.rc;
        // represents the min soup we would need to build. lower is higher priority
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
    }

    public RobotType determineBuildingNeeded() throws GameActionException {
        updatePriorities(bot.unitCounts);
        RobotType ret = null;
        int minPri = rc.getTeamSoup() + 1;
        for(int i=0; i<buildingIndices.length; i++){
            int idx = buildingIndices[i];
            //Utils.log("priority for " + RobotType.values()[idx] + " is " + soupPriorities[idx]);
            if(soupPriorities[idx] < minPri){
                minPri = soupPriorities[idx];
                ret = RobotType.values()[idx];
            }
        }
        return ret;
    }

    public boolean shouldBuildUnit(RobotType rt) throws GameActionException {
        updatePriorities(bot.unitCounts);
        //Utils.log("priority for " + rt.ordinal()  + " is " + soupPriorities[rt.ordinal()]);
        //Utils.log(bot.unitCounts[rt.ordinal()] + " units have been created.");
        return soupPriorities[rt.ordinal()] <= rc.getTeamSoup();
    }

    public void updatePriorities(int[] unitCounts) throws GameActionException {
        return;
    }


    public void updateBasedOnDesiredComp(int[] unitCounts, int[] desiredComp, RobotType[] types) {
        RobotType bestType = null;
        double bestRatio = Double.MAX_VALUE;
        int highestNum = -1;
        for(int i=0; i < desiredComp.length; i++) {
            RobotType type = types[i];
            int count = unitCounts[type.ordinal()];
            int comp = desiredComp[i];
            if(comp==0)
            	continue;
            double ratio = (1.0 * count) / comp;
            Utils.log("I think we have " + count + " " + type + " ratio is " + ratio);
            if(ratio < bestRatio || (ratio == bestRatio && comp > highestNum)) {
                bestRatio = ratio;
                bestType = type;
                highestNum = comp;
            }
        }
        Utils.log("best type is "+ bestType);
        for(int i=0; i < desiredComp.length; i++) {
        	RobotType type = types[i];
            if(type == bestType)
                soupPriorities[type.ordinal()] = 0;
            else if(desiredComp[i] == 0)
            	soupPriorities[type.ordinal()] = Integer.MAX_VALUE;
            else
                soupPriorities[type.ordinal()] = bestType.cost + type.cost + 2;
        }
    }

}
