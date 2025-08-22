package com.example;

import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BuildHandler {

    public static void loadAndPlace(ServerCommandSource source, String filename) {
        ServerWorld world = source.getWorld();
        ServerPlayerEntity player = source.getPlayer();
        BlockPos origin = player.getBlockPos();
        Direction facing = player.getHorizontalFacing();

        try {
            Path filePath = Paths.get("config/mybuilds", filename + ".json"); // 存放目录
            JsonArray arr = JsonParser.parseReader(new FileReader(filePath.toFile())).getAsJsonArray();

            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();

                String blockId = obj.get("block").getAsString();
                Block block = Registries.BLOCK.get(Identifier.of(blockId));
                if (block == Blocks.AIR) continue;

                JsonArray coords = obj.getAsJsonArray("pos");
                for (JsonElement coordEl : coords) {
                    JsonArray coord = coordEl.getAsJsonArray();
                    int dx = coord.get(0).getAsInt();
                    int dy = coord.get(1).getAsInt();
                    int dz = coord.get(2).getAsInt();

                    BlockPos placePos = transformPos(origin, dx, dy, dz, facing);
                    world.setBlockState(placePos, block.getDefaultState());
                }
            }

            player.sendMessage(Text.literal("Build Success: " + filename), false);

        } catch (Exception e) {
            player.sendMessage(Text.literal("Load Failed: " + e.getMessage()), false);
            e.printStackTrace();
        }
    }

    private static BlockPos transformPos(BlockPos origin, int dx, int dy, int dz, Direction facing) {
        switch (facing) {
            case NORTH: return origin.add(dx, dy, -dz);
            case SOUTH: return origin.add(-dx, dy, dz);
            case EAST: return origin.add(-dz, dy, -dx);
            case WEST: return origin.add(dz, dy, dx);
            default: return origin.add(dx, dy, dz); // 默认朝向
        }
    }
}
