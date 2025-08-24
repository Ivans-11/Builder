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
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
    public void onInitialize() {

        // Register the blocks
        BuilderBlocks.init();

        // Initialize the build handler
        BuildHandler.init();

        // Register the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("builder")
                    .then(literal("place")// Place blocks based on the file
					        .then(CommandManager.argument("filename", StringArgumentType.string())
                                    .executes(context -> {
                                        String filename = StringArgumentType.getString(context, "filename");
								        BuildHandler.loadAndPlace(context.getSource(), filename);
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("list")// List available build files
                            .executes(context -> {
                                BuildHandler.listBuilds(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("anchors")// List anchor positions
                           .executes(context -> {
                                BuildHandler.listAnchors(context.getSource());
                                return 1;
                            })
                    )
                    .then(literal("undo")// Undo the last build action
                          .executes(context -> {
                                UndoManager.undo(context.getSource().getPlayer(), context.getSource().getWorld());
                                return 1;
                            })
                    )
                    .then(literal("help")// Display help message
                          .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Builder Mod Commands:"), false);
                                context.getSource().sendFeedback(() -> Text.literal("/builder place <filename> - Place blocks based on the specified file."), false);
                                context.getSource().sendFeedback(() -> Text.literal("/builder list - List available build files."), false);
                                context.getSource().sendFeedback(() -> Text.literal("/builder anchors - List anchor positions."), false);
                                context.getSource().sendFeedback(() -> Text.literal("/builder undo - Undo the last build action."), false);
                                return 1;
                            })
                    )
            );
        });
    }
}