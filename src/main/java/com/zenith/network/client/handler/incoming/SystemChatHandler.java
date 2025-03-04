package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.event.proxy.QueueSkipEvent;
import com.zenith.event.proxy.SelfDeathMessageEvent;
import com.zenith.event.proxy.chat.DeathMessageChatEvent;
import com.zenith.event.proxy.chat.PublicChatEvent;
import com.zenith.event.proxy.chat.SystemChatEvent;
import com.zenith.event.proxy.chat.WhisperChatEvent;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.DeathMessagesParser;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import com.zenith.util.ComponentSerializer;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.Objects;
import java.util.Optional;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class SystemChatHandler implements ClientEventLoopPacketHandler<ClientboundSystemChatPacket, ClientSession> {
    private static final TextColor DEATH_MSG_COLOR_2b2t = TextColor.color(170, 0, 0);
    private final DeathMessagesParser deathMessagesHelper = new DeathMessagesParser();

    @Override
    public boolean applyAsync(@NonNull ClientboundSystemChatPacket packet, @NonNull ClientSession session) {
        try {
            if (CONFIG.client.extra.logChatMessages) {
                String serializedChat = ComponentSerializer.serializeJson(packet.getContent());
                if (Proxy.getInstance().isInQueue()) serializedChat = serializedChat.replace("\\n\\n", "");
                CHAT_LOG.info(serializedChat);
            }
            final Component component = packet.getContent();
            final String messageString = ComponentSerializer.serializePlain(component);
            Optional<DeathMessageParseResult> deathMessage = Optional.empty();
            String senderName = null;
            String whisperTarget = null;
            if (!messageString.startsWith("<") && Proxy.getInstance().isOn2b2t())
                deathMessage = parseDeathMessage2b2t(component, deathMessage, messageString);
            if (messageString.startsWith("<")) {
                senderName = extractSenderNameNormalChat(messageString);
            } else if (deathMessage.isEmpty()) {
                final String[] split = messageString.split(" ");
                if (split.length > 2) {
                    if (split[1].startsWith("whispers")) {
                        senderName = extractSenderNameReceivedWhisper(split);
                        whisperTarget = CONFIG.authentication.username;
                    } else if (messageString.startsWith("to ")) {
                        senderName = CONFIG.authentication.username;
                        whisperTarget = extractReceiverNameSentWhisper(split);
                    }
                }
            }
            var sender = Optional.ofNullable(senderName).flatMap(t -> CACHE.getTabListCache().getFromName(t));
            var playerWhisperTarget = Optional.ofNullable(whisperTarget).flatMap(t -> CACHE.getTabListCache().getFromName(t));
            if (Proxy.getInstance().isOn2b2t()
                && "Reconnecting to server 2b2t.".equals(messageString)
                && NamedTextColor.GOLD.equals(component.style().color())) {
                CLIENT_LOG.info("Queue Skip Detected");
                EVENT_BUS.postAsync(QueueSkipEvent.INSTANCE);
            }

            if (sender.isPresent() && deathMessage.isEmpty() && playerWhisperTarget.isEmpty()) {
                EVENT_BUS.postAsync(new PublicChatEvent(sender.get(), component, messageString));
            } else if (sender.isPresent() && deathMessage.isEmpty() && playerWhisperTarget.isPresent()) {
                var outgoing = sender.get().getName().equalsIgnoreCase(CONFIG.authentication.username);
                EVENT_BUS.postAsync(new WhisperChatEvent(outgoing, sender.get(), playerWhisperTarget.get(), component, messageString));
            } else if (sender.isEmpty() && deathMessage.isPresent() && playerWhisperTarget.isEmpty()) {
                EVENT_BUS.postAsync(new DeathMessageChatEvent(deathMessage.get(), component, messageString));
            } else {
                EVENT_BUS.postAsync(new SystemChatEvent(component, messageString));
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Caught exception in ChatHandler. Packet: {}", packet, e);
        }
        return true;
    }

    private Optional<DeathMessageParseResult> parseDeathMessage2b2t(final Component component, Optional<DeathMessageParseResult> deathMessage, final String messageString) {
        if (component.children().stream().anyMatch(child -> nonNull(child.color())
            && Objects.equals(child.color(), DEATH_MSG_COLOR_2b2t))) { // death message color on 2b
            deathMessage = deathMessagesHelper.parse(component, messageString);
            if (deathMessage.isPresent()) {
                if (deathMessage.get().victim().equals(CACHE.getProfileCache().getProfile().getName())) {
                    EVENT_BUS.postAsync(new SelfDeathMessageEvent(messageString));
                }
            } else {
                CLIENT_LOG.warn("Failed to parse death message: {}", messageString);
            }
        }
        return deathMessage;
    }

    private String extractSenderNameNormalChat(final String message) {
        return message.substring(message.indexOf("<") + 1, message.indexOf(">"));
    }

    private String extractSenderNameReceivedWhisper(final String[] messageSplit) {
        return messageSplit[0].trim();
    }

    private String extractReceiverNameSentWhisper(final String[] messageSplit) {
        return messageSplit[1].replace(":", "");
    }
}
