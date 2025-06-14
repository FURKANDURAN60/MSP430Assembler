package assembler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kaynak koddaki macroları işleyen, genişleten ve PassOne için hazırlayan sınıf.
 * .macro, .endm, .mlib direktiflerini ve yerel etiketleri (?) destekler.
 */
public class MacroProcessor {

    private final Map<String, Macro> macros = new HashMap<>();
    private int uniqueLabelCounter = 0;

    /**
     * Verilen kaynak dosyayı okur, macroları genişletir ve sonucu bir String olarak döner.
     * @param sourcePath Ana kaynak dosyanın yolu.
     * @return Macroları genişletilmiş tam assembly kodu.
     */
    public String expandFile(String sourcePath) throws IOException {
        List<String> inputLines = Files.readAllLines(Paths.get(sourcePath));
        // Ana dosyanın bulunduğu dizini alarak .mlib direktifindeki göreceli yolları çözmek için kullan.
        String baseDir = new File(sourcePath).getParent();
        List<String> expandedLines = processLines(inputLines, baseDir);
        return String.join("\n", expandedLines);
    }

    /**
     * Verilen satır listesini işler, macro tanımlarını kaydeder ve macro çağrılarını genişletir.
     * .mlib direktifi ile karşılaşıldığında kendini özyineli olarak çağırır.
     */
    private List<String> processLines(List<String> lines, String baseDir) throws IOException {
        List<String> outputLines = new ArrayList<>();
        boolean isDefiningMacro = false;
        String currentMacroName = null;
        List<String> currentMacroParams = new ArrayList<>();
        List<String> currentMacroBody = new ArrayList<>();

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (isDefiningMacro) {
                if (trimmedLine.equalsIgnoreCase(".endm")) {
                    macros.put(currentMacroName.toLowerCase(), new Macro(currentMacroName, currentMacroParams, currentMacroBody));
                    isDefiningMacro = false;
                } else {
                    currentMacroBody.add(line); // Girintiyi korumak için orijinal satırı ekle
                }
                continue;
            }

            // Yorumu ayıklayalım
            String effectiveLine = line;
            int commentIndex = effectiveLine.indexOf(';');
            if (commentIndex != -1) {
                effectiveLine = effectiveLine.substring(0, commentIndex);
            }
            String effectiveTrimmedLine = effectiveLine.trim();


            if (effectiveTrimmedLine.isEmpty()) {
                outputLines.add(line);
                continue;
            }

            String[] parts = effectiveTrimmedLine.split("\\s+", -1);
            String firstWord = parts.length > 0 ? parts[0] : "";
            String secondWord = parts.length > 1 ? parts[1] : "";

            if (secondWord.equalsIgnoreCase(".macro")) {
                // Macro tanımı: "isim .macro param1,param2"
                isDefiningMacro = true;
                currentMacroName = firstWord;
                currentMacroBody = new ArrayList<>();

                String[] macroDefParts = effectiveTrimmedLine.split("\\s+", 3);
                if (macroDefParts.length > 2) {
                    currentMacroParams = Arrays.asList(macroDefParts[2].split(","));
                    // Parametrelerdeki boşlukları temizle
                    for (int i = 0; i < currentMacroParams.size(); i++) {
                        currentMacroParams.set(i, currentMacroParams.get(i).trim());
                    }
                } else {
                    currentMacroParams = new ArrayList<>();
                }

            } else if (firstWord.equalsIgnoreCase(".mlib")) {
                // Macro kütüphanesi yükle
                String libPath = effectiveTrimmedLine.substring(5).trim().replace("\"", "");
                File libFile = new File(libPath);
                if (!libFile.isAbsolute()) {
                    libFile = new File(baseDir, libPath);
                }
                List<String> libLines = Files.readAllLines(libFile.toPath());
                // Kütüphane dosyasını işle (bu işlem `macros` haritasını doldurur),
                // ancak genişletilmiş içeriğini ana çıktıya ekleme.
                processLines(libLines, libFile.getParent());

            } else if (macros.containsKey(firstWord.toLowerCase())) {
                // Macro çağırma
                Macro macro = macros.get(firstWord.toLowerCase());
                String[] invocationParts = effectiveTrimmedLine.split("\\s+", 2);
                String[] args = (invocationParts.length > 1) ? invocationParts[1].split(",") : new String[0];

                if (macro.getParameters().size() != args.length) {
                    throw new IllegalArgumentException(String.format(
                            "Macro '%s' çağrısı hatalı: %d parametre bekleniyordu, %d verildi.",
                            macro.getName(), macro.getParameters().size(), args.length));
                }

                uniqueLabelCounter++;

                // Parametre -> Argüman eşleştirme haritası oluştur
                Map<String, String> substitution = new HashMap<>();
                for (int i = 0; i < macro.getParameters().size(); i++) {
                    substitution.put(macro.getParameters().get(i), args[i].trim());
                }

                // Macro gövdesini genişlet
                for (String bodyLine : macro.getBody()) {
                    String expandedLine = bodyLine;
                    for (Map.Entry<String, String> entry : substitution.entrySet()) {
                        // Tam kelime eşleşmesi için regex kullan (örn: 'MOV' içindeki 'M' değişmesin)
                        expandedLine = expandedLine.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
                    }
                    // Benzersiz etiketleri işle ('etiket?' -> 'etiket_123')
                    expandedLine = expandedLine.replaceAll("(\\w+)\\?", "$1_" + uniqueLabelCounter);
                    outputLines.add(expandedLine);
                }
            } else {
                // Normal assembly satırı
                outputLines.add(line);
            }
        }

        if (isDefiningMacro) {
            throw new IllegalStateException("'" + currentMacroName + "' adlı macro tanımı '.endm' ile kapatılmadı.");
        }
        return outputLines;
    }
} 