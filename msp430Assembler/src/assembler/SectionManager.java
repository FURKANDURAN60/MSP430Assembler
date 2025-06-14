package assembler;

import java.util.HashMap;
import java.util.Map;

/**
 * Her section için SPC (Section Program Counter) yöneticisi.
 * MSP430'un .text, .data, .bss gibi direktiflerine uygun.
 * Dinamik section desteği (.sect ve .usect) eklendi.
 */
public class SectionManager {

    private final Map<String, Integer> spcMap;
    private String currentSection;

    public SectionManager() {
        spcMap = new HashMap<>();
        spcMap.put(".text", 0);
        spcMap.put(".data", 0);
        spcMap.put(".bss", 0);
        currentSection = ".text"; // Varsayılan
    }

    /**
     * Mevcut aktif section'ı değiştirir. Eğer tanımsızsa dinamik olarak ekler.
     */
    public void setActiveSection(String section) {
        if (!spcMap.containsKey(section)) {
            spcMap.put(section, 0);  // .sect veya .usect ile gelen yeni section
        }
        this.currentSection = section;
    }

    public String getActiveSection() {
        return currentSection;
    }

    public int getCurrentSPC() {
        return spcMap.get(currentSection);
    }

    public void incrementSPC(int amount) {
        spcMap.put(currentSection, spcMap.get(currentSection) + amount);
    }

    public void setSPC(String section, int value) {
        if (!spcMap.containsKey(section)) {
            spcMap.put(section, value);  // bilinmeyen section'a SPC setle
        } else {
            spcMap.put(section, value);
        }
    }

    public int getSPC(String section) {
        return spcMap.getOrDefault(section, 0);
    }

    /**
     * SPC tablosunun tümünü dışarı verir (debug veya analiz için).
     */
    public Map<String, Integer> getAllSPCs() {
        return new HashMap<>(spcMap);
    }

    public boolean isInitializedSection(String sectionName) {
        // .text ve .data her zaman initialized
        if (sectionName.equals(".text") || sectionName.equals(".data")) return true;

        // .bss ve .usect daima uninitialized
        if (sectionName.equals(".bss") || sectionName.equals(".usect")) return false;

        // .sect "..." gibi isimlendirilmiş özel section'lar initialized olarak kabul edilir
        return true;
    }

}
