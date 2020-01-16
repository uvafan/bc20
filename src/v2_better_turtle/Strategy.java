package v2_better_turtle;

import battlecode.common.*;

public class Strategy {
    static Bot bot;
    static RobotController rc;
    static int[] buildingIndices = {2,3,7,8,9};
    static int[] soupPriorities;

    public Strategy(Bot b) {
        bot = b;
        rc = b.rc;
        // Cow, Drone, School, Fulfillment Center, HQ, Landscaper,
        // Miner, Net Gun, Refinery, Vaporator
        soupPriorities = new int[]{
                Integer.MAX_VALUE, // Cow
                Integer.MAX_VALUE, // Drone
                Integer.MAX_VALUE, // Design School
                Integer.MAX_VALUE, // Fulfillment Center
                Integer.MAX_VALUE, // HQ
                Integer.MAX_VALUE, // Landscaper
                Integer.MAX_VALUE, // Miner
                Integer.MAX_VALUE, // Net Gun
                Integer.MAX_VALUE, // Refinery
                Integer.MAX_VALUE, // Vaporator
        };
    }

    public RobotType determineBuildingNeeded() {
        updatePriorities();
        RobotType ret = null;
        int minPri = rc.getTeamSoup() + 1;
        for(int i=0; i<buildingIndices.length; i++){
            int idx = buildingIndices[i];
            if(soupPriorities[idx] < minPri){
                minPri = soupPriorities[idx];
                ret = RobotType.values()[idx];
            }
        }
        return ret;
    }

    public boolean shouldBuildUnit(RobotType rt) {
        updatePriorities();
        return soupPriorities[rt.ordinal()] > rc.getTeamSoup();
    }

    private void updatePriorities() {
        return;
    }

}
