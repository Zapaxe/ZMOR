package com.example.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record TextureResponsePayload(UUID targetPlayerUuid, UUID ownerUuid, String texturePath, byte[] pngData) implements CustomPacketPayload {
    public static final Type<TextureResponsePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("zmor", "texture_response"));

    // Max 1MB payload per texture PNG
    private static final int MAX_PNG_BYTES = 1024 * 1024;

    public static final StreamCodec<FriendlyByteBuf, TextureResponsePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.targetPlayerUuid());
                buf.writeUUID(payload.ownerUuid());
                buf.writeUtf(payload.texturePath());
                buf.writeByteArray(payload.pngData());
            },
            buf -> new TextureResponsePayload(
                    buf.readUUID(),
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readByteArray(MAX_PNG_BYTES)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
