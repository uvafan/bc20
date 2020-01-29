package v14_final_bot;

import battlecode.common.*;

public class DeliveryDrone extends Unit {

    public static MapLocation targetLoc;
    public static boolean defending = false;
    public static boolean harassing = false;
    public static boolean landscaperDropper = false;
    public static boolean droppedOff = false;
    public static boolean pickedUpFriend = false;
    public Objective obj;
    public Objective prevObj;
    public State state;
    MapLocation dropOffLoc;
    RobotInfo closestFriend;

    public static enum Objective {
        HELP_FRIEND_UP,
        HARASS_ENEMY_HQ,
        HARASS_SOUP_LOCATIONS,
        HARASS_RANDOMLY,
        DEFEND_HQ,
        DEFEND_EDGE_OF_LATTICE,
        PICK_UP_LANDSCAPER,
        PICK_UP_MINER,
        CRUNCH_ENEMY_HQ_DROWN_ENEMY,
        CRUNCH_ENEMY_HQ_DROP_MINER,
        CRUNCH_ENEMY_HQ_DROP_LANDSCAPER,
        CRUNCH_ENEMY_NET_GUN_DROP_LANDSCAPER,
        RUSH,
    }

    public static enum State {
        HOLDING_FRIENDLY_LANDSCAPER,
        HOLDING_FRIENDLY_MINER,
        HOLDING_ENEMY,
        NOT_HOLDING_ANYTHING
    }

    public DeliveryDrone(RobotController r) throws GameActionException {
        super(r);
        state = State.NOT_HOLDING_ANYTHING;
        if(strat instanceof Rush) {
            obj = Objective.RUSH;
        }
        else {
            if (round >= MagicConstants.PICK_UP_LANDSCAPER_ROUND && round < MagicConstants.DONT_PICK_UP) {
                if(round % MagicConstants.LANDSCAPER_MINER_RATIO > 0)
                    obj = Objective.PICK_UP_LANDSCAPER;
                else
                    obj = Objective.PICK_UP_MINER;
            }
            else if(round > MagicConstants.DONT_PICK_UP) {
                obj = Objective.DEFEND_HQ;
            }
            else {
                obj = Objective.HARASS_ENEMY_HQ;
            }
        }
        prevObj = obj;
        if (obj == Objective.RUSH) {
            if (hqLoc == null || hqLoc.distanceSquaredTo(center) > here.distanceSquaredTo(hqLoc))
                targetLoc = center;
            else
                targetLoc = pickTargetFromEnemyHQs(true);
        }
        else {
            updateTargetLoc();
        }
        /*
        if(enemyHQLoc == null) {
            if (obj == Objective.RUSH && (hqLoc == null || hqLoc.distanceSquaredTo(center) > here.distanceSquaredTo(hqLoc)))
                targetLoc = center;
            else if(obj = obj.HARASS_ENEMY_HQ)
                targetLoc = center;
            else
                targetLoc = pickTargetFromEnemyHQs(true);
        }*/
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        if(turnCount == 9 && comms.isCaughtUp() && prevObj == Objective.HARASS_ENEMY_HQ) {
            if(enemyHQLoc != null && round < MagicConstants.STOP_HARASSING_SOUP_ROUND) {
                if (unitCounts[RobotType.DELIVERY_DRONE.ordinal()] % MagicConstants.HQ_SOUP_HARASS_RATIO == 0) {
                    prevObj = Objective.HARASS_SOUP_LOCATIONS;
                    if(obj == Objective.HARASS_ENEMY_HQ)
                        obj = Objective.HARASS_SOUP_LOCATIONS;
                }
            }
        }
        if(round > 1640 && here.distanceSquaredTo(hqLoc) <= 8 && state != State.HOLDING_ENEMY)
            rc.disintegrate();
        if(rc.getCooldownTurns() < 1) {
            updateObjective();
            // System.out.println("objective: " + obj + " state: " + state);
            switch (obj) {
                case RUSH:
                    doRush();
                    break;
                case DEFEND_HQ:
                    doDefense();
                    break;
                case CRUNCH_ENEMY_HQ_DROP_LANDSCAPER:
                case CRUNCH_ENEMY_HQ_DROP_MINER:
                case CRUNCH_ENEMY_HQ_DROWN_ENEMY:
                    doCrunch();
                    break;
                case HARASS_ENEMY_HQ:
                case HARASS_SOUP_LOCATIONS:
                case HARASS_RANDOMLY:
                    doHarass();
                    break;
                case PICK_UP_MINER:
                case PICK_UP_LANDSCAPER:
                    pickUpAttackDrop();
                    break;
                case HELP_FRIEND_UP:
                    helpOutFriends();
            }
        }
        broadcastNetGuns();
        comms.readMessages();
        if(Clock.getBytecodesLeft() > 1000)
            updateTargetLoc();
        if(numWaterLocs <= MagicConstants.MAX_WATER_LOCS && comms.isCaughtUp())
            broadcastWater();
        /*
        if (rushing) {
            doRush();
        }
        else if (rc.getCooldownTurns() < 1) {
            if(!crunching && shouldCrunch())
                crunching = true;
            if(crunching) {
                Utils.log("crunching");
                doCrunch();
            }
            else if ((!defending && !rc.isCurrentlyHoldingUnit() && !landscaperDropper) || pickedUpFriend) {
                Utils.log("helping out friends");
                helpOutFriends();
            }
            if(rc.getCooldownTurns() < 1 && (harassing || defending)) {
                Utils.log("harassing");
                doHarass();
            }
        }*/
    }

