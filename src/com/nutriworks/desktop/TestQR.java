package com.nutriworks.desktop;

import javax.imageio.ImageIO;
import java.io.File;
import java.awt.image.BufferedImage;

public class TestQR {
    public static void main(String[] args) throws Exception {
        BufferedImage img = PureQrEncoder.generateQrImage("http://192.168.1.5:8080", 10);
        File out = new File("C:/Users/Lenovo/.gemini/antigravity/scratch/NutriWorks/qr_code.png");
        ImageIO.write(img, "png", out);
        System.out.println("QR saved to " + out.getAbsolutePath() + " (" + img.getWidth() + "x" + img.getHeight() + ")");
    }
}
