package v3_lattice;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class DesignSchool extends Building {
	
	public static int landscapers;

    public DesignSchool(RobotController r) throws GameActionException {
        super(r);
        comms.broadcastLoc(Comms.MessageType.DESIGN_SCHOOL_LOC, rc.getLocation());
        landscapers = 0;
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(landscapers == 8)
        	return;
        for (Direction dir : directions)
            if(tryBuild(RobotType.LANDSCAPER, dir)) {
                System.out.println("made a landscaper");
                landscapers++;
            }
        comms.readMessages();
    }

}