    private void setObjective(Objective objective) throws GameActionException {
        if(obj != Objective.HELP_FRIEND_UP && obj != Objective.DEFEND_HQ)
            prevObj = obj;
        obj = objective;
        crunching = isCrunching();
        updateTargetLoc();
    }

    private boolean shouldHelpOutFriends() {
        if(state != State.NOT_HOLDING_ANYTHING && obj != Objective.HELP_FRIEND_UP)
            return false;
        switch(obj) {
            case DEFEND_HQ:
                return enemies.length == 0;
            case DEFEND_EDGE_OF_LATTICE:
            case CRUNCH_ENEMY_HQ_DROP_LANDSCAPER:
            case CRUNCH_ENEMY_HQ_DROP_MINER:
            case CRUNCH_ENEMY_HQ_DROWN_ENEMY:
            case CRUNCH_ENEMY_NET_GUN_DROP_LANDSCAPER:
            case PICK_UP_LANDSCAPER:
            case PICK_UP_MINER:
                return false;
            default:
                return true;
        }
    }

    private boolean isDefending() {
        switch (obj) {
            case DEFEND_EDGE_OF_LATTICE:
            case DEFEND_HQ:
                return true;
            default:
                return false;
        }
    }

    private boolean isCrunching() {
       switch (obj) {
           case CRUNCH_ENEMY_HQ_DROP_LANDSCAPER:
           case CRUNCH_ENEMY_HQ_DROP_MINER:
           case CRUNCH_ENEMY_HQ_DROWN_ENEMY:
           case CRUNCH_ENEMY_NET_GUN_DROP_LANDSCAPER:
               return true;
           default:
               return false;
       }
    }

    private boolean shouldDefend() {
        return enemyHQLoc == null || round < MagicConstants.RUN_BACK_TO_DEFEND_MAX_ROUND || (round < MagicConstants.PICK_UP_LANDSCAPER_ROUND && here.distanceSquaredTo(enemyHQLoc) > here.distanceSquaredTo(hqLoc));
    }

    private boolean shouldGiveUpOnPickingUp() {
        return round > MagicConstants.DONT_PICK_UP;
    }

