package com.example;

import java.util.function.Function;

import com.example.block.AnchorBlock;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class BuilderBlocks {// 方块注册
    public static final AnchorBlock ANCHOR_BLOCK = register("anchor_block", AnchorBlock::new, Block.Settings.create().strength(4.0f));
    public static final BlockItem ANCHOR_BLOCK_ITEM = registerItem("anchor_block", settings -> new BlockItem(ANCHOR_BLOCK, settings));
    
    private static <T extends Block> T register(String path, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings settings) {
        final Identifier identifier = Identifier.of("builder", path);
        final RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK, identifier);
        return Registry.register(Registries.BLOCK, identifier, factory.apply(settings.registryKey(registryKey)));
    }

    private static <T extends Item> T registerItem(String path, Function<Item.Settings, T> itemFunction) {
        final Identifier identifier = Identifier.of("builder", path);
		final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, identifier);
		return Registry.register(Registries.ITEM, registryKey, itemFunction.apply(new Item.Settings().registryKey(registryKey)));
	}
    
    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(ANCHOR_BLOCK_ITEM);
        });// 将方块加入到构建方块组
    }
}