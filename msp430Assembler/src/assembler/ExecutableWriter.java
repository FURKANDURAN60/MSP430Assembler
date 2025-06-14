package assembler;

import java.io.*;
import java.util.*;

public class ExecutableWriter {

    /**
     * TI-TXT formatında bellek görüntüsünü yazar.
     * Bu format, bellekteki veri bloklarını adresleriyle birlikte listeler.
     * Kod boyutunu küçültmek ve okunabilirliği artırmak için, bellekteki uzun
     * sıfır dizileri (boşluklar) dosyaya yazılmaz. Metot, sadece veri içeren
     * anlamlı "chunk"ları (parçaları) bulur ve onları yazar.
     */
    public static void writeTiTxt(Map<String, MemorySegment> segments, String outputFile) throws IOException {
        // Bir veri bloğunu (chunk) sonlandırmak için gereken ardışık sıfır sayısı.
        // Örneğin, 16 tane 0x00 byte'ı peş peşe gelirse, yeni bir bloğa başlanır.
        final int MAX_ZERO_GAP = 16; // Chunks are split by this many consecutive zeros.

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (MemorySegment segment : segments.values()) {
                // BSS segmentini atla, çünkü içinde programlanacak veri yok.
                if (segment.getName().equals(".bss")) continue;

                byte[] data = segment.getContent();
                int origin = segment.getOrigin();
                int length = data.length;

                int i = 0;
                while (i < length) {
                    // 1. Veri içeren bir sonraki bloğun başlangıcını bul (baştaki sıfırları atla)
                    while (i < length && data[i] == 0) {
                        i++;
                    }
                    if (i >= length) break; // Segmentin sonu

                    int chunkStart = i;

                    // 2. Bu bloğun sonunu bul. Uzun bir sıfır dizisi bloğu bitirir.
                    int chunkEnd = chunkStart;
                    int consecutiveZeros = 0;
                    for (int j = chunkStart; j < length; j++) {
                        if (data[j] == 0) {
                            consecutiveZeros++;
                        } else {
                            chunkEnd = j; // Gördüğümüz son veri byte'ı burası
                            consecutiveZeros = 0;
                        }
                        // Eğer büyük bir boşluğa ulaştıysak, blok bitmiştir.
                        if (consecutiveZeros >= MAX_ZERO_GAP) {
                            break;
                        }
                    }

                    // 3. Tespit edilen bloğu yazdır
                    writer.write(String.format("@%04X\n", origin + chunkStart));

                    int lineByteCount = 0;
                    for (int k = chunkStart; k <= chunkEnd; k++) {
                        writer.write(String.format("%02X", data[k]));
                        lineByteCount++;

                        boolean isLastByteInChunk = (k == chunkEnd);
                        if (isLastByteInChunk) {
                            writer.write("\n");
                            break;
                        }

                        if (lineByteCount % 16 == 0) {
                            writer.write("\n");
                        } else {
                            writer.write(" ");
                        }
                    }

                    // 4. Ana işaretçiyi işlenen bloğun sonuna taşı ve bir sonraki bloğu ara.
                    i = chunkEnd + 1;
                }
            }

            writer.write("q\n"); // TI-TXT formatının sonlandırma karakteri
        }

        System.out.println("[TI-TXT] Uyumlu çıktı başarıyla yazıldı.");
    }
}