package assembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Tek satırı ifade eder.
 * Bu sınıf üzerinden etiket, mnemonic, operand ve adres bilgileri saklanır ve makine kodu oluşturulur.
 */
public class Instruction {

    private String label;         // Satırdaki etiket (varsa)
    private String mnemonic;      // Komut
    private String operandString; // Operant kısmı
    private int address;          // LOCCTR den atanan bellek adresi
    private Integer machineCode;      // hexkod
    private int format;           // Komut formatı
    private int bw;               // Byte/Word biti
    private String rawLine;       // Assembly satırının orijinal hali
    private String section;       // Bu komutun ait olduğu segment (.text, .data, ...)
    private String sourceFile;    // Bu komutun geldiği kaynak .obj dosyası

    private List<Integer> extraWords = new ArrayList<>();
    private List<Integer> extraBytes = new ArrayList<>();

    /**Yapıc*/
    public Instruction(String label, String mnemonic, String operandString, int address, int format) {
        this.label = label;
        this.mnemonic = mnemonic;
        this.operandString = operandString;
        this.address = address;
        this.format = format;
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public String getMnemonic() {
        return mnemonic;
    }
    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public String getOperandString() {
        return operandString;
    }
    public void setOperandString(String operandString) {
        this.operandString = operandString;
    }

    public int getAddress() {
        return address;
    }
    public void setAddress(int address) {
        this.address = address;
    }

    public Integer getMachineCode() {
        return machineCode;
    }
    public void setMachineCode(Integer machineCode) {
        this.machineCode = machineCode;
    }

    public int getFormat() {
        return format;
    }
    public void setFormat(int format) {
        this.format = format;
    }

    public int getBw() {
        return bw;
    }
    public void setBw(int bw) {
        this.bw = bw;
    }

    public String getRawLine() {
        return rawLine;
    }
    public void setRawLine(String rawLine) {
        this.rawLine = rawLine;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public List<Integer> getExtraWords() {
        return extraWords;
    }

    public void setExtraWords(List<Integer> extraWords) {
        this.extraWords = extraWords;
    }

    public List<Integer> getExtraBytes() {
        return extraBytes;
    }

    public void setExtraBytes(List<Integer> extraBytes) {
        this.extraBytes = extraBytes;
    }

    /** Assembly satırının temiz bir temsilini verir.*/
    @Override
    public String toString() {
        return String.format("%04X: %04X ; %s %s", address, machineCode,
                (mnemonic != null ? mnemonic : ""),
                (operandString != null ? operandString : ""));
    }
}
