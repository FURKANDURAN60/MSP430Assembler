package assembler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OpcodeTable {

    /** Komut ve opcode değerlerini saklayan hashmapp */
    private final HashMap<String, String> opcodeMap;

    public OpcodeTable() {
        opcodeMap = new HashMap<>();
        initializeOpcodes();
    }

    private void initializeOpcodes() {
        // Format 1 (Double Operand)
        opcodeMap.put("MOV", "0100");
        opcodeMap.put("ADD", "0101");
        opcodeMap.put("ADDC", "0110");
        opcodeMap.put("SUBC", "0111");
        opcodeMap.put("SUB", "1000");
        opcodeMap.put("CMP", "1001");
        opcodeMap.put("DADD", "1010");
        opcodeMap.put("BIT", "1011");
        opcodeMap.put("BIC", "1100");
        opcodeMap.put("BIS", "1101");
        opcodeMap.put("XOR", "1110");
        opcodeMap.put("AND", "1111");

        // Format 2 (Single Operand)
        opcodeMap.put("RRC",  "000100000");
        opcodeMap.put("SWPB", "000100001");
        opcodeMap.put("RRA",  "000100010");
        opcodeMap.put("SXT",  "000100011");
        opcodeMap.put("PUSH", "000100100");
        opcodeMap.put("CALL", "000100101");
        opcodeMap.put("RETI", "000100110");


        // Format 3 (Jumps)
        opcodeMap.put("JNE", "001000"); opcodeMap.put("JNZ", "001000");
        opcodeMap.put("JEQ", "001001"); opcodeMap.put("JZ", "001001");
        opcodeMap.put("JNC", "001010");
        opcodeMap.put("JC", "001011");
        opcodeMap.put("JN", "001100");
        opcodeMap.put("JGE", "001101");
        opcodeMap.put("JL", "001110");
        opcodeMap.put("JMP", "001111"); opcodeMap.put("BR",  "001111");

        // Ozel Durumlar
        opcodeMap.put("NOP", "NOP");

        opcodeMap.put("CLR", "0100"); // MOV
        opcodeMap.put("INC", "0101"); // ADD
        opcodeMap.put("DEC", "1000"); // SUB
        opcodeMap.put("TST", "1001"); // CMP


    }

    /** Komutun opcode unu verdiren fnks */
    public String getOpcode(String mnemonic) {
        String opcode = opcodeMap.get(mnemonic.toUpperCase());
        if (opcode == null) {
            throw new IllegalArgumentException("Geçersiz komut: " + mnemonic);
        }
        return opcode;
    }

     /** tabloda varsa true yoksa false */
    public boolean contains(String mnemonic) {
        return opcodeMap.containsKey(mnemonic.toUpperCase());
    }

    /** Opcode ları dışarı verir ama değiştirilmesine izin vermez put remove gibi komutların çalışmalarını engeller*/
    public Map<String, String> getAllOpcodes() {
        return Collections.unmodifiableMap(opcodeMap);
    }

    /** Konsol Çıktısı*/
    public void printOpcodeTable() {
        System.out.println("Opcode Table İçeriği:");
        System.out.println("----------------------");
        opcodeMap.forEach((mnemonic, opcode) ->
                System.out.printf("%-10s : %s\n", mnemonic, opcode)
        );
        System.out.println();
    }
}
