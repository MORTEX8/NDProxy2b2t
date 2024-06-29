package com.zenith.network.server.handler.player.postoutgoing;

import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerConnection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundStartConfigurationPacket;

public class StartConfigurationPostOutgoingHandler implements PostOutgoingPacketHandler<ClientboundStartConfigurationPacket, ServerConnection> {
    @Override
    public void accept(final ClientboundStartConfigurationPacket packet, final ServerConnection session) {
        if (session.isConfigured()) {
            // the player is already logged into zenith, this is the dest server doing the configuration protocol switch
            session.setAwaitingProtocolSwitch(true);
        }
    }
}
