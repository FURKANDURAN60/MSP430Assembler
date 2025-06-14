package assembler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * PassOne:
 * - Etiketleri sembol tablosuna ekler
 * - Instruction listesini oluşturur
 * - Her section için SectionManager üzerinden SPC takibi yapar
 */
public class PassOne {

    private final SymbolTable symbolTable;
    private final List<Instruction> instructions;
    private final SectionManager sectionManager;
    private String currentSourceFile; // Mevcut dosya adını saklamak için

    public PassOne() {
        symbolTable = new SymbolTable();
        instructions = new ArrayList<>();
        sectionManager = new SectionManager();
    }

    public void processFile(String sourceFile) {
        try {
            // 1. ADIM: Macroları genişletmek için MacroProcessor'ı kullan
            this.currentSourceFile = new java.io.File(sourceFile).getName();
            MacroProcessor macroProcessor = new MacroProcessor();
            String expandedCode = macroProcessor.expandFile(sourceFile);

            // 2. ADIM: Genişletilmiş kodu işle
            processString(expandedCode);

        } catch (IOException e) {
            System.err.println("Dosya okunamadı veya macro işlenemedi: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Macro hatalarını (örn: argüman sayısı, kapatılmamış macro) yakala
            System.err.println("Macro işleme hatası: " + e.getMessage());
            e.printStackTrace();
            // GUI'de hatayı göstermek için bir RuntimeException fırlatabiliriz
            throw new RuntimeException("Macro Hatası: " + e.getMessage(), e);
        }
    }

    private void processLine(String line) {
        line = line.trim();

        int commentIndex = line.indexOf(';');
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex).trim();
        }

        if (line.isEmpty()) return;

        String label = null;
        String mnemonic;
        String operandString = null;
        int bw = 0;

        String[] parts = line.split("[\\s]+", 2);

        if (parts[0].endsWith(":")) {
            label = parts[0].substring(0, parts[0].length() - 1);
            if (symbolTable.contains(label)) {
                SymbolTable.SymbolEntry entry = symbolTable.getAllSymbols().get(label);
                if (entry.isDefined()) {
                throw new IllegalArgumentException("Aynı etiket birden fazla kez tanımlanamaz: " + label);
            }
                if (entry.getBinding() == SymbolTable.SymbolEntry.Binding.REF) {
                     throw new IllegalArgumentException("Hata: '" + label + "' .REF ile içeri aktarılmış, bu dosyada yeniden tanımlanamaz.");
                }
                // It's a forward reference from .def. Update it.
                symbolTable.addOrUpdateSymbol(label, sectionManager.getCurrentSPC(), sectionManager.getActiveSection());
            } else {
                // Not in table, a simple new label.
            symbolTable.addSymbol(label, sectionManager.getCurrentSPC(), sectionManager.getActiveSection());
            }

            if (parts.length > 1) {
                line = parts[1];
                parts = line.split("[\\s]+", 2);
            } else {
                Instruction inst = new Instruction(label, null, null, sectionManager.getCurrentSPC(), 0);
                inst.setSection(sectionManager.getActiveSection());
                inst.setRawLine(label + ":"); // sadece etiket varsa onu rawLine olarak kaydet
                inst.setSourceFile(currentSourceFile);
                instructions.add(inst);
                return;
            }
        }

        mnemonic = parts[0].toLowerCase();

        // .equ veya .set tanımı ise
        if (mnemonic.equalsIgnoreCase(".equ") || mnemonic.equalsIgnoreCase(".set")) {
            if (label == null) {
                // .equ/.set için etiketsiz kullanım anlamsız, ancak macro içinde olabilir.
                // Şimdilik hata fırlatmak yerine uyarı verip geçmek daha güvenli olabilir.
                // System.err.println("Uyarı: Etiketsiz .equ/.set tanımı bulundu: " + line);
                return; // Bu satırı atla
            }

            if (parts.length < 2) {
                throw new IllegalArgumentException(mnemonic + " için değer belirtilmemiş.");
            }

            String valueStr = parts[1].trim();
            int value = LiteralResolver.resolve(valueStr, symbolTable);

            if (mnemonic.equalsIgnoreCase(".equ")) {
                if (symbolTable.contains(label)) {
                    throw new IllegalArgumentException(".equ ile sembol yeniden tanımlanamaz: " + label);
                }
                symbolTable.addSymbol(label, value, ".text"); // .equ symbols don't have a section, using .text as placeholder
            } else {
                // .set
                symbolTable.addOrUpdateSymbol(label, value, ".text");
            }

            return; // satır sonlandırılır
        }

