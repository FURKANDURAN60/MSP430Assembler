package assembler;

/**
 * RelocationEntry:
 * - REF olarak tanımlanmış sembollerin
 * - hangi adreslerde çözümlenmesi gerektiğini tutar
 */
public class RelocationEntry {
    public enum RelocationType {
        ABSOLUTE_16BIT,     // For extra words (MOV #addr, R5)
        PC_RELATIVE_10BIT   // For jump offsets (JNZ label)
    }

    private final String symbol;            // Hangi sembol çözümlenecek
    private final int address;             // Hangi adres çözümlenecek
    private final RelocationType type;

    public RelocationEntry(String symbol, int address, RelocationType type) {
        this.symbol = symbol;
        this.address = address;
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getAddress() {
        return address;
    }

    public RelocationType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("RelocationEntry{symbol='%s', address=0x%04X, type=%s}", symbol, address, type);
    }
}
