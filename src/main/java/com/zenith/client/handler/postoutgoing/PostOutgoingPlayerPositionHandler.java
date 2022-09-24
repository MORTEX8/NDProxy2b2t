package com.zenith.client.handler.postoutgoing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.zenith.client.ClientSession;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;

import static com.zenith.util.Constants.CACHE;

public class PostOutgoingPlayerPositionHandler implements HandlerRegistry.PostOutgoingHandler<ClientPlayerPositionPacket, ClientSession> {

    @Override
    public void accept(ClientPlayerPositionPacket packet, ClientSession session) {
        CACHE.getPlayerCache()
                .setX(packet.getX())
                .setY(packet.getY())
                .setZ(packet.getZ());
        SpectatorHelper.syncPlayerPositionWithSpectators();
    }

    @Override
    public Class<ClientPlayerPositionPacket> getPacketClass() {
        return ClientPlayerPositionPacket.class;
    }
}