    private void updateObjective() throws GameActionException {
        if(hqAttacked && obj != Objective.DEFEND_HQ && shouldDefend())
            setObjective(Objective.DEFEND_HQ);
        else if(obj == Objective.DEFEND_HQ && (!hqAttacked || round > MagicConstants.PICK_UP_LANDSCAPER_ROUND) && round < MagicConstants.DONT_PICK_UP)
            setObjective(prevObj);
        if(obj == Objective.HARASS_SOUP_LOCATIONS && round > MagicConstants.STOP_HARASSING_SOUP_ROUND) {
            setObjective(Objective.HARASS_ENEMY_HQ);
        }
        else if(!isCrunching() && shouldCrunch()) {
            setCrunchObjective();
        }
        else if(obj == Objective.PICK_UP_MINER ||
                obj == Objective.PICK_UP_LANDSCAPER) {
            // System.out.println("here");
            if(state == State.NOT_HOLDING_ANYTHING && shouldGiveUpOnPickingUp()) {
                if(enemyHQLoc != null && here.distanceSquaredTo(enemyHQLoc) <= MagicConstants.BECOME_NON_DROPPER_DIST) {
                    setObjective(Objective.CRUNCH_ENEMY_HQ_DROWN_ENEMY);
                }
                else {
                    // System.out.println("here2");
                    setObjective(Objective.DEFEND_HQ);
                }
            }
            else if (state != State.NOT_HOLDING_ANYTHING){
                setObjective(Objective.HARASS_ENEMY_HQ);
            }
        }
        else if(shouldHelpOutFriends()) {
            getDropOffLoc();
            if(state == State.NOT_HOLDING_ANYTHING)
                getUnitToHelp();
            boolean canHelp = dropOffLoc != null && (state != State.NOT_HOLDING_ANYTHING || closestFriend != null);
            if (canHelp && obj != Objective.HELP_FRIEND_UP) {
                setObjective(Objective.HELP_FRIEND_UP);
            }
            else if (obj == Objective.HELP_FRIEND_UP && !canHelp) {
                if(hqAttacked)
                    setObjective(Objective.DEFEND_HQ);
                else
                    setObjective(prevObj);
            }
        }
    }

    private void setCrunchObjective() throws GameActionException {
        switch(state) {
            case NOT_HOLDING_ANYTHING:
                setObjective(Objective.CRUNCH_ENEMY_HQ_DROWN_ENEMY); break;
            case HOLDING_FRIENDLY_LANDSCAPER:
                setObjective(Objective.CRUNCH_ENEMY_HQ_DROP_LANDSCAPER); break;
            case HOLDING_FRIENDLY_MINER:
                setObjective(Objective.CRUNCH_ENEMY_HQ_DROP_MINER);
        }
    }

    public void getDropOffLoc() throws GameActionException {
        int minDist = Integer.MAX_VALUE;
        dropOffLoc = null;
        for(int i = 0; i < MagicConstants.WALL_X_OFFSETS.length; i++) {
            if(Clock.getBytecodesLeft() < 1000)
                break;
            int dx = MagicConstants.WALL_X_OFFSETS[i];
            int dy = MagicConstants.WALL_Y_OFFSETS[i];
            MapLocation check = new MapLocation(hqLoc.x + dx, hqLoc.y + dy);
            if(rc.canSenseLocation(check) && !rc.isLocationOccupied(check) && safeFromDrones(check)
                && rc.senseElevation(check) >= MagicConstants.LATTICE_HEIGHT && (rc.senseElevation(check) <= MagicConstants.LATTICE_TOLERANCE || round > MagicConstants.CRUNCH_ROUND)) {
                int dist = here.distanceSquaredTo(check);
                if(dist < minDist && dist > 0) {
                    minDist = dist;
                    dropOffLoc = check;
                }
            }
        }
    }

    private void getUnitToHelp() {
        closestFriend = null;
        int minDist = Integer.MAX_VALUE;
        for (RobotInfo ri : friends) {
            if (((ri.type == RobotType.MINER && round < MagicConstants.CRUNCH_ROUND) && (isWallComplete || round > MagicConstants.HELP_MINER_UP_ROUND)) || ri.type == RobotType.LANDSCAPER) {
                if (ri.location.distanceSquaredTo(hqLoc) < 9 && here.distanceSquaredTo(ri.location) < minDist) {
                    minDist = here.distanceSquaredTo(ri.location);
                    closestFriend = ri;
                }
            }
        }
    }

