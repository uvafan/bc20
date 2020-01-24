package v8_eco_lattice;

import battlecode.common.RobotType;

public class MagicConstants {
	public static int FAST_SECRET_NUM = 476210;
	public static int SLOW_SECRET_NUM = 763695;
	public static int LOCATION_SECRET_NUM = 227306;
	public static int ORDINAL_SECRET_NUM = 234746;
	public static int MAX_CLUSTER_DIST = RobotType.MINER.sensorRadiusSquared;
    public static int GIVE_UP_CLUSTER_DIST = 2;
	public static int GIVE_UP_WATER_DIST = 2;
	public static int EXPLORE_BOREDOM = 25;
    public static int REQUIRED_REFINERY_DIST = 50;
	public static int TOLERATED_WATER_DIST = 50;
	public static int RUSH_DEFENSE_DIST = 100;
	public static int LATTICE_HEIGHT = 8;
	public static int CRUNCH_ROUND = 1500;
	public static int PICK_UP_LANDSCAPER_ROUND = 1250;
	public static int MAX_WATER_LOCS = 20;
	public static RobotType[] LATTICE_COMP_TYPES = new RobotType[]{
			RobotType.NET_GUN,
			RobotType.FULFILLMENT_CENTER,
			RobotType.DESIGN_SCHOOL,
			RobotType.DELIVERY_DRONE,
			RobotType.VAPORATOR,
			RobotType.LANDSCAPER,
	};
	public static int[] LATTICE_ARMY_COMP = new int[]{
			0, //Net Guns
			1, //FCs
			1, //DS
			10, //Drones
			10, //Vaps
			10, //Landscapers
	};
	public static RobotType[] RUSH_DEFENSE_COMP_TYPES = new RobotType[]{
			RobotType.DELIVERY_DRONE,
			RobotType.LANDSCAPER,
	};
	public static int[] RUSH_DEFENSE_ARMY_COMP = new int[]{
			1, //Drones
			1, //Landscapers
	};
	//public static int[] WALL_Y_OFFSETS = {-2,-1,0,1,2,2,2,2,2,1,0,-1,-2,-2,-2,-2};
	// public static int[] WALL_X_OFFSETS = {-2,-2,-2,-2,-2,-1,0,1,2,2,2,2,2,1,0,-1};
	public static int[] WALL_X_OFFSETS = {-3,-2,-1,0,1,2,3,3,3,3,3,3,3,2,1,0,-1,-2,-3,-3,-3,-3,-3,-3};
	public static int[] WALL_Y_OFFSETS = {3,3,3,3,3,3,3,2,1,0,-1,-2,-3,-3,-3,-3,-3,-3,-3,-2,-1,0,1,2};
	public static int BUILD_MINER_NUM = 6;
}
