package v5_rush_op;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class FulfillmentCenter extends Building {

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
