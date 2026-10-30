package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.exception.InvalidImageException;

/** Detects the photo format from its first bytes; the declared content type is not trusted. */
final class ImageFormat {

    static final int MAX_BYTES = 5 * 1024 * 1024;

    private ImageFormat() {}

    static String extensionOf(byte[] b) {
        if (b == null || b.length < 12 || b.length > MAX_BYTES) throw new InvalidImageException();
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        throw new InvalidImageException();
    }
}
