package assembler;

import java.util.Map;

public class Loader {

    private final byte[] memoryImage = new byte[65536]; // 64KB memory for MSP430

    public Loader(Map<String, MemorySegment> segments) {
        // Initialize memory with a default value, e.g., 0xFF, which often represents erased flash
        for (int i = 0; i < memoryImage.length; i++) {
            memoryImage[i] = (byte) 0xFF;
        }

        // Load each segment into the memory image
        for (MemorySegment segment : segments.values()) {
            int origin = segment.getOrigin();
            byte[] data = segment.getContent();
            // Copy segment data into the main memory image at the correct origin
            if (origin + data.length <= memoryImage.length) {
                System.arraycopy(data, 0, memoryImage, origin, data.length);
            } else {
                System.err.println("Uyarı: '" + segment.getName() + "' segmenti, başlangıç adresi 0x" + Integer.toHexString(origin) +
                                   " ve uzunluğu " + data.length + " olan bellek sınırlarını aşıyor.");
            }
        }
    }

    public byte[] getMemoryImage() {
        return memoryImage;
    }
} 