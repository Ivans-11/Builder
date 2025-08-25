package com.example;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.block.AnchorBlock;
import com.example.block.AnchorState;

import net.minecraft.block.BlockState;
//import net.minecraft.block.entity.BlockEntity;
//import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class UndoManager {
    private static final int MAX_UNDO = 3;
    private static final Map<UUID, Deque<List<BlockSnapshot>>> history = new HashMap<>();

    public static void record(ServerPlayerEntity player, List<BlockSnapshot> snapshots) {
        Deque<List<BlockSnapshot>> stack = history.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());
        
        // If the stack is full, remove the oldest operation
        if (stack.size() >= MAX_UNDO) {
            stack.removeLast();
        }
        
        stack.push(snapshots); // Newest operation is at the top of the stack
    }

    public static void undo(ServerPlayerEntity player, ServerWorld world) {
        Deque<List<BlockSnapshot>> stack = history.get(player.getUuid());
        if (stack == null || stack.isEmpty()) {
            player.sendMessage(Text.literal("No Undo History"), false);
            return;
        }

        List<BlockSnapshot> snapshots = stack.pop();
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            BlockSnapshot snap = snapshots.get(i);
            world.setBlockState(snap.pos, snap.state);
            //if (snap.nbt!= null) {
                //BlockEntity be = world.getBlockEntity(snap.pos);
                //if (be!= null) {
                    //be.readNbt(snap.nbt);
                //}
            //}
            // If it is an anchor block, record the position
            if (snap.state.getBlock() instanceof AnchorBlock) {
                AnchorState.getState(world.getServer()).add(snap.pos);
            }
        }

        player.sendMessage(Text.literal("Undo Success"), false);
    }

    public static class BlockSnapshot {
        public final BlockPos pos;
        public final BlockState state;
        //public final NbtCompound nbt;

        public BlockSnapshot(BlockPos pos, BlockState state/*, NbtCompound nbt*/) {
            this.pos = pos.toImmutable();
            this.state = state;
            //this.nbt = nbt == null ? null : nbt.copy();
        }
    }
}

