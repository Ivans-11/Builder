package com.example;

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
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;

import java.io.FileReader;
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
        AnchorState cache = AnchorState.getState(world.getServer());// Get cache
        List<BlockPos> anchors = cache.getAnchors();// Get anchors
        for (BlockPos p : anchors) {
            player.sendMessage(Text.literal(p.toString()), false);
        }
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
        if (!(anchorState.getBlock() instanceof com.example.block.AnchorBlock)) {
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

            UndoManager.record(player, snapshots);// Record snapshots
            player.sendMessage(Text.literal("Build Success: " + filename), false);

        } catch (Exception e) {
            player.sendMessage(Text.literal("Load Failed: " + e.getMessage()), false);
            e.printStackTrace();
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
}
