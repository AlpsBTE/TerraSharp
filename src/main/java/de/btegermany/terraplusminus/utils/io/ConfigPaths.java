package de.btegermany.terraplusminus.utils.io;

public class ConfigPaths {
    private ConfigPaths() {
        throw new IllegalStateException("Utility class"); // Disable instantiation (Static Class)
    }

    // Chat Formatting
    private static final String CHAT_FORMAT = "chat-format.";
    public static final String CHAT_FORMAT_INFO_PREFIX = CHAT_FORMAT + "info-prefix";
    public static final String CHAT_FORMAT_ALERT_PREFIX = CHAT_FORMAT + "alert-prefix";

    public static final String REDUCED_CONSOLE_MESSAGES = "reduced_console_messages";

    public static final String HEIGHT_DATAPACK = "height_datapack";

    public static final String HEIGHT_IN_ACTIONBAR = "height_in_actionbar";

    // Terrain Offset
    private static final String TERRAIN_OFFSET = "terrain_offset.";
    public static final String TERRAIN_OFFSET_X = TERRAIN_OFFSET + "x";
    public static final String TERRAIN_OFFSET_Y = TERRAIN_OFFSET + "y";
    public static final String TERRAIN_OFFSET_Z = TERRAIN_OFFSET + "z";

    // Bounds
    public static final String MIN_LATITUDE = "min_latitude";
    public static final String MAX_LATITUDE = "max_latitude";
    public static final String MIN_LONGITUDE = "min_longitude";
    public static final String MAX_LONGITUDE = "max_longitude";

    public static final String PASSTHROUGH_TPLL = "passthrough_tpll";

    // Linked Worlds
    private static final String LINKED_WORLDS = "linked_worlds.";
    public static final String LINKED_WORLDS_ENABLED = LINKED_WORLDS + "enabled";
    public static final String LINKED_WORLDS_METHOD = LINKED_WORLDS + "method";
    public static final String LINKED_WORLDS_WORLDS = LINKED_WORLDS + "worlds";

    public static final String GENERATE_TREES = "generate_trees";
    public static final String DIFFERENT_BIOMES = "different_biomes";
    public static final String SURFACE_MATERIAL = "surface_material";
    public static final String BUILDING_OUTLINES_MATERIAL = "building_outlines_material";
    public static final String ROAD_MATERIAL = "road_material";
    public static final String PATH_MATERIAL = "path_material";

}
