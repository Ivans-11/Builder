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

// State of anchor, used to save the position of anchors
public class AnchorState extends PersistentState{
    
    private static final String KEY = "anchor_state";
    private final List<BlockPos> anchors = new ArrayList<>();

    private AnchorState() {}
    private AnchorState(List<BlockPos> loaded) {
        // Read list from CODEC
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
                AnchorState::new, // Create a new instance when no data is saved
                CODEC,
                null // DataFixTypes，can be null
            );

    public static AnchorState getState(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        assert overworld != null;
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }
}
