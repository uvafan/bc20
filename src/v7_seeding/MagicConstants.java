package v7_seeding;

import battlecode.common.RobotType;

public class MagicConstants {
	public static int FAST_SECRET_NUM = 194;
	public static int SLOW_SECRET_NUM = 155252936;
	public static int LOCATION_SECRET_NUM = 12345;
	public static int ORDINAL_SECRET_NUM = 235203;
	public static int BUG_PATIENCE = 20; //how many turns before we give up bugging
    public static int MAX_CLUSTER_DIST = RobotType.MINER.sensorRadiusSquared;
    public static int GIVE_UP_CLUSTER_DIST = 2;
    public static int EXPLORE_BOREDOM = 10;
}
