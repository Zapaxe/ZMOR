package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

public class LocalTextureExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTextureExtractor.class);

    public static byte[] getActiveTextureBytes(Identifier textureId) {
        if (textureId == null) return null;
        
        try {
            Optional<Resource> resourceOpt = Minecraft.getInstance().getResourceManager().getResource(textureId);
            if (resourceOpt.isPresent()) {
                try (InputStream stream = resourceOpt.get().open()) {
                    return stream.readAllBytes();
                }
            }
        } catch (Exception e) {
            LOGGER.error("[zmor] Failed to read bytes for texture {}", textureId, e);
        }
        return null;
    }

    public static String computeHash(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
