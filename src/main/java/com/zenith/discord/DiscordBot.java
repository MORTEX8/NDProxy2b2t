package com.zenith.discord;

import com.zenith.Proxy;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.rest.RestClient;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;
import reactor.core.publisher.Mono;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.DISCORD_LOG;

public class DiscordBot {

    private RestClient restClient;
    private Proxy proxy;

    public DiscordBot() {
    }

    public void start(Proxy proxy) {
        this.proxy = proxy;
        GatewayDiscordClient client = DiscordClient.create(CONFIG.discord.token)
                .login()
                .block();
        restClient = client.getRestClient();
        long applicationId = restClient.getApplicationId().block();

        /**
         * Discord commands:
         *  /connect
         *      * Login player to server
         *  /disconnect
         *      * disconnect a player from the server
         *  /status
         *      * Get current status of player. e.g. are they in queue or ingame, what position in queue, how long online, etc.
         */

        ApplicationCommandRequest connectCommand = ApplicationCommandRequest.builder()
                .name("connect")
                .description("Connect proxy account to the server")
                // todo: add option for server to connect to here instead of getting it from config
                .build();
        ApplicationCommandRequest disconnectCommand = ApplicationCommandRequest.builder()
                .name("disconnect")
                .description("Disconnect proxy account from the server")
                .build();
        ApplicationCommandRequest statusCommand = ApplicationCommandRequest.builder()
                .name("status")
                .description("Gets the current player status on the proxy")
                .build();
        registerCommand(connectCommand, applicationId);
        registerCommand(disconnectCommand, applicationId);
        registerCommand(statusCommand, applicationId);

        client.on(ApplicationCommandInteractionEvent.class, event -> {
            RestChannel restChannel = restClient.getChannelById(event.getInteraction().getChannelId());
            switch (event.getCommandName()) {
                case "connect":
                    return handleConnectCommand(event, restChannel);
                case "disconnect":
                    return handleDisconnectCommand(event, restChannel);
                case "status":
                    return handleStatusCommand(event, restChannel);
                default:
                    break;
            }
            return event.reply("unhandled command");
        }).blockLast();
    }

    private void registerCommand(ApplicationCommandRequest findCommand, long applicationId) {
        restClient.getApplicationService()
                .createGuildApplicationCommand(applicationId, Snowflake.asLong(CONFIG.discord.guildId), findCommand)
                .doOnError(e -> DISCORD_LOG.fatal("Unable to create guild command", e))
                .doOnSuccess(applicationCommandData -> {
                    DISCORD_LOG.trace("Created command");
                })
                .onErrorResume(e -> Mono.empty())
                .block();
    }

    private Mono<Void> handleConnectCommand(ApplicationCommandInteractionEvent event, RestChannel restChannel) {
        return event.reply("Connecting...").doOnSuccess(unused -> {
            try {
                userAllowed(event);
                this.proxy.connect();
                restChannel.createMessage(getConnectMessageCreateRequest(true)).block();
            } catch (final Exception e) {
                restChannel.createMessage(getConnectMessageCreateRequest(false)).block();
                DISCORD_LOG.error("Failed to connect", e);
            }
        });
    }

    private Mono<Void> handleDisconnectCommand(ApplicationCommandInteractionEvent event, RestChannel restChannel) {
        return event.reply("Disconnecting...").doOnSuccess(unused -> {
            try {
                userAllowed(event);
                this.proxy.disconnect();
                restChannel.createMessage(getDisconnectMessageCreateRequest(true)).block();
            } catch (final Exception e) {
                restChannel.createMessage(getDisconnectMessageCreateRequest(false)).block();
                DISCORD_LOG.error("Failed to disconnect", e);
            }
        });
    }

    private Mono<Void> handleStatusCommand(ApplicationCommandInteractionEvent event, RestChannel restChannel) {
        return event.reply("Getting Status...").doOnSuccess(unused -> {
            restChannel.createMessage(getStatusMessageCreateRequest()).block();
        });
    }

    private MultipartRequest<MessageCreateRequest> getConnectMessageCreateRequest(boolean success) {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Connection " + (success ? "Succeeded" : "Failed") + " : " + CONFIG.authentication.username)
                        .color((success ? Color.LIGHT_SEA_GREEN : Color.RED))
                        .image(this.proxy.getAvatarURL().toString())
                        .build())
                .build().asRequest();
    }

    private MultipartRequest<MessageCreateRequest> getDisconnectMessageCreateRequest(boolean success) {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Disconnect " + (success ? "Succeeded" : "Failed") + " : " + CONFIG.authentication.username)
                        .color((success ? Color.LIGHT_SEA_GREEN : Color.RED))
                        .image(this.proxy.getAvatarURL().toString())
                        .build())
                .build().asRequest();
    }

    private MultipartRequest<MessageCreateRequest> getStatusMessageCreateRequest() {
        return MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title("ZenithProxy Status" + " : " + CONFIG.authentication.username)
                        .color(Color.CYAN)
                        .addField("Status", proxy.isConnected() ? "Connected" : "Disconnected", true)
                        .addField("Queue Position", proxy.isInQueue() ? "" + proxy.getQueuePosition() : "N/A", true)
                        .image(this.proxy.getAvatarURL().toString())
                        .build())
                .build().asRequest();
    }

    private void userAllowed(ApplicationCommandInteractionEvent event) {
        String id = event.getInteraction().getMember().get().getId().asString();
        if (!CONFIG.discord.allowedUsers.contains(id) && !CONFIG.discord.allowedUsers.isEmpty()) {
            throw new RuntimeException("Not an allowed user");
        }
    }
}
