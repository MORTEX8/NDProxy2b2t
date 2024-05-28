package com.zenith.feature.world.blockdata;

import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public record Block(int id, String name, boolean isBlock, int minStateId, int maxStateId, int mapColorId, @Nullable BlockEntityType blockEntityType) { }
