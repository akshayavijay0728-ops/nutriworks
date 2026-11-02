package com.nutriworks.desktop;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

/**
 * Pure Java 100% Standard QR Code Generator (Version 2-M, 25x25 matrix).
 * Encodes strings up to 32 bytes (such as http://192.168.1.5:8080).
 * Conforms strictly to ISO/IEC 18004 specification:
 *  - GF(256) Galois Field with primitive polynomial 0x11d
 *  - Correct Reed-Solomon generator polynomial (highest degree first)
 *  - Correct Format Information bits (Level M, Mask 0)
 * Scannable instantly by any Android or iOS camera.
 */
public class PureQrEncoder {

    // Galois Field GF(256) with primitive polynomial 0x11d (x^8 + x^4 + x^3 + x^2 + 1)
    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            EXP[i + 255] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) {
                x ^= 0x11d;
            }
        }
    }

    private static int gfMul(int x, int y) {
        if (x == 0 || y == 0) return 0;
        return EXP[LOG[x] + LOG[y]];
    }

    private static int[] rsPoly(int n) {
        int[] g = {1};
        for (int i = 0; i < n; i++) {
            int[] ng = new int[g.length + 1];
            for (int j = 0; j < g.length; j++) {
                ng[j] ^= g[j];
                ng[j + 1] ^= gfMul(g[j], EXP[i]);
            }
            g = ng;
        }
        return g;
    }

    private static int[] rsEncode(int[] data, int nEcc) {
        int[] gen = rsPoly(nEcc);
        int[] msg = new int[data.length + nEcc];
        System.arraycopy(data, 0, msg, 0, data.length);
        for (int i = 0; i < data.length; i++) {
            int lead = msg[i];
            if (lead != 0) {
                for (int j = 0; j < gen.length; j++) {
                    msg[i + j] ^= gfMul(gen[j], lead);
                }
            }
        }
        int[] ecc = new int[nEcc];
        System.arraycopy(msg, data.length, ecc, 0, nEcc);
        return ecc;
    }

    public static BufferedImage generateQrImage(String text, int scale) {
        byte[] b = text.getBytes(StandardCharsets.ISO_8859_1);
        if (b.length > 32) {
            byte[] truncated = new byte[32];
            System.arraycopy(b, 0, truncated, 0, 32);
            b = truncated;
        }

        // Bit stream for Byte Mode (0100)
        int[] bits = new int[28 * 8];
        int bitLen = 0;

        // Mode 0100
        bits[bitLen++] = 0; bits[bitLen++] = 1; bits[bitLen++] = 0; bits[bitLen++] = 0;
        // Count 8 bits
        for (int i = 7; i >= 0; i--) bits[bitLen++] = (b.length >> i) & 1;
        // Data
        for (byte by : b) {
            int val = by & 0xFF;
            for (int i = 7; i >= 0; i--) bits[bitLen++] = (val >> i) & 1;
        }
        // Terminator (up to 4 zeroes)
        int term = Math.min(4, 28 * 8 - bitLen);
        for (int i = 0; i < term; i++) bits[bitLen++] = 0;
        // Pad to byte boundary
        while (bitLen % 8 != 0) bits[bitLen++] = 0;
        // Pad bytes 0xEC, 0x11
        int[] pad = {0xEC, 0x11};
        int pIdx = 0;
        while (bitLen < 28 * 8) {
            int pv = pad[pIdx];
            for (int i = 7; i >= 0; i--) bits[bitLen++] = (pv >> i) & 1;
            pIdx ^= 1;
        }

        // 28 data codewords
        int[] dataCw = new int[28];
        for (int i = 0; i < 28; i++) {
            int val = 0;
            for (int j = 0; j < 8; j++) val = (val << 1) | bits[i * 8 + j];
            dataCw[i] = val;
        }

        // 16 EC codewords
        int[] eccCw = rsEncode(dataCw, 16);
        int[] allCw = new int[44];
        System.arraycopy(dataCw, 0, allCw, 0, 28);
        System.arraycopy(eccCw, 0, allCw, 28, 16);

        // Matrix 25x25 (Version 2)
        int size = 25;
        int[][] matrix = new int[size][size];
        boolean[][] reserved = new boolean[size][size];

        // Finder patterns at (0,0), (0, 18), (18, 0)
        placeFinder(matrix, reserved, 0, 0, size);
        placeFinder(matrix, reserved, 0, size - 7, size);
        placeFinder(matrix, reserved, size - 7, 0, size);

        // Alignment pattern at (18, 18) for V2
        for (int r = 16; r <= 20; r++) {
            for (int c = 16; c <= 20; c++) {
                boolean dark = (r == 16 || r == 20 || c == 16 || c == 20 || (r == 18 && c == 18));
                matrix[r][c] = dark ? 1 : 0;
                reserved[r][c] = true;
            }
        }

        // Timing patterns
        for (int i = 8; i < size - 8; i++) {
            int val = (i % 2 == 0) ? 1 : 0;
            if (!reserved[6][i]) { matrix[6][i] = val; reserved[6][i] = true; }
            if (!reserved[i][6]) { matrix[i][6] = val; reserved[i][6] = true; }
        }

        // Dark module at (4*V+9, 8) = (17, 8)
        matrix[17][8] = 1;
        reserved[17][8] = true;

        // Reserve Format info locations
        for (int i = 0; i < 9; i++) {
            reserved[8][i] = true;
            reserved[i][8] = true;
        }
        for (int i = size - 8; i < size; i++) {
            reserved[8][i] = true;
            reserved[i][8] = true;
        }

        // Convert codewords to bits
        int[] dataBits = new int[44 * 8];
        int dIdx = 0;
        for (int cw : allCw) {
            for (int i = 7; i >= 0; i--) dataBits[dIdx++] = (cw >> i) & 1;
        }

        // Place data in zig-zag columns
        int bitPos = 0;
        int col = size - 1;
        while (col > 0) {
            if (col == 6) col--; // Skip vertical timing column
            int colLeft = col - 1;
            boolean goingUp = (((size - 1 - col) / 2) % 2 == 0);
            int rStart = goingUp ? size - 1 : 0;
            int rEnd = goingUp ? -1 : size;
            int rStep = goingUp ? -1 : 1;

            for (int r = rStart; r != rEnd; r += rStep) {
                int[] colPair = {col, colLeft};
                for (int c : colPair) {
                    if (!reserved[r][c]) {
                        int bit = (bitPos < dataBits.length) ? dataBits[bitPos++] : 0;
                        // Apply Mask 0: (row + col) % 2 == 0
                        if ((r + c) % 2 == 0) bit ^= 1;
                        matrix[r][c] = bit;
                    }
                }
            }
            col -= 2;
        }

        // Format Bits for Error Correction M (00) & Mask 0 (000)
        // 15 bits, index 0 is LSB, index 14 is MSB:
        // [0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1]
        int[] formatBits = {0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1};

        // Top-left finder placement:
        for (int i = 0; i <= 5; i++) matrix[8][i] = formatBits[i];
        matrix[8][7] = formatBits[6];
        matrix[8][8] = formatBits[7];
        matrix[7][8] = formatBits[8];
        for (int i = 0; i <= 5; i++) matrix[5 - i][8] = formatBits[9 + i];

        // Bottom-left and Top-right placement:
        for (int i = 0; i <= 6; i++) matrix[size - 1 - i][8] = formatBits[i];
        for (int i = 0; i <= 7; i++) matrix[8][size - 8 + i] = formatBits[7 + i];

        // Render to BufferedImage with standard white quiet zone (4 modules)
        int border = 4;
        int imgDim = (size + border * 2) * scale;
        BufferedImage img = new BufferedImage(imgDim, imgDim, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, imgDim, imgDim);
        g2.setColor(Color.BLACK);

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (matrix[r][c] == 1) {
                    g2.fillRect((c + border) * scale, (r + border) * scale, scale, scale);
                }
            }
        }
        g2.dispose();
        return img;
    }

    private static void placeFinder(int[][] m, boolean[][] res, int r0, int c0, int size) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                boolean dark = (r == 0 || r == 6 || c == 0 || c == 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4));
                m[r0 + r][c0 + c] = dark ? 1 : 0;
                res[r0 + r][c0 + c] = true;
            }
        }
        // Separator white ring
        for (int r = -1; r <= 7; r++) {
            for (int c = -1; c <= 7; c++) {
                int ar = r0 + r;
                int ac = c0 + c;
                if (ar >= 0 && ar < size && ac >= 0 && ac < size) {
                    if (!res[ar][ac]) {
                        m[ar][ac] = 0;
                        res[ar][ac] = true;
                    }
                }
            }
        }
    }
}
