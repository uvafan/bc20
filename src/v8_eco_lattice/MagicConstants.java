package v8_eco_lattice;

import battlecode.common.RobotType;

public class MagicConstants {
	public static int FAST_SECRET_NUM = 476210;
	public static int SLOW_SECRET_NUM = 763695;
	public static int LOCATION_SECRET_NUM = 227306;
	public static int ORDINAL_SECRET_NUM = 234746;
	public static int BUG_PATIENCE = 20; //how many turns before we give up bugging
    public static int MAX_CLUSTER_DIST = RobotType.MINER.sensorRadiusSquared;
    public static int GIVE_UP_CLUSTER_DIST = 2;
    public static int EXPLORE_BOREDOM = 10;
    public static int REQUIRED_REFINERY_DIST = 50;
}
