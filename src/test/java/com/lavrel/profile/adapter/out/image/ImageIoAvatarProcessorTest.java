package com.lavrel.profile.adapter.out.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavrel.profile.application.port.out.AvatarImagePort;
import com.lavrel.profile.application.port.out.AvatarImagePort.PreparedAvatar;
import com.lavrel.profile.domain.InvalidAvatarException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageIoAvatarProcessorTest {

    private final ImageIoAvatarProcessor processor = new ImageIoAvatarProcessor();

    private static byte[] image(int width, int height, String format, int type) throws IOException {
        BufferedImage img = new BufferedImage(width, height, type);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private static BufferedImage read(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void jpegBecomesASquareJpeg() throws IOException {
        PreparedAvatar result = processor.prepare(image(400, 300, "jpeg", BufferedImage.TYPE_INT_RGB));
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        BufferedImage out = read(result.bytes());
        assertThat(out.getWidth()).isEqualTo(AvatarImagePort.SIZE);
        assertThat(out.getHeight()).isEqualTo(AvatarImagePort.SIZE);
    }

    @Test
    void pngKeepsItsTransparency() throws IOException {
        PreparedAvatar result = processor.prepare(image(300, 300, "png", BufferedImage.TYPE_INT_ARGB));
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(read(result.bytes()).getColorModel().hasAlpha()).isTrue();
    }

    @Test
    void gifIsKeptExactlyAsUploaded() throws IOException {
        byte[] gif = image(128, 128, "gif", BufferedImage.TYPE_BYTE_INDEXED);
        PreparedAvatar result = processor.prepare(gif);
        assertThat(result.contentType()).isEqualTo("image/gif");
        assertThat(result.bytes()).isEqualTo(gif);
    }

    @Test
    void oversizedGifsAreRefused() throws IOException {
        byte[] gif = image(600, 600, "gif", BufferedImage.TYPE_BYTE_INDEXED);
        assertThatThrownBy(() -> processor.prepare(gif)).isInstanceOf(InvalidAvatarException.class);
    }

    @Test
    void theTypeComesFromTheContentNotTheName() {
        byte[] notAnImage = "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> processor.prepare(notAnImage)).isInstanceOf(InvalidAvatarException.class);
    }

    @Test
    void tinyPicturesAreRefused() throws IOException {
        byte[] tiny = image(10, 10, "png", BufferedImage.TYPE_INT_ARGB);
        assertThatThrownBy(() -> processor.prepare(tiny)).isInstanceOf(InvalidAvatarException.class);
    }
}