        // .equ veya .set tanımı varsa ve label içeriyorsa (örn: BUF_SIZE .equ 16)
        if (line.matches("^[a-zA-Z_][\\w]*\\s+\\.(equ|set)\\b.*")) {
            int firstSpace = line.indexOf(' ');
            String rawLabel = line.substring(0, firstSpace).trim();

            String rest = line.substring(firstSpace).trim();
            int secondSpace = rest.indexOf(' ');
            String directive = (secondSpace == -1) ? rest : rest.substring(0, secondSpace).trim();
            String operand = (secondSpace == -1) ? "" : rest.substring(secondSpace).trim();

            int value = LiteralResolver.resolve(operand, symbolTable);

            if (directive.equalsIgnoreCase(".equ")) {
                if (symbolTable.contains(rawLabel)) {
                    throw new IllegalArgumentException(".equ ile sembol yeniden tanımlanamaz: " + rawLabel);
                }
                symbolTable.addSymbol(rawLabel, value, ".text");
            } else {
                symbolTable.addOrUpdateSymbol(rawLabel, value, ".text");
            }

            return;
        }

        // .REF and .DEF handling
        if (mnemonic.equalsIgnoreCase(".ref")) {
            if (parts.length < 2) throw new IllegalArgumentException(".ref için sembol belirtilmemiş.");
            String[] refSymbols = parts[1].split(",");
            for (String sym : refSymbols) {
                // Add to symbol table with REF binding, address is 0 as it's external
                symbolTable.define(sym.trim(), 0, SymbolTable.SymbolEntry.Binding.REF);
            }
            // This is a directive, not an instruction, so we return.
            return;
        }

        if (mnemonic.equalsIgnoreCase(".def")) {
            if (parts.length < 2) throw new IllegalArgumentException(".def için sembol belirtilmemiş.");
            String[] defSymbols = parts[1].split(",");
            for (String sym : defSymbols) {
                String symbolToDefine = sym.trim();
                if (symbolTable.contains(symbolToDefine)) {
                    // It might exist as REF from another module, update to DEF
                    symbolTable.setBinding(symbolToDefine, SymbolTable.SymbolEntry.Binding.DEF);
                } else {
                    // Define it with a placeholder address. The label definition will update it.
                    symbolTable.define(symbolToDefine, 0, SymbolTable.SymbolEntry.Binding.DEF);
                }
            }
            return;
        }

        // Section değişimi varsa .text, .data, .bss
        if (mnemonic.equals(".text") || mnemonic.equals(".data") || mnemonic.equals(".bss")) {
            sectionManager.setActiveSection(mnemonic);
            return;
        }

        // .ORG varsa SPC güncelle
        if (mnemonic.equals(".org")) {
            int newAddr = Integer.decode(parts[1].trim());
            sectionManager.setSPC(sectionManager.getActiveSection(), newAddr);
            return;
        }

        // .B / .W ayrımı
        if (mnemonic.endsWith(".b")) {
            bw = 1;
            mnemonic = mnemonic.substring(0, mnemonic.length() - 2);
        } else if (mnemonic.endsWith(".w")) {
            bw = 0;
            mnemonic = mnemonic.substring(0, mnemonic.length() - 2);
        }

        if (parts.length > 1) {
            operandString = parts[1];
        }

        int format = determineFormat(mnemonic);
        int currentSPC = sectionManager.getCurrentSPC();

        Instruction inst = new Instruction(label, mnemonic.toUpperCase(), operandString, currentSPC, format);
        inst.setSection(sectionManager.getActiveSection());
        inst.setSourceFile(currentSourceFile);
        inst.setBw(bw);
        instructions.add(inst);
        inst.setRawLine(line);


