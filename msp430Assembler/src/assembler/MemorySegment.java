package assembler;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Bellek üzerinde bir segmenti (örneğin .text, .data, .bss) temsil eder.
 */
public class MemorySegment {

    private final String name;     // Segment adı (örneğin .text)
    private final int origin;      // Başlangıç adresi
    private final int length;      // Uzunluk (byte cinsinden)
    private final byte[] content;  // Segmentin verisi (bellek içeriği)

    public MemorySegment(String name, int origin, int length) {
        this.name = name;
        this.origin = origin;
        this.length = length;
        this.content = new byte[length];
        Arrays.fill(this.content, (byte) 0); // Başlangıçta tüm alan 0
    }

    public String getName() {
        return name;
    }

    public int getOrigin() {
        return origin;
    }

    public int getLength() {
        return length;
    }

    public byte[] getContent() {
        return content;
    }

    public int getAbsoluteAddress(int offset) {
        return origin + offset;
    }

    public void writeByte(int offset, byte value) {
        if (offset < 0 || offset >= length) {
            throw new IndexOutOfBoundsException("Segment sınırlarının dışında yazma: " + offset);
        }
        content[offset] = value;
    }

    public void writeWord(int offset, int value) {
        if (offset < 0 || offset + 1 >= length) {
            throw new IndexOutOfBoundsException("Segment sınırlarının dışında word yazma: " + offset);
        }
        content[offset]     = (byte) (value & 0xFF);
        content[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    public byte readByte(int offset) {
        if (offset < 0 || offset >= length) {
            throw new IndexOutOfBoundsException("Segment sınırlarının dışında okuma: " + offset);
        }
        return content[offset];
    }

    public int readWord(int offset) {
        if (offset < 0 || offset + 1 >= length) {
            throw new IndexOutOfBoundsException("Segment sınırlarının dışında word okuma: " + offset);
        }
        return ((content[offset + 1] & 0xFF) << 8) | (content[offset] & 0xFF);
    }

    @Override
    public String toString() {
        return String.format("MemorySegment{origin=0x%04X, size=%d}", origin, content.length);
    }

    public byte[] getBytes() {
        return content;
    }

    public String toHexDump() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i += 16) {
            // Adres bölümü
            sb.append(String.format("0x%04X: ", origin + i));

            // Hex bölümü
            for (int j = 0; j < 16; j++) {
                if (i + j < length) {
                    sb.append(String.format("%02X ", content[i + j]));
                } else {
                    sb.append("   "); // 3 boşluk
                }
            }
            sb.append(" ");

            // ASCII bölümü
            for (int j = 0; j < 16; j++) {
                if (i + j < length) {
                    char c = (char) content[i + j];
                    if (c >= 32 && c < 127) {
                        sb.append(c);
                    } else {
                        sb.append('.');
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public List<int[]> getNonZeroRanges(int chunkSize) {
        List<int[]> ranges = new ArrayList<>();
        int i = 0;
        while (i < content.length) {
            // Veri içeren alanı bul
            while (i < content.length && content[i] == 0) i++;
            int start = i;
            while (i < content.length && (i - start) < chunkSize) {
                if (content[i] != 0) {
                    i++;
                } else {
                    // Eğer 0 tekrar başlarsa, aralığı bitir
                    break;
                }
            }
            if (start < i) {
                ranges.add(new int[]{start, i});
            }
        }
        return ranges;
    }
}
