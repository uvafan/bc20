package v4_best_turtle_only;

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

    public RobotType determineBuildingNeeded() {
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

    public boolean shouldBuildUnit(RobotType rt) {
        updatePriorities(bot.unitCounts);
        //Utils.log("priority for " + rt.ordinal()  + " is " + soupPriorities[rt.ordinal()]);
        //Utils.log(bot.unitCounts[rt.ordinal()] + " units have been created.");
        return soupPriorities[rt.ordinal()] <= rc.getTeamSoup();
    }

    public void updatePriorities(int[] unitCounts) {
        return;
    }

}
