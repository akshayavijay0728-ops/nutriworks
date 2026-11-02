package com.nutriworks.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * QR Code Generator using the ZXing library.
 * Generates high-contrast, mathematically exact QR codes for the phone bridge.
 */
public final class QrCodeGenerator {

    private QrCodeGenerator() {
    }

    /**
     * Generates a ZXing BufferedImage for a target content URL.
     */
    public static BufferedImage generateQrBufferedImage(String text, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2); // 2 modules quiet zone

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Generates a JavaFX Image for direct rendering in an ImageView.
     */
    public static Image generateQrFxImage(String text, int width, int height) {
        try {
            BufferedImage bufferedImage = generateQrBufferedImage(text, width, height);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates raw PNG bytes of the QR code (for server endpoint).
     */
    public static byte[] generateQrPngBytes(String text, int width, int height) throws WriterException, IOException {
        BufferedImage img = generateQrBufferedImage(text, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
