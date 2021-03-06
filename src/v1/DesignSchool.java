package v1;

import battlecode.common.*;

public class DesignSchool extends Bot {
	
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
