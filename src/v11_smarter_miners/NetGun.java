package v11_smarter_miners;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class NetGun extends Building {

    public NetGun(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        shootDrones();
        comms.readMessages();
    }

}
