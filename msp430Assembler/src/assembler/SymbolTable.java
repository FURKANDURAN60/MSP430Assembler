package assembler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Etiket ve Labellerin tutulduğu sınıf */
public class SymbolTable {

    /** Etiketleri ve sembol bilgilerini tutan hashmap */
    private final HashMap<String, SymbolEntry> symbolMap;

    /** Yapıcı fonksiyon */
    public SymbolTable() {
        symbolMap = new HashMap<>();
    }

    /** yeni etiket ve adresini eklemek için kullanılan fonksiyon */
    public void addSymbol(String label, int address, String section) {
        if (symbolMap.containsKey(label)) {
            throw new IllegalArgumentException("Sembol zaten mevcut: " + label);
        }
        // A simple label is LOCAL by default. It becomes DEF if exported via .def
        SymbolEntry e = new SymbolEntry(label, address, SymbolEntry.Binding.LOCAL, section);
        e.setDefined(true); // Label definition makes it "defined"
        symbolMap.put(label, e);
    }

    /** .global, .ref, .def gibi binding türleriyle sembol tanımlamak için */
    public void define(String label, int address, SymbolEntry.Binding binding) {
        // This method creates a new symbol, which is not yet "defined" by a label.
        // `isDefined` will be false by default in the constructor.
            symbolMap.put(label, new SymbolEntry(label, address, binding, null)); // Section is unknown for REF/DEF
    }

    /** Etiketin bellek adresini döndüren fonk. etiketi bulamazsa hata verir */
    public int getAddress(String label) {
        SymbolEntry entry = symbolMap.get(label);
        if (entry == null) {
            throw new IllegalArgumentException("Sembol bulunamadı: " + label);
        }
        return entry.getAddress();
    }

    /** Etiket tabloda var mı yok mu onu kontrol eder. */
    public boolean contains(String label) {
        return symbolMap.containsKey(label);
    }

    /** Tüm etiketleri siler */
    public void clear() {
        symbolMap.clear();
    }

    /** Salt okunur değiştirilmez listeyi döndürür */
    public Map<String, SymbolEntry> getAllSymbols() {
        return Collections.unmodifiableMap(symbolMap);
    }

    /** Sembol tablosunu ekrana yazdırır. */
    public void printSymbolTable() {
        System.out.println("Symbol Table İçeriği:");
        System.out.println("----------------------");
        symbolMap.forEach((label, entry) ->
                System.out.printf("%-15s : %04X (%s, defined: %b, section: %s)\n", label, entry.getAddress(), entry.getBinding(), entry.isDefined(), entry.getSection())
        );
        System.out.println();
    }

    /** .set için veya .def'ten sonra bulunan bir etiketi güncellemek için kullanılır */
    public void addOrUpdateSymbol(String label, int value, String section) {
        SymbolEntry e = symbolMap.get(label);
        if (e != null) {
            // It exists, update its value and mark as defined.
            // Do NOT change the binding. The .def directive is responsible for that.
            e.setAddress(value);
            e.setSection(section);
            e.setDefined(true);
        } else {
            // It doesn't exist, so add it as a new, local, defined symbol.
            addSymbol(label, value, section);
        }
    }

    public void setBinding(String label, SymbolEntry.Binding binding) {
        SymbolEntry entry = symbolMap.get(label);
        if (entry != null) {
            entry.setBinding(binding);
        }
    }

    /** İç sınıf: her sembolün bağlayıcılığını ve adresini tutar */
    public static class SymbolEntry {
        public enum Binding {
            LOCAL,
            DEF,
            REF
        }

        private final String label;
        private int address;
        private Binding binding;
        private boolean isDefined; // True if its address is set by a label
        private String section;    // Hangi section'a ait (.text, .data, etc)
        private String sourceFile; // Sembolün tanımlandığı kaynak dosya (.obj)

        public SymbolEntry(String label, int address, Binding binding, String section) {
            this.label = label;
            this.address = address;
            this.binding = binding;
            this.section = section;
            this.isDefined = false; // Default to not defined
            this.sourceFile = null;
        }

        public String getLabel() { return label; }
        public int getAddress() { return address; }
        public Binding getBinding() { return binding; }
        public boolean isDefined() { return isDefined; }
        public String getSection() { return section; }
        public String getSourceFile() { return sourceFile; }

        public void setAddress(int address) { this.address = address; }
        public void setBinding(Binding binding) { this.binding = binding; }
        public void setDefined(boolean defined) { this.isDefined = defined; }
        public void setSection(String section) { this.section = section; }
        public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    }
}
