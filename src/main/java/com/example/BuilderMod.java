package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

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

        // 注册方块
        BuilderBlocks.init();

        // 处理器初始化
        BuildHandler.init();

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("builder")
                    .then(literal("place")// 根据文件批量放置方块
					        .then(CommandManager.argument("filename", StringArgumentType.string())
                                    .executes(context -> {
                                        String filename = StringArgumentType.getString(context, "filename");
								        BuildHandler.loadAndPlace(context.getSource(), filename);
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("list")// 列举文件名
                            .executes(context -> {
                                BuildHandler.listBuilds(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("anchors")// 列举锚点位置
                           .executes(context -> {
                                BuildHandler.listAnchors(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("help")// 帮助
                          .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Builder Mod Commands:"), false);
                                context.getSource().sendFeedback(() -> Text.literal("/builder place <filename> - Place blocks based on the specified file."), false);
                                context.getSource().sendFeedback(() -> Text.literal("/builder list - List available build files."), false);
                                context.getSource().sendFeedback(() -> Text.literal("/builder anchors - List anchor positions."), false);
                                return 1;
                            })
                    )
            );
        });
    }
}