    private void helpOutFriends() throws GameActionException {
        MapLocation wallLoc = dropOffLoc;
        if(state == State.HOLDING_FRIENDLY_LANDSCAPER ||
            state == State.HOLDING_FRIENDLY_MINER) {
            Utils.log("holding friend!");
            if(wallLoc != null) {
                int dist = here.distanceSquaredTo(wallLoc);
                rc.setIndicatorLine(here, wallLoc, 0,  0, 255);
                if(dist <= 2) {
                    tryDrop(here.directionTo(wallLoc), false);
                }
                else {
                    goTo(wallLoc);
                }
            }
            else {
                tryDrop(here.directionTo(hqLoc), true);
            }
        }
        else if (wallLoc != null && state == State.NOT_HOLDING_ANYTHING){
            if(closestFriend != null) {
                if(Utils.DEBUG)
                    rc.setIndicatorLine(here, closestFriend.location, 0, 255, 0);
                if(here.distanceSquaredTo(closestFriend.location) <= 2) {
                    if(rc.canPickUpUnit(closestFriend.ID)) {
                        switch(closestFriend.type) {
                            case MINER:
                                state = State.HOLDING_FRIENDLY_MINER;
                                break;
                            case LANDSCAPER:
                                state = State.HOLDING_FRIENDLY_LANDSCAPER;
                                break;
                        }
                        rc.pickUpUnit(closestFriend.ID);
                    }
                }
                else {
                    goTo(closestFriend.location);
                }
            }
        }
        else if(state == State.HOLDING_ENEMY)
            dropUnitInWater();
    }

    private void doRush() throws GameActionException {
        if(!rc.isCurrentlyHoldingUnit()) {
            int minerID = -1;
            RobotInfo[] friends = rc.senseNearbyRobots(2,us);
            for(RobotInfo f: friends) {
                if(f.type == RobotType.MINER) {
                    minerID = f.getID();
                    break;
                }
            }
            if(minerID != -1 && rc.canPickUpUnit(minerID)) {
                rc.pickUpUnit(minerID);
            }
            else if (minerID == -1){
                rushing = false;
            }
        }
        else {
            runToEnemyHQ();
        }
    }

    private void doDefense() throws GameActionException {
        if(obj == Objective.DEFEND_HQ)
            targetLoc = hqLoc;
        if(state == State.HOLDING_ENEMY) {
            dropUnitInWater();
        }
        else if (state == State.NOT_HOLDING_ANYTHING){
            findAndPickUpEnemyUnit();
        }
        else {
            dropFriendlyUnit();
        }
    }

    public void dropFriendlyUnit() throws GameActionException {
        for(Direction dir: directions) {
            MapLocation loc = here.add(dir);
            if(!rc.senseFlooding(loc) && rc.canDropUnit(dir)) {
                rc.dropUnit(dir);
                state = State.NOT_HOLDING_ANYTHING;
            }
        }
        if(state != State.NOT_HOLDING_ANYTHING) {
            goTo(targetLoc);
        }
    }

    private void updateTargetLoc() throws GameActionException {
        switch(obj) {
            case HARASS_RANDOMLY:
                targetLoc = null;
                break;
            case HARASS_SOUP_LOCATIONS:
                targetLoc = getTargetSoupHarass();
                break;
            case DEFEND_HQ:
                targetLoc = hqLoc;
                break;
            case HELP_FRIEND_UP:
                break;
            default:
                if(enemyHQLoc == null || enemyHqLocPossibilities.length > 1)
                    if(targetLoc == null || updateSymmetryAndOpponentHQs() || here.distanceSquaredTo(targetLoc) <= MagicConstants.SYM_DIST_TO_CENTER) {
                        targetLoc = pickTargetFromEnemyHQs(true);
                    }
                if(enemyHQLoc != null)
                    targetLoc = enemyHQLoc;
                break;
        }
        if(targetLoc != null && obj != Objective.HELP_FRIEND_UP)
            rc.setIndicatorLine(here, targetLoc, 255, 0, 0);
    }

    private MapLocation getTargetSoupHarass() {
        if(enemyHQLoc == null || numSoupClusters == 0)
            return null;
        if(targetLoc == null || targetLoc == enemyHQLoc || (targetLoc != null && here.isWithinDistanceSquared(targetLoc, 2))) {
            int randomPick = rand.nextInt(numSoupClusters);
            MapLocation cand = reflect(getSymmetry(hqLoc, enemyHQLoc), soupClusters[randomPick]);
            if(cand.distanceSquaredTo(enemyHQLoc) > 50)
                targetLoc = cand;
        }
        return targetLoc;
    }