        updateSPC(inst);
    }

    private void updateSPC(Instruction inst) {
        // Sözde komutların gerçekte neye dönüştüğünü öğrenmek için PassTwo'daki
        // merkezi dönüşüm mantığını kullanıyoruz. Bu, SPC hesaplamasının doğru olmasını sağlar.
        PassTwo.TransformedInstruction ti = PassTwo.transformPseudoInstruction(inst);

        String mnemonic = ti.mnemonic();
        String operandString = ti.operands();
        int format = ti.format();

        int size = 2; // Varsayılan komut boyutu

        if (mnemonic.startsWith(".")) {
            switch (mnemonic.toLowerCase()) {
                case ".word":
                    sectionManager.incrementSPC(2 * countOperands(operandString));
                    return;
                case ".byte":
                    sectionManager.incrementSPC(countOperands(operandString));
                    return;
                case ".resw":
                    sectionManager.incrementSPC(2 * resolveLiteralValue(operandString));
                    return;
                case ".space":
                case ".bss": // .bss ve .space boyut hesaplamada benzerdir
                    sectionManager.incrementSPC(resolveLiteralValue(operandString));
                    return;
                case ".string":
                    String cleaned = operandString.trim();
                    if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
                        sectionManager.incrementSPC(cleaned.length() - 2 + 1); // +1 for null
                    } else {
                        throw new IllegalArgumentException(".string literali geçersiz: " + operandString);
                    }
                    return;
                case ".float":
                    sectionManager.incrementSPC(4);
                    return;
                default:
                    // Diğer direktifler (.sect, .equ, .set vb.) yer kaplamaz
                    return;
            }
        }

        // Komutlar için boyut hesaplama
        if (operandString == null || operandString.trim().isEmpty()) {
            sectionManager.incrementSPC(size); // RETI gibi operandsız komutlar için 2 byte
            return;
        }

        String[] operands = operandString.split(",");

        // Format 1 (2 operand) ve Format 2 (1 operand) için
        if (format == 1 || format == 2) {
            for (String opStr : operands) {
                OperandInfo op = parseOperandForSize(opStr.trim());
                if (op.requiresExtraWord) {
                    size += 2;
                }
            }
        }
        // Format 3 (Jump) her zaman 2 byte'tır ve JMP/BR zaten Format 1'e dönüştürüldüğü için bu mantık doğrudur.

        sectionManager.incrementSPC(size);
    }

    private OperandInfo parseOperandForSize(String operand) {
        operand = operand.trim();
        if (operand.startsWith("##")) {
            operand = operand.substring(1);
        }

        // Register direct, indirect, and auto-increment do not require an extra word.
        if (RegisterTable.isRegister(operand) ||
            (operand.startsWith("@") && RegisterTable.isRegister(operand.substring(1, operand.endsWith("+") ? operand.length() - 1 : operand.length())))) {
            return new OperandInfo(false);
        }

        // Immediate values (#...) check for constant generator
        if (operand.startsWith("#")) {
            String valueStr = operand.substring(1);
            try {
                int value = LiteralResolver.resolve(valueStr, symbolTable);
                switch (value) {
                    case 0: case 1: case 2: case -1: case 4: case 8:
                        return new OperandInfo(false); // Handled by constant generator
                    default:
                        return new OperandInfo(true);  // Needs an extra word
                }
            } catch (Exception e) {
                // If resolving fails, it's likely a symbolic expression that needs relocation.
                // e.g., #LABEL. This always requires an extra word.
                return new OperandInfo(true);
            }
        }

        // Symbolic addresses, absolute addresses (&...), and indexed addressing (X(R))
        // all require an extra word.
        return new OperandInfo(true);
    }

    private static class OperandInfo {
        boolean requiresExtraWord;
        OperandInfo(boolean req) {
            this.requiresExtraWord = req;
        }
    }

    private int countOperands(String operandString) {
        if (operandString == null || operandString.trim().isEmpty()) return 0;
        return operandString.split(",").length;
    }

    private int determineFormat(String mnemonic) {
        if (mnemonic.startsWith(".")) return 0;

        String bin = new OpcodeTable().getOpcode(mnemonic);
        if (bin.length() == 4) return 1;
        if (bin.length() == 9) return 2;
        if (bin.length() == 6) return 3;

        return 0;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public int getSPC() {
        return sectionManager.getCurrentSPC();
    }

    public void printSymbolTable() {
        symbolTable.printSymbolTable();
    }

    public void processString(String input) {
        try (BufferedReader br = new BufferedReader(new StringReader(input))) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e) {
            // This should not happen with a StringReader
            System.err.println("Kod okunamadı: " + e.getMessage());
        }
    }

    private int resolveLiteralValue(String token) {
        return LiteralResolver.resolve(token, symbolTable);
    }

}
