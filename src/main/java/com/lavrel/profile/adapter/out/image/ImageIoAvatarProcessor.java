package com.lavrel.profile.adapter.out.image;

import com.lavrel.profile.application.port.out.AvatarImagePort;
import com.lavrel.profile.domain.InvalidAvatarException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/**
 * Uses only the JDK (javax.imageio). The type is decided by the file's first bytes, never by its name. Dimensions are
 * read from the header before decoding, so a small file claiming a huge image ("decompression bomb") is refused
 * before it can fill the memory.
 */
@Component
public class ImageIoAvatarProcessor implements AvatarImagePort {

    static final int MAX_STATIC_SIDE = 4096;
    static final int MAX_GIF_BYTES = 1024 * 1024;
    static final int MAX_GIF_SIDE = 512;
    static final int MAX_GIF_FRAMES = 300;
    static final int MIN_SIDE = 32;

    enum Type { JPEG, PNG, GIF }

    @Override
    public PreparedAvatar prepare(byte[] upload) {
        Type type = detect(upload);
        try {
            return type == Type.GIF ? gif(upload) : still(upload, type);
        } catch (IOException e) {
            throw new InvalidAvatarException("The picture could not be read");
        }
    }

    static Type detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Type.JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Type.PNG;
        }
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return Type.GIF;
        }
        throw new InvalidAvatarException("Only JPG, PNG or GIF pictures are accepted");
    }

    private PreparedAvatar gif(byte[] upload) throws IOException {
        if (upload.length > MAX_GIF_BYTES) {
            throw new InvalidAvatarException("A GIF can be at most 1 MB");
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            ImageReader reader = reader(in, "gif");
            try {
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_GIF_SIDE || height > MAX_GIF_SIDE) {
                    throw new InvalidAvatarException("A GIF can be at most " + MAX_GIF_SIDE + "x" + MAX_GIF_SIDE);
                }
                checkMinimum(width, height);
                if (reader.getNumImages(true) > MAX_GIF_FRAMES) {
                    throw new InvalidAvatarException("A GIF can have at most " + MAX_GIF_FRAMES + " frames");
                }
            } finally {
                reader.dispose();
            }
        }
        return new PreparedAvatar(upload, "image/gif", "gif");
    }

    private PreparedAvatar still(byte[] upload, Type type) throws IOException {
        BufferedImage image;
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            ImageReader reader = reader(in, type == Type.PNG ? "png" : "jpeg");
            try {
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_STATIC_SIDE || height > MAX_STATIC_SIDE) {
                    throw new InvalidAvatarException("A picture can be at most " + MAX_STATIC_SIDE + " pixels wide");
                }
                checkMinimum(width, height);
                image = reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        boolean png = type == Type.PNG;
        BufferedImage square = squareAndResize(image, png);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(square, png ? "png" : "jpeg", out)) {
            throw new InvalidAvatarException("The picture could not be converted");
        }
        return png ? new PreparedAvatar(out.toByteArray(), "image/png", "png")
                : new PreparedAvatar(out.toByteArray(), "image/jpeg", "jpg");
    }

    /** Centre crop to a square, then scale to SIZE; PNG keeps its transparency, JPEG has none. */
    static BufferedImage squareAndResize(BufferedImage image, boolean keepAlpha) {
        int side = Math.min(image.getWidth(), image.getHeight());
        int x = (image.getWidth() - side) / 2;
        int y = (image.getHeight() - side) / 2;
        BufferedImage result = new BufferedImage(SIZE, SIZE,
                keepAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(image, 0, 0, SIZE, SIZE, x, y, x + side, y + side, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static void checkMinimum(int width, int height) {
        if (width < MIN_SIDE || height < MIN_SIDE) {
            throw new InvalidAvatarException("A picture must be at least " + MIN_SIDE + "x" + MIN_SIDE);
        }
    }

    private static ImageReader reader(ImageInputStream in, String format) {
        if (in == null) {
            throw new InvalidAvatarException("The picture could not be read");
        }
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
        if (!readers.hasNext()) {
            throw new InvalidAvatarException("The picture could not be read");
        }
        ImageReader reader = readers.next();
        reader.setInput(in, false, true);
        return reader;
    }
}
