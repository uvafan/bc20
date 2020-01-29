package v14_final_bot;

import battlecode.common.*;

public class HQ extends Building {

    int seesDroneEnemyRound;
    boolean seesNonDroneEnemy = false;
    boolean seesOpponentHQ = false;
    int lastAttackedRound = -100;
    int distToOpponent;

    public HQ(RobotController r) throws GameActionException {
        super(r);
        seesDroneEnemyRound = -100;
        comms.broadcastLoc(Comms.MessageType.HQ_LOC, here);
    }

    private boolean isUnderAttack() {
        boolean isLate = isWallComplete || round > MagicConstants.NOT_ATTACKED_ROUND;
        return !seesOpponentHQ && seesNonDroneEnemy &&
                (rc.getDirtCarrying() < 0 ||
                distToOpponent <= MagicConstants.MAX_EARLY_RUSH_DIST && !isLate ||
                distToOpponent <= MagicConstants.MAX_LATE_RUSH_DIST);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        shootDrones();
        seesNonDroneEnemy = false;
        seesOpponentHQ = false;
        distToOpponent = Integer.MAX_VALUE;
        for(RobotInfo e: enemies) {
            if(e.type != RobotType.DELIVERY_DRONE && !Utils.isBuilding(e.type)) {
                seesNonDroneEnemy = true;
                distToOpponent = Math.min(distToOpponent, here.distanceSquaredTo(e.location));
            }
            else if(e.type == RobotType.DELIVERY_DRONE) {
                if(seesDroneEnemyRound == -100) {
                    seesDroneEnemyRound = round;
                }
            }
            else if(e.type == RobotType.HQ) {
                seesOpponentHQ = true;
            }
        }
        boolean underAttack = isUnderAttack();
        if(hqAttacked && !underAttack && round - lastAttackedRound > MagicConstants.MIN_ATTACK_DURATION) {
           hqAttacked = false;
           comms.broadcastLoc(Comms.MessageType.HQ_OK, here);
        }
        else if(!hqAttacked && underAttack) {
            hqAttacked = true;
            lastAttackedRound = round;
            comms.broadcastLoc(Comms.MessageType.HQ_ATTACKED, here);
        }
        if(!isWallComplete) {
            checkForWallCompletion();
        }
        comms.readMessages();
        broadcastNetGuns();
    }

    private void checkForWallCompletion() throws GameActionException {
        for(int i = 0; i < MagicConstants.WALL_X_OFFSETS.length; i++) {
            int dx = MagicConstants.WALL_X_OFFSETS[i];
            int dy = MagicConstants.WALL_Y_OFFSETS[i];
            MapLocation check = new MapLocation(here.x + dx, here.y + dy);
            if(rc.canSenseLocation(check)) {
                RobotInfo ri = rc.senseRobotAtLocation(check);
                if(ri != null && Utils.isBuilding(ri.type) && ri.team == us) {
                    continue;
                }
                if (rc.senseElevation(check) < MagicConstants.LATTICE_HEIGHT)
                    return;
            }
        }
        isWallComplete = true;
        wallCompletionRound = round;
        comms.broadcastLoc(Comms.MessageType.WALL_COMPLETE, here);
    }

    public Direction getBuildDirection() {
        if(strat instanceof Rush && round == 1)
            return here.directionTo(center);
        MapLocation[] locs = rc.senseNearbySoup();
        if(locs.length > 0)
            return here.directionTo(locs[0]);
        return randomDirection();
    }

}
