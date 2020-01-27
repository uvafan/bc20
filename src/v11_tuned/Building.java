package v11_tuned;

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
        buildIfShould();
    }

    public void buildIfShould() throws GameActionException {
        if(prodType != null && strat.shouldBuildUnit(prodType)) {
            tryBuild(prodType, getBuildDirection(), true);
        }
    }

    public Direction getBuildDirection() {
        return randomDirection();
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
