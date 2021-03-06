package v2_better_turtle;

import battlecode.common.*;

public class Building extends Bot {

    RobotType prodType;

    public Building(RobotController r) throws GameActionException {
        super(r);
        prodType = null;
        switch(type){
            case HQ:
                prodType = RobotType.MINER;
                break;
            case FULFILLMENT_CENTER:
                prodType = RobotType.DELIVERY_DRONE;
                break;
            case DESIGN_SCHOOL:
                prodType = RobotType.LANDSCAPER;
                break;
        }
        here = rc.getLocation();
    }

    @Override
    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(prodType != null && strat.shouldBuildUnit(prodType)) {
            tryBuild(prodType, randomDirection());
        }
    }

    public void shootDrones() throws GameActionException {
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, enemy);

        for (RobotInfo e : enemiesInRange) {
            if (e.type == RobotType.DELIVERY_DRONE) {
                if (rc.canShootUnit(e.ID)){
                    rc.shootUnit(e.ID);
                    break;
                }
            }
        }
    }

}
