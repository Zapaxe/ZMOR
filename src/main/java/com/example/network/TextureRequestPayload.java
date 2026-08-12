package com.example.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record TextureRequestPayload(UUID requesterUuid, UUID targetPlayerUuid, String texturePath) implements CustomPacketPayload {
    public static final Type<TextureRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("zmor", "texture_request"));

    public static final StreamCodec<FriendlyByteBuf, TextureRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.requesterUuid());
                buf.writeUUID(payload.targetPlayerUuid());
                buf.writeUtf(payload.texturePath());
            },
            buf -> new TextureRequestPayload(
                    buf.readUUID(),
                    buf.readUUID(),
                    buf.readUtf()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
