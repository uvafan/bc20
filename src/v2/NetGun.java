package v2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class NetGun extends Bot {

    public NetGun(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

}
