package v2_better_turtle;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        // System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
        // findHQ();
        Bot bot;
        switch (rc.getType()) {
            case HQ:                 bot = new HQ(rc);                break;
            case MINER:              bot = new Miner(rc);             break;
            case REFINERY:           bot = new Refinery(rc);          break;
            case VAPORATOR:          bot = new Vaporator(rc);         break;
            case DESIGN_SCHOOL:      bot = new DesignSchool(rc);      break;
            case FULFILLMENT_CENTER: bot = new FulfillmentCenter(rc); break;
            case LANDSCAPER:         bot = new Landscaper(rc);        break;
            case DELIVERY_DRONE:     bot = new DeliveryDrone(rc);     break;
            case NET_GUN:            bot = new NetGun(rc);            break;
            default: bot = new Bot();
        }
        while (true) {
            try {
                bot.takeTurn();
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