    private void pickUpAttackDrop() throws GameActionException {
        if(state == State.NOT_HOLDING_ANYTHING) {
            RobotType desiredType = obj == Objective.PICK_UP_LANDSCAPER ? RobotType.LANDSCAPER : RobotType.MINER;
            RobotInfo closestFriend = null;
            int minDist = Integer.MAX_VALUE;
            for(RobotInfo f: friends) {
                if(f.type != desiredType)
                    continue;
                int dist = here.distanceSquaredTo(f.location);
                if(dist < minDist) {
                    closestFriend = f;
                    minDist = dist;
                }
            }
            if(minDist <= 2) {
                if(rc.canPickUpUnit(closestFriend.ID)) {
                    rc.pickUpUnit(closestFriend.ID);
                    if(desiredType == RobotType.LANDSCAPER)
                        state = State.HOLDING_FRIENDLY_LANDSCAPER;
                    else
                        state = State.HOLDING_FRIENDLY_MINER;
                }
            }
            else if (closestFriend != null) {
                goTo(closestFriend.location);
            }
            else {
                explore();
            }
        }
    }

    private void doHarass() throws GameActionException {
        // updateTargetLoc();
        if (state == State.NOT_HOLDING_ANYTHING) {
            findAndPickUpEnemyUnit();
        }
        else if (state == State.HOLDING_ENEMY){
            dropUnitInWater();
        }
        else if (prevObj != Objective.PICK_UP_LANDSCAPER &&
                prevObj != Objective.PICK_UP_MINER) {
            dropFriendlyUnit();
        }
        else {
            if(targetLoc == null)
                explore();
            else
                goTo(targetLoc);
        }
    }

    private void dropUnitInWater() throws GameActionException {
        Utils.log("trying to drop unit in water.");
        for(Direction dir: directions) {
            MapLocation loc = here.add(dir);
            if(rc.canSenseLocation(loc) && rc.senseFlooding(loc)) {
                if(tryDrop(dir, false)) {
                    state = State.NOT_HOLDING_ANYTHING;
                }
            }
        }
        MapLocation waterLoc = null;
        int minDist = Integer.MAX_VALUE;
        for(int i=0; i<numWaterLocs; i++) {
            if(invalidWater[i])
                continue;
            int dist = waterLocs[i].distanceSquaredTo(here);
            if(dist <= MagicConstants.GIVE_UP_WATER_DIST) {
                invalidWater[i] = true;
            }
            else if(dist < minDist){
                minDist = dist;
                waterLoc = waterLocs[i];
            }
        }
        if(waterLoc != null) {
            if(Utils.DEBUG)
                rc.setIndicatorLine(here, waterLoc, 0, 0, 255);
            goTo(waterLoc);
        }
        else {
            explore();
        }
    }

    private void findAndPickUpEnemyUnit() throws GameActionException {
        MapLocation closestEnemyLoc = null;
        int closestEnemyID = -1;
        int minDist = Integer.MAX_VALUE;
        for(RobotInfo e: enemies) {
            if(!Utils.isBuilding(e.type) && e.type != RobotType.DELIVERY_DRONE && here.distanceSquaredTo(e.location) < minDist) {
                minDist = here.distanceSquaredTo(e.location);
                closestEnemyLoc = e.location;
                closestEnemyID = e.getID();
            }
        }
        if(closestEnemyLoc == null) {
            Utils.log("dont see an enemy to pick up");
            if (obj == Objective.CRUNCH_ENEMY_HQ_DROWN_ENEMY && here.distanceSquaredTo(enemyHQLoc) <= 16) {
                Utils.log("gonna go defend");
                setObjective(Objective.DEFEND_HQ);
            }
            else if (targetLoc != null) {
                goTo(targetLoc);
            }
            else {
                explore();
            }
        }
        else {
            if(minDist <= 2 && rc.canPickUpUnit(closestEnemyID)) {
                rc.pickUpUnit(closestEnemyID);
                state = State.HOLDING_ENEMY;
            }
            else {
                goTo(closestEnemyLoc);
            }
        }
    }

    public boolean shouldCrunch() {
        return round >= MagicConstants.CRUNCH_ROUND && !isDefending() && obj != Objective.HELP_FRIEND_UP;
    }

    public boolean seesNearbyDrone() {
        for(RobotInfo e: enemies)
            if(e.type == RobotType.DELIVERY_DRONE)
                return true;
        return false;
    }

