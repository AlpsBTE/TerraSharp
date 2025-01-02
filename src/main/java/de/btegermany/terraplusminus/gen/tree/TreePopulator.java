package de.btegermany.terraplusminus.gen.tree;

import com.alpsbte.alpslib.io.config.ConfigurationUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import de.btegermany.terraplusminus.TerraSharp;
import de.btegermany.terraplusminus.gen.CustomBiomeProvider;
import de.btegermany.terraplusminus.utils.io.ConfigPaths;
import de.btegermany.terraplusminus.utils.io.ConfigUtil;
import net.buildtheearth.terraminusminus.generator.CachedChunkData;
import net.buildtheearth.terraminusminus.generator.ChunkDataLoader;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorPipelines;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.generator.data.TreeCoverBaker;
import net.buildtheearth.terraminusminus.substitutes.BlockState;
import net.buildtheearth.terraminusminus.substitutes.ChunkPos;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class TreePopulator extends BlockPopulator {

    public static final Cached<byte[]> RNG_CACHE = Cached.threadLocal(() -> new byte[16 * 16], ReferenceStrength.SOFT);
    public final LoadingCache<ChunkPos, CompletableFuture<CachedChunkData>> cache;
    private final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    ChunkDataLoader loader = new ChunkDataLoader(bteGeneratorSettings);
    int xOffset;
    int yOffset;
    int zOffset;
    boolean generateTrees; // Should Trees be added to the Terrain
    String surface;
    CustomBiomeProvider customBiomeProvider;

    // List of Possible trees by type
    HashMap<String, ArrayList<ArrayList<TreeBlock>>> trees = new HashMap<>();


    public TreePopulator(CustomBiomeProvider customBiomeProvider, int yOffset) {
        this.customBiomeProvider = customBiomeProvider;

        ConfigurationUtil.ConfigFile configFile = ConfigUtil.getInstance().configs[0];

        this.xOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_X);
        this.yOffset = yOffset;
        this.zOffset = configFile.getInt(ConfigPaths.TERRAIN_OFFSET_Z);
        this.generateTrees = configFile.getBoolean(ConfigPaths.GENERATE_TREES);
        this.surface = configFile.getString(ConfigPaths.SURFACE_MATERIAL);
        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5L, TimeUnit.MINUTES)
                .softValues()
                .build(new ChunkDataLoader(this.bteGeneratorSettings));


        // Load Trees from customTrees.json
        JsonObject treeTypes = getJSONObject();
        final int[] treeCount = {0};
        treeTypes.entrySet().forEach(treeSizes -> {

            trees.put(treeSizes.getKey(), new ArrayList<>());

            TerraSharp.instance.getComponentLogger().info("[T#] Loading Tree Type " + treeSizes.getKey());

            treeSizes.getValue().getAsJsonObject().entrySet().forEach(treeNames -> {

                treeNames.getValue().getAsJsonObject().entrySet().forEach(tree -> {

                    treeCount[0]++;
                    ArrayList<TreeBlock> treeBlocks = new ArrayList<>();

                    tree.getValue().getAsJsonObject().get("blocks").getAsJsonArray().forEach(treeBlockElement -> {

                        JsonObject treeBlock = treeBlockElement.getAsJsonObject();
                        treeBlocks.add(new TreeBlock(treeBlock.get("x").getAsInt(), treeBlock.get("y").getAsInt(), treeBlock.get("z").getAsInt(), Material.getMaterial(treeBlock.get("material").getAsString())));

                    });

                    trees.get(treeSizes.getKey()).add(treeBlocks);

                });

            });

        });
        TerraSharp.instance.getComponentLogger().info("[T#] Finished loading " + treeCount[0] + " custom trees");
    }

    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull LimitedRegion limitedRegion) {
        World world = Bukkit.getWorld(worldInfo.getName());
        if (!generateTrees) return;
        try {
            CachedChunkData data = this.loader.load(new ChunkPos(x - (xOffset / 16), z - (zOffset / 16))).get();

            byte[] treeCover = data.getCustom(EarthGeneratorPipelines.KEY_DATA_TREE_COVER, TreeCoverBaker.FALLBACK_TREE_DENSITY);
            byte[] rng = RNG_CACHE.get();

            for (int i = 0, dx = 0; dx < 16 >> 1; dx++) {
                for (int dz = 0; dz < 16 >> 1; dz++, i++) {
                    if ((rng[i] & 0xFF) < (treeCover[(((x * 16) & 0xF) << 4) | ((z * 16) & 0xF)] & 0xFF)) {
                        random.nextBytes(rng);

                        int valueX = random.nextInt(15) + 1; // Depending on the size of the tree this should be changed
                        int valueZ = random.nextInt(15) + 1;
                        int groundY = 0;
                        int waterY = 0;
                        BlockState state = data.surfaceBlock(0, 0);

                        try {
                            groundY = data.groundHeight(valueX, valueZ);
                            waterY = data.waterHeight(valueX, valueZ);
                            state = data.surfaceBlock(valueX, valueZ);
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }

                        if (groundY < waterY) {
                            return;
                        }

                        Location loc = new Location(world, valueX + x * 16, groundY + 1 + yOffset, valueZ + z * 16); // is offset missing?
                        if (!(groundY < waterY) && groundY + yOffset < world.getMaxHeight() - 35 && groundY + yOffset > world.getMinHeight() && state == null) {
                            switch ((int) customBiomeProvider.getBiome()) {
                                // desert and savanna
                                case 4, 6, 17 -> generateCustomTree(limitedRegion, loc, "savanna");
                                // flower forest
                                case 14, 15 -> generateCustomTree(limitedRegion, loc, "oak", "birch");
                                // taiga
                                case 27 -> generateCustomTree(limitedRegion, loc, "spruce");
                                // snowy regions
                                case 28, 29, 30 -> {
                                    // TODO: trees with snow
                                }
                                default -> generateCustomTree(limitedRegion, loc, "oak", "birch");
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void generateCustomTree(LimitedRegion limitedRegion, Location loc, String... types) {

        ArrayList<ArrayList<TreeBlock>> trees = new ArrayList<>();
        for (String type : types) {
            trees.addAll(this.trees.get(type));
        }

        // Random Tree
        if (trees.size() == 0) return;

        int randTree = (new Random()).nextInt(trees.size());
        if (randTree < 0) randTree = 0;
        if (randTree > trees.size() - 1) randTree = trees.size() - 1;
        ArrayList<TreeBlock> tree = trees.get(randTree);

        int originX = loc.getBlockX();
        int originY = loc.getBlockY();
        int originZ = loc.getBlockZ();


        // Rotate Tree Randomly
        Random rand = new Random();
        int angle = rand.nextInt(4) * 90;

        // Place Tree
        for (TreeBlock block : tree) {
            int x = block.getX();
            int z = block.getZ();
            if (angle == 90) {
                int temp = x;
                x = -z;
                z = temp;
            } else if (angle == 180) {
                x = -x;
                z = -z;
            } else if (angle == 270) {
                int temp = x;
                x = z;
                z = -temp;
            }
            limitedRegion.setType(originX + x, originY + block.getY(), originZ + z, block.getMaterial());
        }
    }

    public JsonObject getJSONObject() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("assets/terraplusminus/data/customTrees.json");

        JsonReader reader;
        reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(reader);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        return jsonObject.get("trees").getAsJsonObject();
    }

}