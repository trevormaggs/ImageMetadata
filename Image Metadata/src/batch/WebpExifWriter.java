package batch;

import org.apache.commons.imaging.Imaging;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class WebpExifWriter
{
    // Public entry point
    public static void updateWebpSoftware(File src, File dst, String software) throws IOException
    {
        byte[] srcBytes = readAll(src);

        // 1) Validate RIFF/WEBP header
        if (srcBytes.length < 12 || !ascii(srcBytes, 0, 4).equals("RIFF") || !ascii(srcBytes, 8, 4).equals("WEBP"))
        {
            throw new IOException("Not a RIFF/WEBP file.");
        }

        // Parse chunks
        List<Chunk> chunks = parseChunks(srcBytes, 12);

        // 2) Build EXIF payload: "Exif\0\0" + minimal TIFF containing Software tag
        byte[] exifPayload = buildExifPayloadForSoftware(software);

        // 3) Ensure VP8X exists and has EXIF flag set (0x08)
        boolean hasVp8x = chunks.stream().anyMatch(c -> c.fourcc.equals("VP8X"));
        
        if (!hasVp8x)
        {
            // Create VP8X with width/height from the decoded image and EXIF flag on
            BufferedImage img;
        
            try
            {
                img = Imaging.getBufferedImage(src);
            }
            
            catch (Exception e)
            {
                // Fallback: if Imaging not available, refuse cleanly
                throw new IOException("Cannot read image size for VP8X creation.", e);
            }
            int w = img.getWidth();
            int h = img.getHeight();
            
            chunks.add(0, makeVp8xChunk(/* icc */false, /* alpha */img.getColorModel().hasAlpha(), /* exif */true, /* xmp */false, /* anim */false, w, h));
        }
        else
        {
            // Set EXIF flag in existing VP8X
            for (Chunk c : chunks)
            {
                if (c.fourcc.equals("VP8X"))
                {
                    // Flags are at byte 0 of the 10-byte payload; EXIF mask = 0x08
                    c.data[0] = (byte) (c.data[0] | 0x08);
                    break;
                }
            }
        }

        // 4) Remove any existing EXIF chunk and add the new one
        chunks.removeIf(c -> c.fourcc.equals("EXIF"));
        Chunk exifChunk = new Chunk("EXIF", exifPayload);

        // Spec suggests metadata after image data; appending is widely accepted.
        // If you want to place it right after image data, insert after first "VP8 " or "VP8L "
        // chunk.
        int insertAt = chunks.size();
        
        for (int i = 0; i < chunks.size(); i++)
        {
            String id = chunks.get(i).fourcc;
            if (id.equals("VP8 ") || id.equals("VP8L"))
            {
                insertAt = Math.min(insertAt, i + 1);
            }
        }
        
        chunks.add(insertAt, exifChunk);

        // 5) Rebuild RIFF with fixed sizes and even padding
        byte[] rebuilt = buildRiff(chunks);
        
        try (FileOutputStream fos = new FileOutputStream(dst))
        {
            fos.write(rebuilt);
        }
    }

    /* ------------------------------ helpers ------------------------------ */

    private static final class Chunk
    {
        final String fourcc;
        final byte[] data;

        Chunk(String fourcc, byte[] data)
        {
            this.fourcc = fourcc;
            this.data = data;
        }
    }

    private static List<Chunk> parseChunks(byte[] data, int off) throws IOException
    {
        List<Chunk> out = new ArrayList<>();
        
        while (off + 8 <= data.length)
        {
            String id = ascii(data, off, 4);
            int size = le32(data, off + 4);
            int start = off + 8;
            
			if (start + size > data.length) break;
            
			byte[] payload = new byte[size];
			
            System.arraycopy(data, start, payload, 0, size);
            out.add(new Chunk(id, payload));
            off = start + size + (size & 1); // pad to even
        }
        
        return out;
    }

    private static byte[] buildRiff(List<Chunk> chunks) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "RIFF");
        // Placeholder for RIFF size
        writeLE32(out, 0);
        writeAscii(out, "WEBP");

        int payloadSize = 4; // "WEBP"
        
        for (Chunk c : chunks)
        {
            writeAscii(out, c.fourcc);
            writeLE32(out, c.data.length);
            out.write(c.data);
        
            if ((c.data.length & 1) == 1) out.write(0); // pad
            
            payloadSize += 8 + c.data.length + (c.data.length & 1);
        }

        // RIFF size is payloadSize (after "RIFF" + size field), i.e. total - 8
        byte[] riff = out.toByteArray();
        int riffSize = payloadSize;
        
        putLE32(riff, 4, riffSize);

        return riff;
    }

    private static Chunk makeVp8xChunk(boolean icc, boolean alpha, boolean exif, boolean xmp, boolean anim, int width, int height) throws IOException
    {
        // VP8X payload is 10 bytes: 1 flag byte + 3 reserved + 3 bytes (W-1) + 3 bytes (H-1)
        // Flag bits (MSB..LSB): 00 ICC Alpha EXIF XMP ANIM 0 (EXIF mask = 0x08)
        int flags = 0;
        if (icc) flags |= 0x20;
        if (alpha) flags |= 0x10;
        if (exif) flags |= 0x08;
        if (xmp) flags |= 0x04;
        if (anim) flags |= 0x02;

        int wMinus1 = Math.max(0, width - 1);
        int hMinus1 = Math.max(0, height - 1);

        ByteArrayOutputStream p = new ByteArrayOutputStream(10);
        p.write(flags);
        p.write(new byte[]{0, 0, 0}); // reserved
        writeLE24(p, wMinus1);
        writeLE24(p, hMinus1);

        return new Chunk("VP8X", p.toByteArray());
    }

    // Build minimal EXIF payload with only TIFF Software tag (0x0131)
    private static byte[] buildExifPayloadForSoftware(String software) throws IOException
    {
        ByteArrayOutputStream exif = new ByteArrayOutputStream();

        // "Exif\0\0"
        exif.write(new byte[]{'E', 'x', 'i', 'f', 0, 0});

        // TIFF header (little-endian "II", 42, IFD0 offset = 8)
        ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        tiff.write(new byte[]{'I', 'I'}); // byte order
        writeLE16(tiff, 42); // magic
        writeLE32(tiff, 8); // offset to IFD0

        // IFD0 with 1 entry (Software)
        byte[] s = (software == null ? "" : software).getBytes(StandardCharsets.US_ASCII);
        int count = s.length + 1; // include trailing NUL
        int ifd0Start = 8;
        int numEntries = 1;
        int dataOffset = ifd0Start + 2 + (12 * numEntries) + 4; // after entries + nextIFD offset

        writeLE16(tiff, numEntries);

        // Entry: Tag=0x0131, Type=ASCII(2), Count, ValueOffset -> points to the string
        writeLE16(tiff, 0x0131);
        writeLE16(tiff, 2);
        writeLE32(tiff, count);
        writeLE32(tiff, dataOffset);

        // next IFD = 0
        writeLE32(tiff, 0);

        // string data (NUL terminated)
        tiff.write(s);
        tiff.write(0);

        exif.write(tiff.toByteArray());

        return exif.toByteArray();
    }

    /* ---------------- little-endian + io utils ---------------- */

    private static String ascii(byte[] a, int off, int len)
    {
        return new String(a, off, len, StandardCharsets.US_ASCII);
    }

    private static byte[] readAll(File f) throws IOException
    {
        try (InputStream is = new FileInputStream(f))
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;

            while ((n = is.read(buf)) != -1)
                out.write(buf, 0, n);

            return out.toByteArray();
        }
    }

    private static void writeAscii(OutputStream os, String s) throws IOException
    {
        os.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeLE16(OutputStream os, int v) throws IOException
    {
        os.write(v & 0xFF);
        os.write((v >>> 8) & 0xFF);
    }

    private static void writeLE24(OutputStream os, int v) throws IOException
    {
        os.write(v & 0xFF);
        os.write((v >>> 8) & 0xFF);
        os.write((v >>> 16) & 0xFF);
    }

    private static void writeLE32(OutputStream os, int v) throws IOException
    {
        os.write(v & 0xFF);
        os.write((v >>> 8) & 0xFF);
        os.write((v >>> 16) & 0xFF);
        os.write((v >>> 24) & 0xFF);
    }

    private static void putLE32(byte[] a, int off, int v)
    {
        a[off] = (byte) (v);
        a[off + 1] = (byte) (v >>> 8);
        a[off + 2] = (byte) (v >>> 16);
        a[off + 3] = (byte) (v >>> 24);
    }

    private static int le32(byte[] a, int off)
    {
        return (a[off] & 0xFF) | ((a[off + 1] & 0xFF) << 8) | ((a[off + 2] & 0xFF) << 16) | ((a[off + 3] & 0xFF) << 24);
    }
}