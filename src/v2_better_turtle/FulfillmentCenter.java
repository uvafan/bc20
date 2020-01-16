package v2_better_turtle;

import battlecode.common.*;

public class FulfillmentCenter extends Bot {

    public FulfillmentCenter(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
        comms.readMessages();
    }

}
