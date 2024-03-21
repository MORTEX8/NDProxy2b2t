package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.VisualRange;
import discord4j.rest.util.Color;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Shared.*;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.command.util.CommandOutputHelper.playerListToString;
import static com.zenith.discord.DiscordBot.escape;
import static java.util.Arrays.asList;

public class VisualRangeCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "visualRange",
            CommandCategory.MODULE,
            "Configure the VisualRange notification feature",
            asList(
                        "on/off",
                        "mention on/off",
                        "friend add/del <player>",
                        "friend list",
                        "friend clear",
                        "ignoreFriends on/off",
                        "enter on/off",
                        "leave on/off",
                        "logout on/off",
                        "enemyReplayRecord on/off",
                        "enemyReplayRecord cooldown <minutes>"
                ),
            asList("vr")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("visualRange")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.visualRange.enabled = getToggle(c, "toggle");
                MODULE.get(VisualRange.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("VisualRange " + toggleStrCaps(CONFIG.client.extra.visualRange.enabled));
                return 1;
            }))
            .then(literal("enter")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRange.enterAlert = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("VisualRange Enter Alerts " + toggleStrCaps(CONFIG.client.extra.visualRange.enterAlert));
                            return 1;
                      }))
                      .then(literal("mention").then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRange.enterAlertMention = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("VisualRange Enter Mentions " + toggleStrCaps(CONFIG.client.extra.visualRange.enterAlertMention));
                            return 1;
                      }))))
            // todo: move friends to another command
            .then(literal("friend")
                      .then(literal("add").then(argument("player", string()).executes(c -> {
                          final String player = StringArgumentType.getString(c, "player");
                          PLAYER_LISTS.getFriendsList().add(player)
                              .ifPresentOrElse(e ->
                                                   c.getSource().getEmbed()
                                                       .title("Friend added"),
                                               () -> c.getSource().getEmbed()
                                                   .title("Failed to add user: " + escape(player) + " to friends. Unable to lookup profile."));
                          return 1;
                      })))
                      .then(literal("del").then(argument("player", string()).executes(c -> {
                          final String player = StringArgumentType.getString(c, "player");
                          PLAYER_LISTS.getFriendsList().remove(player);
                          c.getSource().getEmbed()
                              .title("Friend deleted");
                          return 1;
                      })))
                      .then(literal("list").executes(c -> {
                          c.getSource().getEmbed()
                              .title("Friend list");
                      }))
                      .then(literal("clear").executes(c -> {
                          PLAYER_LISTS.getFriendsList().clear();
                          c.getSource().getEmbed()
                              .title("Friend list cleared!");
                      })))
            .then(literal("ignoreFriends")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRange.ignoreFriends = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Ignore Friends " + toggleStrCaps(CONFIG.client.extra.visualRange.ignoreFriends));
                            return 1;
                      })))
            .then(literal("leave")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRange.leaveAlert = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Leave Alerts " + toggleStrCaps(CONFIG.client.extra.visualRange.leaveAlert));
                            return 1;
                      })))
            .then(literal("logout")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRange.logoutAlert = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Logout Alerts " + toggleStrCaps(CONFIG.client.extra.visualRange.logoutAlert));
                            return 1;
                      })))
            .then(literal("enemyReplayRecord")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.visualRange.replayRecording = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Enemy Replay Recording " + toggleStrCaps(CONFIG.client.extra.visualRange.replayRecording));
                            return 1;
                      }))
                      .then(literal("cooldown").then(argument("minutes", integer(0)).executes(c -> {
                          CONFIG.client.extra.visualRange.replayRecordingCooldownMins = getInteger(c, "minutes");
                          c.getSource().getEmbed()
                              .title("Enemy Replay Recording Cooldown Set");
                          return 1;
                      }))));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            // todo: move friends to another command
            .description("**Friend List**\n" + playerListToString(PLAYER_LISTS.getFriendsList()))
            .addField("VisualRange", toggleStr(CONFIG.client.extra.visualRange.enabled), false)
            .addField("Enter Alerts", toggleStr(CONFIG.client.extra.visualRange.enterAlert), false)
            .addField("Enter Mentions", toggleStr(CONFIG.client.extra.visualRange.enterAlertMention), false)
            .addField("Ignore Friends", toggleStr(CONFIG.client.extra.visualRange.ignoreFriends), false)
            .addField("Leave Alerts", toggleStr(CONFIG.client.extra.visualRange.leaveAlert), false)
            .addField("Logout Alerts", toggleStr(CONFIG.client.extra.visualRange.logoutAlert), false)
            .addField("Enemy Replay Recording", toggleStr(CONFIG.client.extra.visualRange.replayRecording), false)
            .addField("Enemy Replay Recording Cooldown", CONFIG.client.extra.visualRange.replayRecordingCooldownMins, false)
            .color(Color.CYAN);
    }
}
