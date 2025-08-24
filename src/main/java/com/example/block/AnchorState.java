package com.example.block;

import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

// 锚点状态，用于保存锚点的位置
public class AnchorState extends PersistentState{
    
    private static final String KEY = "anchor_state";
    private final List<BlockPos> anchors = new ArrayList<>();

    private AnchorState() {}
    private AnchorState(List<BlockPos> loaded) {
        // 通过 CODEC 读取到的列表
        if (loaded != null) anchors.addAll(loaded);
    }

    public List<BlockPos> getAnchors() {
        return anchors;
    }

    public void add(BlockPos pos) {
        if (!anchors.contains(pos)) {
            anchors.add(pos.toImmutable());
            markDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (anchors.remove(pos)) {
            markDirty();
        }
    }

    private static final Codec<AnchorState> CODEC =
            BlockPos.CODEC.listOf()
                    .fieldOf("anchors")
                    .codec()
                    .xmap(AnchorState::new, AnchorState::getAnchors);

    private static final PersistentStateType<AnchorState> TYPE =
            new PersistentStateType<>(
                KEY,
                AnchorState::new, // 没有存档时，创建空实例
                CODEC,
                null // DataFixTypes，可为 null
            );

    public static AnchorState getState(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        assert overworld != null;
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }
}
