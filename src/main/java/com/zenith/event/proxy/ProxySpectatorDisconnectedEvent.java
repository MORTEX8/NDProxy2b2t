package com.zenith.event.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;

public record ProxySpectatorDisconnectedEvent(GameProfile clientGameProfile) { }
