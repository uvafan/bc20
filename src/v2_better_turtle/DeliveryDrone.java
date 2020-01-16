package v2_better_turtle;

import battlecode.common.*;

public class DeliveryDrone extends Unit {

    public DeliveryDrone(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
        comms.readMessages();
    }

}