    public void doCrunch() throws GameActionException {
        crunching = true;
        if(state == State.NOT_HOLDING_ANYTHING || state == State.HOLDING_ENEMY) {
            doHarass();
        }
        else {
            boolean shouldTryToDrop = state == State.HOLDING_FRIENDLY_LANDSCAPER ||
                    state == State.HOLDING_FRIENDLY_MINER &&
                    (here.distanceSquaredTo(enemyHQLoc) > MagicConstants.MINER_GIVE_UP_DIST_THRESHOLD ||
                        seesNearbyDrone());
            if(shouldTryToDrop) {
                if (enemyHQLoc != null) {
                    int distThreshold = (state == State.HOLDING_FRIENDLY_LANDSCAPER ? 2 : MagicConstants.MINER_DIST_THRESHOLD);
                    int minDist = Integer.MAX_VALUE;
                    Direction bestDir = null;
                    for (Direction dir : directions) {
                        MapLocation loc = here.add(dir);
                        if (loc.distanceSquaredTo(enemyHQLoc) < minDist && rc.canDropUnit(dir)
                        && !rc.senseFlooding(loc)) {
                            minDist = loc.distanceSquaredTo(enemyHQLoc);
                            bestDir = dir;
                        }
                    }
                    if(minDist <= distThreshold) {
                        tryDrop(bestDir, false);
                    }
                }
                if(state != State.NOT_HOLDING_ANYTHING) {
                    //if (Utils.DEBUG)
                     //   rc.setIndicatorLine(here, enemyHQLoc, 255, 0, 0);
                    goTo(enemyHQLoc);
                }
            }
            else {
                setObjective(Objective.DEFEND_HQ);
            }
        }
    }

    public void runToEnemyHQ() throws GameActionException {
        if((enemyHQLoc == null || enemyHqLocPossibilities.length > 1) && rushing){
            if(updateSymmetryAndOpponentHQs())
                targetLoc = pickTargetFromEnemyHQs(true);
        }
        if(enemyHQLoc != null) {
            Utils.log("know enemy hq loc");
            if(Utils.DEBUG)
                rc.setIndicatorLine(here, enemyHQLoc, 255, 0, 0);
            targetLoc = enemyHQLoc;
            if(rushing && here.distanceSquaredTo(targetLoc) <= 25){
                Utils.log("trying to drop");
                if(tryDrop(here.directionTo(targetLoc), true))
                    rushing = false;
            }
            if(rc.getCooldownTurns() < 1)
                goTo(targetLoc);
        }
        else {
            if(targetLoc == null || here.equals(targetLoc)) {
                targetLoc = pickTargetFromEnemyHQs(false);
            }
            if(Utils.DEBUG)
                rc.setIndicatorLine(here, targetLoc, 255, 0, 0);
            goTo(targetLoc);
        }
    }

    public boolean shouldDropUnit(Direction dir) throws GameActionException {
        if (rushing) {
            MapLocation dropLoc = here.add(dir);
            if (rc.senseFlooding(dropLoc))
                return false;
            int elev = rc.senseElevation(dropLoc);
            for(Direction d: directions) {
                MapLocation loc = targetLoc.add(d);
                if(rc.canSenseLocation(loc) && Math.abs(rc.senseElevation(loc) - elev) <= 3) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public boolean tryDrop(Direction dir, boolean tryOthers) throws GameActionException {
        if (rc.canDropUnit(dir) && shouldDropUnit(dir)) {
            state = State.NOT_HOLDING_ANYTHING;
            rc.dropUnit(dir);
            return true;
        }
        if(tryOthers) {
            Direction dirL = dir.rotateLeft();
            Direction dirR = dir.rotateRight();
            while(dirL != dir) {
                if (rc.canDropUnit(dirL) && shouldDropUnit(dir)) {
                    state = State.NOT_HOLDING_ANYTHING;
                    rc.dropUnit(dirL);
                    return true;
                }
                if (rc.canDropUnit(dirR) && shouldDropUnit(dir)) {
                    state = State.NOT_HOLDING_ANYTHING;
                    rc.dropUnit(dirR);
                    return true;
                }
                dirL = dirL.rotateLeft();
                dirR = dirR.rotateRight();
            }
        }
        return false;
    }
}
