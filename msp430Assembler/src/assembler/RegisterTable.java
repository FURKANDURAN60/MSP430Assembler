package assembler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegisterTable {

    private static final Map<String, Integer> registers;

    /**Registar tablosu aliaslarla birlikte*/
    static {
        HashMap<String, Integer> tempMap = new HashMap<>();

        tempMap.put("R0", 0);  tempMap.put("PC", 0);
        tempMap.put("R1", 1);  tempMap.put("SP", 1);
        tempMap.put("R2", 2);  tempMap.put("SR", 2);
        tempMap.put("R3", 3);  tempMap.put("CG", 3);

        for (int i = 4; i <= 15; i++) {
            tempMap.put("R" + i, i);
        }

        registers = Collections.unmodifiableMap(tempMap);
    }

    /** verilen stringin register olup olamdığını kontrol etme*/
    public static boolean isRegister(String reg) {
        return registers.containsKey(reg.toUpperCase());
    }

    /**
     * Register ismine karşılık gelen register numarasını döner.
     * Hatalı isim verilirse exception fırlatır.
     */
    public static int getRegisterNumber(String reg) {
        Integer number = registers.get(reg.toUpperCase());
        if (number == null) {
            throw new IllegalArgumentException("Geçersiz register ismi: " + reg);
        }
        return number;
    }

    /**Salt okunur şekilde Registirları yazdırma*/
    public static Map<String, Integer> getRegisters() {
        return registers;
    }

    /** Register tablosunu konsola yazdırıy */
    public static void printRegisters() {
        System.out.println("Register Tablosu:");
        registers.forEach((name, number) ->
                System.out.printf("%-5s : %d\n", name, number)
        );
    }
}
