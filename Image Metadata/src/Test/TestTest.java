package Test;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import common.ByteValueConverter;

class TestTest
{
    @Test
    public void testToFloatArrayLittleEndian()
    {
        byte[] data = {
                0x00, 0x00, (byte) 0x80, 0x3F, // 1.0f
                0x00, 0x00, 0x00, 0x40, // 2.0f
                0x00, 0x00, 0x40, 0x40 // 3.0f
        };
        float[] floats = ByteValueConverter.toFloatArray(data, ByteOrder.LITTLE_ENDIAN);
        assertEqualsWithDelta(1.0f, floats[0], 0.00001f);
        assertEqualsWithDelta(2.0f, floats[1], 0.00001f);
        assertEqualsWithDelta(3.0f, floats[2], 0.00001f);
    }

    @Test
    public void testToFloatArrayBigEndian()
    {
        byte[] data = {
                0x3F, (byte) 0x80, 0x00, 0x00, // 1.0f
                0x40, 0x00, 0x00, 0x00, // 2.0f
                0x40, 0x40, 0x00, 0x00 // 3.0f
        };
        float[] floats = ByteValueConverter.toFloatArray(data, ByteOrder.BIG_ENDIAN);
        assertEqualsWithDelta(1.0f, floats[0], 0.00001f);
        assertEqualsWithDelta(2.0f, floats[1], 0.00001f);
        assertEqualsWithDelta(3.0f, floats[2], 0.00001f);
    }

    @Test
    public void testToDoubleArrayLittleEndian()
    {
        byte[] data = {
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF0, 0x3F, // 1.0
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40 // 2.0
        };
        double[] doubles = ByteValueConverter.toDoubleArray(data, ByteOrder.LITTLE_ENDIAN);
        assertEquals(1.0, doubles[0], 0.0000001);
        assertEquals(2.0, doubles[1], 0.0000001);
    }

    @Test
    public void testToDoubleArrayBigEndian()
    {
        byte[] data = {
                0x3F, (byte) 0xF0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // 1.0
                0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 // 2.0
        };
        double[] doubles = ByteValueConverter.toDoubleArray(data, ByteOrder.BIG_ENDIAN);
        assertEquals(1.0, doubles[0], 0.0000001);
        assertEquals(2.0, doubles[1], 0.0000001);
    }

    public static void assertEqualsWithDelta(float expected, float actual, float delta)
    {
        assertEquals(expected, actual, delta);
    }
}
