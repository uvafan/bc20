package v11_smarter_miners_tuned;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Vaporator extends Building {

    public Vaporator(RobotController r) throws GameActionException {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        comms.readMessages();
    }

}
