package v11_smarter_miners_tuned;

import battlecode.common.RobotType;

public class MagicConstants {
	// comms
	public static int FAST_SECRET_NUM = 476210;
	public static int SLOW_SECRET_NUM = 763695;
	public static int LOCATION_SECRET_NUM = 227306;
	public static int ORDINAL_SECRET_NUM = 234746;

	// misc
	public static int GIVE_UP_WATER_DIST = 2;
	public static int EXPLORE_BOREDOM = 25;
	public static int TOLERATED_WATER_DIST = 50;
	public static int RUSH_DEFENSE_DIST = 100;
	public static int MAX_WATER_LOCS = 20;

	// determining if tile is flooding soon
	public static int FLOODING_SOON_MAX = 100;
	public static int FLOODING_SOON_MIN = -20;
	public static int FLOODING_SOON_MIN_ROUND = 200;
	public static int FLOODING_SOON_MIN_HQ_DIST = 18;

	// net guns and dealing with drones
	public static int MIN_NET_GUN_CLOSENESS = 8;
	public static int MIN_NET_GUN_DIST_FROM_HQ = 8;
	public static final int MAX_DIST_TO_FLEE = 8;
	public static final int MAX_DIST_TO_BUILD_NET_GUN = 25;
	public static final boolean FLEE_BEFORE_BUILD = false;
	public static int DIST_SOUP_MULTIPLIER = 10;

	// crunching
	public static int CRUNCH_ROUND = 1610;
	public static int PICK_UP_LANDSCAPER_ROUND = 1410;

	// lattice
	//public static int[] WALL_Y_OFFSETS = {-2,-1,0,1,2,2,2,2,2,1,0,-1,-2,-2,-2,-2};
	// public static int[] WALL_X_OFFSETS = {-2,-2,-2,-2,-2,-1,0,1,2,2,2,2,2,1,0,-1};
	public static int[] WALL_X_OFFSETS = {-3,-2,-1,0,1,2,3,3,3,3,3,3,3,2,1,0,-1,-2,-3,-3,-3,-3,-3,-3};
	public static int[] WALL_Y_OFFSETS = {3,3,3,3,3,3,3,2,1,0,-1,-2,-3,-3,-3,-3,-3,-3,-3,-2,-1,0,1,2};
	public static final int HELP_MINER_UP_ROUND = 1000;
	public static int LATTICE_HEIGHT = 10;
	public static int LATTICE_TOLERANCE = 50;
	public static int WATER_TOLERANCE = 100;
	public static int BUBBLE_AROUND_HQ = 100;

	// build order
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
			20, //Drones
			23, //Vaps
			20, //Landscapers
	};
	public static RobotType[] RUSH_DEFENSE_COMP_TYPES = new RobotType[]{
			RobotType.DELIVERY_DRONE,
			RobotType.LANDSCAPER,
	};
	public static int[] RUSH_DEFENSE_ARMY_COMP = new int[]{
			1, //Drones
			1, //Landscapers
	};
	public static int NUM_NON_BUILD_MINERS = 6;
	public static int INITIAL_BUILD_MINERS = 1;
	public static int TURNS_FOR_VAP_TO_PAY = 250;
	public static int SOUP_RATIO_MULTIPLIER = 50;
	public static int NEW_BUILD_MINER_FREQ = 150;

	// rush defense prioritization
	public static int SPOTS_FREE_MULTIPLIER = 50;
	public static int SPOT_DIST_MULTIPLIER = 1;
    public static int MAX_BUILDING_DIST = 20;
    public static int NEXT_TO_BUILDING_BONUS = 1000;
    public static int STAY_HERE_BONUS = 50;
    public static int LANDSCAPER_DIFF_MULTIPLIER = 50;
    public static int HQ_DIRT_MULTIPLIER = 2;
	public static int MY_DIRT_MULTIPLIER = 0;
	public static int NET_GUN_BONUS = 100;
	public static int BUILDING_DIRT_MULTIPLIER = 2;
	public static int BUILDING_DIST_MULTIPLIER = 1;
	public static int BUILDING_ADJ_BONUS = 50;
	public static int BUILDING_DIFF_MULTIPLIER = 10;
	public static int BUILD_NET_GUN_ROUND = 200;
	public static int NOT_ATTACKED_ROUND = 300;
	public static int MAX_LATE_RUSH_DIST = 8;
	public static int REFINERY_BUFFER = 54;
	public static boolean BUILD_REFINERY = true;
	public static int NUM_RUSH_DEFENSE_MINERS = 10;

	// mining
	public static int MINER_ELEVATION_TOLERANCE = 10;
	public static int MAX_CLUSTER_DIST = RobotType.MINER.sensorRadiusSquared;
	public static int GIVE_UP_CLUSTER_DIST = 8;
	public static int REQUIRED_REFINERY_DIST = 50;
	public static int SOUP_REQUIRED_FOR_REFINERY = 320;
}
