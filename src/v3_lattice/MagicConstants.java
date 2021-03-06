package v3_lattice;

import battlecode.common.RobotType;

public class MagicConstants {
	public static int FAST_SECRET_NUM = 194;
	public static int SLOW_SECRET_NUM = 155252936;
	public static int BUG_PATIENCE = 20; //how many turns before we give up bugging
	public static int LATTICE_BUFFER = 100; //how many turns before flooding to patch up that lattice
    public static int MAX_CLUSTER_DIST = RobotType.MINER.sensorRadiusSquared;
    public static int GIVE_UP_CLUSTER_DIST = 2;
}
