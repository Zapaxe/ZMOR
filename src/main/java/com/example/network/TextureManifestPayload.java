package com.example.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record TextureManifestPayload(UUID playerUuid, Map<String, String> textures) implements CustomPacketPayload {
    public static final Type<TextureManifestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("zmor", "texture_manifest"));

    public static final StreamCodec<FriendlyByteBuf, TextureManifestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.playerUuid());
                buf.writeVarInt(payload.textures().size());
                for (Map.Entry<String, String> entry : payload.textures().entrySet()) {
                    buf.writeUtf(entry.getKey());
                    buf.writeUtf(entry.getValue());
                }
            },
            buf -> {
                UUID uuid = buf.readUUID();
                int size = buf.readVarInt();
                Map<String, String> textures = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    String key = buf.readUtf();
                    String hash = buf.readUtf();
                    textures.put(key, hash);
                }
                return new TextureManifestPayload(uuid, textures);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
