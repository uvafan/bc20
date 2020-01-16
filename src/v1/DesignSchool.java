package v1;

import battlecode.common.*;

public class DesignSchool extends Bot {

    public DesignSchool(RobotController r) throws GameActionException {
        super(r);
        comms.broadcastLoc(Comms.MessageType.DESIGN_SCHOOL_LOC, rc.getLocation());
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        for (Direction dir : directions)
            if(tryBuild(RobotType.LANDSCAPER, dir))
                System.out.println("made a landscaper");
        comms.readMessages();
    }

}
