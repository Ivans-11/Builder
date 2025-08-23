package com.example;

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
        //BlockPos origin = player.getBlockPos();
        //Direction facing = player.getHorizontalFacing();
        AnchorResult anchor = findAnchorBlock(world, player.getBlockPos(), 10);
        if (anchor == null) {
            player.sendMessage(Text.literal("No Anchor Block Found or Too Far, Please Place One Nearby"), false);
            return;
        }

        BlockPos origin = anchor.pos;
        Direction facing = anchor.facing;

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

    // 寻找锚点方块
    private static AnchorResult findAnchorBlock(ServerWorld world, BlockPos center, int radius) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();// 可变的位置对象
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(mutable);
                    if (state.getBlock() instanceof com.example.block.AnchorBlock) {
                        Direction facing = state.get(Properties.HORIZONTAL_FACING);
                        return new AnchorResult(mutable.toImmutable(), facing);
                    }
                }
            }
        }
        return null;
    }
    private record AnchorResult(BlockPos pos, Direction facing) {}
}
