package com.example;

import com.example.block.AnchorBlock;
import com.example.block.AnchorState;
import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildHandler {
    public static final String PARENT_PATH = "config/mybuilds";

    // Create a new directory if it doesn't exist
    public static void init() {
        File dir = new File(PARENT_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // List all JSON files in the directory
    public static void listBuilds(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        Path filePath = Paths.get(PARENT_PATH);
        String[] files = filePath.toFile().list();
        if (files!= null) {
            for (String file : files) {
                if (file.endsWith(".json")) {
                    player.sendMessage(Text.literal(file), false);
                }
            }
        }
    }

    // List all AnchorBlock positions
    public static void listAnchors(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        List<BlockPos> anchors = getAnchors(world);// Get anchors
        if (anchors.isEmpty()) {
            player.sendMessage(Text.literal("No Anchors Found"), false);
            return;
        }
        player.sendMessage(Text.literal("Anchors:"), false);
        for (BlockPos p : anchors) {
            player.sendMessage(Text.literal(p.toString()), false);
        }
    }

    // Clear all AnchorBlocks
    public static void clearAnchors(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        ServerWorld world = source.getWorld();
        List<BlockPos> anchors = getAnchors(world);// Get anchors
        for (BlockPos p : anchors) {
            BlockState state = world.getBlockState(p);
            if (state.getBlock() instanceof AnchorBlock) {
                world.setBlockState(p, Blocks.AIR.getDefaultState());// Set to air
            }
        }
        player.sendMessage(Text.literal("All Anchors Cleared"), false);
    }

    // Load and place a build from a JSON file
    public static void loadAndPlace(ServerCommandSource source, String filename) {
        ServerWorld world = source.getWorld();
        ServerPlayerEntity player = source.getPlayer();
        BlockPos playerPos = player.getBlockPos();
        
        AnchorState cache = AnchorState.getState(world.getServer());// Get cache
        BlockPos anchorPos = findNearest(cache.getAnchors(), playerPos);// Find nearest anchor
        if (anchorPos == null) {
            player.sendMessage(Text.literal("Not Found Anchor, Please Place Anchor First"), false);
            return;
        }

        BlockState anchorState = world.getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof AnchorBlock)) {
            // Not found anchor
            player.sendMessage(Text.literal("Invalid Anchor, Please Place Anchor First"), false);
            return;
        }

        Direction facing = anchorState.get(Properties.HORIZONTAL_FACING);
        BlockPos origin = anchorPos;

        try {
            Path filePath = Paths.get(PARENT_PATH, filename + ".json");
            JsonObject obj = JsonParser.parseReader(new FileReader(filePath.toFile())).getAsJsonObject();

            List<UndoManager.BlockSnapshot> snapshots = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String blockId = entry.getKey();
                Block block = Registries.BLOCK.get(Identifier.of(blockId));
                if (block == Blocks.AIR) continue;

                JsonArray coords = entry.getValue().getAsJsonArray();

                // If it is an anchor block, record the position
                if (block instanceof AnchorBlock) {
                    handleAnchors(world, coords, block, facing, origin, snapshots, cache);
                    continue;
                }

                // If it is a block with facing
                if (block.getDefaultState().contains(Properties.HORIZONTAL_FACING)) {
                    handleFacing(world, coords, block, facing, origin, snapshots);
                    continue;
                }

                // If it is a block with axis
                if (block.getDefaultState().contains(Properties.AXIS)) {
                    handleAxis(world, coords, block, facing, origin, snapshots);
                    continue;
                }

                handleNormal(world, coords, block, facing, origin, snapshots);
            }

            UndoManager.record(player, snapshots);// Record snapshots
            player.sendMessage(Text.literal("Build Success: " + filename), false);

        } catch (Exception e) {
            player.sendMessage(Text.literal("Load Failed: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }

    // Save structure to a JSON file
    public static void saveStructure(ServerCommandSource source, int x, int y, int z, String name) {
        ServerWorld world = source.getWorld();
        ServerPlayerEntity player = source.getPlayer();
        BlockPos playerPos = player.getBlockPos();

        AnchorState cache = AnchorState.getState(world.getServer());// Get cache
        BlockPos anchorPos = findNearest(cache.getAnchors(), playerPos);// Find nearest anchor
        if (anchorPos == null) {
            player.sendMessage(Text.literal("Not Found Anchor, Please Place Anchor First"), false);
            return;
        }

        BlockState anchorState = world.getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof AnchorBlock)) {
            // Not found anchor
            player.sendMessage(Text.literal("Invalid Anchor, Please Place Anchor First"), false);
            return;
        }

        Direction facing = anchorState.get(Properties.HORIZONTAL_FACING);
        BlockPos origin = anchorPos;

        // Record the structure
        JsonObject obj = new JsonObject();
        for (int dx = 0; dx <= x; dx++) {
            for (int dy = 0; dy <= y; dy++) {
                for (int dz = 0; dz <= z; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos placePos = transformPos(origin, dx, dy, dz, facing);
                    BlockState state = world.getBlockState(placePos);
                    Block block = state.getBlock();
                    if (block != Blocks.AIR) {
                        String blockId = Registries.BLOCK.getId(block).toString();
                        JsonArray arr = new JsonArray();
                        arr.add(dx);
                        arr.add(dy);
                        arr.add(dz);

                        // Record the rotation of the block
                        if (state.contains(Properties.HORIZONTAL_FACING)) {
                            Direction blockFacing = state.get(Properties.HORIZONTAL_FACING);
                            arr.add(relativeDirection(facing, blockFacing));
                        } else if (state.contains(Properties.AXIS)) {
                            Axis blockAxis = state.get(Properties.AXIS);
                            arr.add(relativeAxis(facing, blockAxis));
                        }

                        if (obj.has(blockId)) {
                            obj.get(blockId).getAsJsonArray().add(arr);
                        } else {
                            JsonArray coords = new JsonArray();
                            coords.add(arr);
                            obj.add(blockId, coords);
                        }
                    }
                }
            }
        }

        // Save to file
        Path filePath = Paths.get(PARENT_PATH, name + ".json");
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(obj.toString());
            player.sendMessage(Text.literal("Save Success: " + name), false);
        } catch (Exception e) {
            player.sendMessage(Text.literal("Save Failed: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }

    // Place normal blocks
    private static void handleNormal(ServerWorld world, JsonArray coords, Block block, Direction facing, BlockPos origin, List<UndoManager.BlockSnapshot> snapshots) {
        for (JsonElement coordEl : coords) {
            JsonArray arr = coordEl.getAsJsonArray();
            int dx = arr.get(0).getAsInt();
            int dy = arr.get(1).getAsInt();
            int dz = arr.get(2).getAsInt();

            BlockPos placePos = transformPos(origin, dx, dy, dz, facing);

            BlockState oldState = world.getBlockState(placePos);
            snapshots.add(new UndoManager.BlockSnapshot(placePos, oldState));

            world.setBlockState(placePos, block.getDefaultState());
        }
    }

    // Place anchor blocks
    private static void handleAnchors(ServerWorld world, JsonArray coords, Block block, Direction facing, BlockPos origin, List<UndoManager.BlockSnapshot> snapshots, AnchorState cache) {
        for (JsonElement coordEl : coords) {
            JsonArray arr = coordEl.getAsJsonArray();
            int dx = arr.get(0).getAsInt();
            int dy = arr.get(1).getAsInt();
            int dz = arr.get(2).getAsInt();
            int rotation = arr.size() > 3 ? arr.get(3).getAsInt() : 0;

            BlockPos placePos = transformPos(origin, dx, dy, dz, facing);

            BlockState oldState = world.getBlockState(placePos);
            snapshots.add(new UndoManager.BlockSnapshot(placePos, oldState));

            world.setBlockState(placePos, block.getDefaultState().with(Properties.HORIZONTAL_FACING, absoluteDirection(facing, rotation)));
            cache.add(placePos);// Add to cache
        }
    }

    // Place blocks with facing
    private static void handleFacing(ServerWorld world, JsonArray coords, Block block, Direction facing, BlockPos origin, List<UndoManager.BlockSnapshot> snapshots) {
        for (JsonElement coordEl : coords) {
            JsonArray arr = coordEl.getAsJsonArray();
            int dx = arr.get(0).getAsInt();
            int dy = arr.get(1).getAsInt();
            int dz = arr.get(2).getAsInt();
            int rotation = arr.size() > 3? arr.get(3).getAsInt() : 0;

            BlockPos placePos = transformPos(origin, dx, dy, dz, facing);

            BlockState oldState = world.getBlockState(placePos);
            snapshots.add(new UndoManager.BlockSnapshot(placePos, oldState));

            world.setBlockState(placePos, block.getDefaultState().with(Properties.HORIZONTAL_FACING, absoluteDirection(facing, rotation)));
        }
    }

    // Place blocks with axis
    private static void handleAxis(ServerWorld world, JsonArray coords, Block block, Direction facing, BlockPos origin, List<UndoManager.BlockSnapshot> snapshots) {
        for (JsonElement coordEl : coords) {
            JsonArray arr = coordEl.getAsJsonArray();
            int dx = arr.get(0).getAsInt();
            int dy = arr.get(1).getAsInt();
            int dz = arr.get(2).getAsInt();
            int axis = arr.size() > 3? arr.get(3).getAsInt() : 0;

            BlockPos placePos = transformPos(origin, dx, dy, dz, facing);

            BlockState oldState = world.getBlockState(placePos);
            snapshots.add(new UndoManager.BlockSnapshot(placePos, oldState));

            world.setBlockState(placePos, block.getDefaultState().with(Properties.AXIS, absoluteAxis(facing, axis)));
        }
    }

    // Transform position based on facing
    private static BlockPos transformPos(BlockPos origin, int dx, int dy, int dz, Direction facing) {
        switch (facing) {
            case NORTH: return origin.add(dx, dy, dz);
            case SOUTH: return origin.add(-dx, dy, -dz);
            case EAST: return origin.add(-dz, dy, dx);
            case WEST: return origin.add(dz, dy, -dx);
            default: return origin.add(dx, dy, dz);
        }
    }

    // Find the nearest anchor
    private static BlockPos findNearest(List<BlockPos> anchors, BlockPos playerPos) {
        double best = Double.MAX_VALUE;
        BlockPos bestPos = null;
        for (BlockPos p : anchors) {
            double d = p.getSquaredDistance(playerPos);
            if (d < best) {
                best = d;
                bestPos = p;
            }
        }
        return bestPos;
    }

    private static List<BlockPos> getAnchors(ServerWorld world) {
        AnchorState cache = AnchorState.getState(world.getServer());// Get cache
        // Get copy of anchors to avoid ConcurrentModificationException
        return new ArrayList<>(cache.getAnchors());
    }

    // Get the relative direction of the block to the anchor (using positive numbers)
    private static int relativeDirection(Direction anchorFacing, Direction blockFacing) {
        int offset = intDirection(blockFacing) - intDirection(anchorFacing);
        if (offset < 0) offset += 4;
        return offset;
    }

    // Get the relative axis of the block to the anchor (using negative numbers)
    private static int relativeAxis(Direction anchorFacing, Axis blockAxis) {
        if (blockAxis == Axis.X) {
            if (anchorFacing == Direction.NORTH || anchorFacing == Direction.SOUTH) return -1;
            if (anchorFacing == Direction.EAST || anchorFacing == Direction.WEST) return -2;
        } else if (blockAxis == Axis.Z) {
            if (anchorFacing == Direction.NORTH || anchorFacing == Direction.SOUTH) return -2;
            if (anchorFacing == Direction.EAST || anchorFacing == Direction.WEST) return -1;
        }
        return -3;
    }

    // Get the absolute direction of the block based on the anchor and the relative direction
    private static Direction absoluteDirection(Direction anchorFacing, int relativeDirection) {
        int absolute = intDirection(anchorFacing) + relativeDirection;
        if (absolute >= 4) absolute -= 4;
        return getDirection(absolute);
    }

    // Get the absolute axis of the block based on the anchor and the relative axis
    private static Axis absoluteAxis(Direction anchorFacing, int relativeAxis) {
        if (anchorFacing == Direction.NORTH || anchorFacing == Direction.SOUTH) {
            if (relativeAxis == -1) return Axis.X;
            if (relativeAxis == -2) return Axis.Z;
        } else if (anchorFacing == Direction.EAST || anchorFacing == Direction.WEST) {
            if (relativeAxis == -1) return Axis.Z;
            if (relativeAxis == -2) return Axis.X;
        }
        return Axis.Y;
    }

    // Convert direction to integer
    private static int intDirection(Direction dir) {
        switch (dir) {
            case NORTH: return 0;
            case EAST: return 1;
            case SOUTH: return 2;
            case WEST: return 3;
            default: return 0;
        }
    }

    // Convert integer to direction
    private static Direction getDirection(int dir) {
        switch (dir) {
            case 0: return Direction.NORTH;
            case 1: return Direction.EAST;
            case 2: return Direction.SOUTH;
            case 3: return Direction.WEST;
            default: return Direction.NORTH;
        }
    }
}
