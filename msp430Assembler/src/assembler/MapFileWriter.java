package assembler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MapFileWriter {

    public static void writeMapFile(String mapFilename, String outputName, Linker linker) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mapFilename))) {
            writer.write("******************************************************************************\n");
            writer.write("*                             MSP430 LINKER MAP FILE                             *\n");
            writer.write("******************************************************************************\n\n");

            writer.write("OUTPUT FILE NAME: <" + outputName + ">\n\n");
            // Bizim sistemimizde belirli bir giriş noktası sembolü yok, bu yüzden bu adımı atlıyoruz.

            writer.write("SECTION ALLOCATION MAP\n\n");
            writer.write("output                                  attributes/\n");
            writer.write("section   page    origin      length      input sections\n");
            writer.write("--------  ----  ----------  ----------  --------------------\n");

            String currentSection = "";
            // Katkıları section ve sonra kaynak dosyaya göre sırala
            List<Linker.SectionContribution> sortedContributions = new ArrayList<>(linker.getSectionContributions());
            sortedContributions.sort(Comparator.comparing((Linker.SectionContribution c) -> c.sectionName)
                                               .thenComparing(c -> c.sourceFile));

            for (Linker.SectionContribution contrib : sortedContributions) {
                if (!contrib.sectionName.equals(currentSection)) {
                    currentSection = contrib.sectionName;
                    MemorySegment segment = linker.getSegments().get(currentSection);
                    if (segment != null) {
                        writer.write(String.format("%-8s  %4d  0x%08X  0x%08X\n",
                                segment.getName(), 0, segment.getOrigin(), segment.getLength()));
                    }
                }
                // Katkının *son* (relocated) adresini yazdır.
                writer.write(String.format("%16s0x%08X  0x%08X  %s (%s)\n",
                        "", contrib.finalAddress, contrib.length, contrib.sourceFile, contrib.sectionName));
            }
            writer.write("\n\n");

            writer.write("GLOBAL SYMBOLS\n\n");
            writer.write("address     name\n");
            writer.write("----------  --------------------\n");

            List<SymbolTable.SymbolEntry> sortedSymbols = new ArrayList<>(linker.getSymbolTable().getAllSymbols().values());
            sortedSymbols.sort(Comparator.comparingInt(SymbolTable.SymbolEntry::getAddress));

            int globalSymbolCount = 0;
            for (SymbolTable.SymbolEntry symbol : sortedSymbols) {
                if (symbol.getBinding() == SymbolTable.SymbolEntry.Binding.DEF) {
                    writer.write(String.format("0x%08X  %s\n", symbol.getAddress(), symbol.getLabel()));
                    globalSymbolCount++;
                }
            }

            writer.write("\n[" + globalSymbolCount + " symbols]\n");
        }
    }
} 