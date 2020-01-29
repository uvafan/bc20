package v14_final_bot;

import battlecode.common.*;

public class Building extends Bot {

    RobotType prodType;
    boolean deathBroadcasted;

    public Building(RobotController r) throws GameActionException {
        super(r);
        deathBroadcasted = false;
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
        if(round < MagicConstants.CRUNCH_ROUND && type != RobotType.HQ && !deathBroadcasted && (type.dirtLimit < rc.getDirtCarrying() + 3 || floodingNextRound(here))) {
           deathBroadcasted = true;
           comms.broadcastUnitDeath(type);
        }
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
