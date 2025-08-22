package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import static net.minecraft.server.command.CommandManager.literal;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuilderMod implements ModInitializer {
	public static final String MOD_ID = "builder";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("builder")
					.then(CommandManager.argument("filename", StringArgumentType.string())
                            .executes(context -> {
                                String filename = StringArgumentType.getString(context, "filename");
								BuildHandler.loadAndPlace(context.getSource(), filename);
                                return 1;
                            })
                    )
            );
        });
    }
}