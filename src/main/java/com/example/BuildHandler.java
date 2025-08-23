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
import java.util.List;

public class BuildHandler {
    public static final String PARENT_PATH = "config/mybuilds";

    // 创建目录
    public static void init() {
        File dir = new File(PARENT_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // 列举文件名
    public static void listBuilds(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        Path filePath = Paths.get(PARENT_PATH); // 存放目录
        String[] files = filePath.toFile().list();
        if (files!= null) {
            for (String file : files) {
                if (file.endsWith(".json")) {
                    player.sendMessage(Text.literal(file), false);
                }
            }
        }
    }

    // 加载并放置方块
    public static void loadAndPlace(ServerCommandSource source, String filename) {
        ServerWorld world = source.getWorld();
        ServerPlayerEntity player = source.getPlayer();
        BlockPos playerPos = player.getBlockPos();
        
        AnchorState cache = AnchorState.getState(world.getServer());// 获取缓存
        BlockPos anchorPos = findNearest(cache.getAnchors(), playerPos);// 查找最近的锚点
        if (anchorPos == null) {
            player.sendMessage(Text.literal("Not Found Anchor, Please Place Anchor First"), false);
            return;
        }

        BlockState anchorState = world.getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof com.example.block.AnchorBlock)) {
            // 缓存中的 Anchor 无效或不在当前维度已加载区块
            player.sendMessage(Text.literal("Invalid Anchor, Please Place Anchor First"), false);
            return;
        }

        Direction facing = anchorState.get(Properties.HORIZONTAL_FACING);
        BlockPos origin = anchorPos;

        try {
            Path filePath = Paths.get(PARENT_PATH, filename + ".json"); // 存放目录
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

    // 转换位置
    private static BlockPos transformPos(BlockPos origin, int dx, int dy, int dz, Direction facing) {
        switch (facing) {
            case NORTH: return origin.add(-dx, dy, dz);
            case SOUTH: return origin.add(dx, dy, -dz);
            case EAST: return origin.add(-dz, dy, -dx);
            case WEST: return origin.add(dz, dy, dx);
            default: return origin.add(dx, dy, dz); // 默认朝向
        }
    }

    // 寻找最近的锚点方块
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
