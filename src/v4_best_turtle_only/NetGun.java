package v4_best_turtle_only;

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
