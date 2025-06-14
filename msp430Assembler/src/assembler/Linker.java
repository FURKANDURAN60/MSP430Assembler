package assembler;

import java.util.*;
import java.util.stream.Collectors;

public class Linker {

    private final List<Instruction> originalInstructions;
    private final SymbolTable originalSymbolTable;
    private final List<RelocationEntry> originalRelocationTable;

    private List<Instruction> finalInstructions;
    private SymbolTable finalSymbolTable;
    private final Map<String, MemorySegment> segments = new LinkedHashMap<>();
    private final List<SectionContribution> sectionContributions = new ArrayList<>();

    private final Map<String, Integer> defaultOrigins = Map.of(
            ".text", 0xF800,
            ".data", 0x2000,
            ".bss", 0x3000
    );

    public Linker(List<Instruction> instructions, SymbolTable symbolTable, List<RelocationEntry> relocationTable) {
        this.originalInstructions = instructions;
        this.originalSymbolTable = symbolTable;
        this.originalRelocationTable = relocationTable;
    }

    private void placeChunk(SectionContribution chunk, int baseAddress, List<Instruction> finalInstructions, SymbolTable finalSymbolTable, List<RelocationEntry> finalRelocations) {
        chunk.finalAddress = baseAddress;
        int relocationOffset = baseAddress - chunk.origin;

        // Bu parçaya ait instruction'ları, sembolleri ve relocation'ları bul ve güncelle
        originalInstructions.stream()
                .filter(i -> chunk.sourceFile.equals(i.getSourceFile()) && chunk.sectionName.equals(i.getSection()))
                .forEach(inst -> {
                    inst.setAddress(inst.getAddress() + relocationOffset);
                    finalInstructions.add(inst);
                });

        originalSymbolTable.getAllSymbols().values().stream()
                .filter(s -> chunk.sourceFile.equals(s.getSourceFile()) && chunk.sectionName.equals(s.getSection()))
                .forEach(s -> finalSymbolTable.define(s.getLabel(), s.getAddress() + relocationOffset, s.getBinding()));

        originalRelocationTable.stream()
            .filter(r -> {
                // Bu relocation entry'sinin ait olduğu instruction'ı bul.
                Instruction ownerInstruction = originalInstructions.stream()
                        .filter(i -> {
                            // Instruction'ın başlangıç adresi, relocation adresinden önce veya ona eşit olmalı.
                            if (r.getAddress() < i.getAddress()) return false;
                            // Relocation adresi, instruction'ın kapladığı alan içinde olmalı.
                            return r.getAddress() < (i.getAddress() + calculateInstructionSize(i));
                        })
                        .findFirst().orElse(null);

                // Eğer instruction bulunduysa ve bu chunk'a aitse, bu relocation entry'sini işle.
                return ownerInstruction != null &&
                       chunk.sourceFile.equals(ownerInstruction.getSourceFile()) &&
                       chunk.sectionName.equals(ownerInstruction.getSection());
            })
            .forEach(r -> finalRelocations.add(new RelocationEntry(r.getSymbol(), r.getAddress() + relocationOffset, r.getType())));
    }

    public void link() {
        // --- 1. Adım: Her bölüm parçasının (contribution) bilgilerini hesapla ---
        // Her bir .obj dosyasından gelen .text, .data gibi bölümlerin başlangıç adreslerini ve
        // boyutlarını tespit et. Bu parçalar daha sonra birleştirilecek.
        Map<String, Map<String, List<Instruction>>> instructionsByFileAndSection = new HashMap<>();
        for (Instruction i : originalInstructions) {
            instructionsByFileAndSection
                    .computeIfAbsent(i.getSourceFile(), k -> new HashMap<>())
                    .computeIfAbsent(i.getSection(), k -> new ArrayList<>())
                    .add(i);
        }

        for (var fileEntry : instructionsByFileAndSection.entrySet()) {
            String sourceFile = fileEntry.getKey();
            for (var sectionEntry : fileEntry.getValue().entrySet()) {
                String sectionName = sectionEntry.getKey();
                List<Instruction> chunkInstructions = sectionEntry.getValue();
                if (chunkInstructions.isEmpty()) continue;

                int origin = chunkInstructions.stream().mapToInt(Instruction::getAddress).min().getAsInt();
                int maxAddr = chunkInstructions.stream().mapToInt(i -> i.getAddress() + calculateInstructionSize(i)).max().getAsInt();
                int length = maxAddr - origin;
                sectionContributions.add(new SectionContribution(sourceFile, sectionName, origin, length));
            }
        }

        // --- 2. Adım: Bölümleri yerleştir ve adresleri yeniden hesapla ---
        // Bölümleri (.text, .data, .bss sırasıyla) ve içindeki parçaları belleğe yerleştir.
        // .org ile belirtilmiş mutlak adresli parçalar önceliklidir. Diğerleri art arda eklenir.
        // Bu adımda tüm sembollerin ve komutların nihai adresleri belirlenir.
        finalInstructions = new ArrayList<>();
        finalSymbolTable = new SymbolTable();
        List<RelocationEntry> finalRelocations = new ArrayList<>();
        Map<String, Integer> placementCounters = new HashMap<>(defaultOrigins);

        // Bölümleri sırala: .text, .data, .bss...
        List<String> sortedSectionNames = sectionContributions.stream()
                .map(sc -> sc.sectionName)
                .distinct()
                .sorted(Comparator.comparingInt(s -> defaultOrigins.containsKey(s) ? new ArrayList<>(defaultOrigins.keySet()).indexOf(s) : Integer.MAX_VALUE))
                .collect(Collectors.toList());

        for (String sectionName : sortedSectionNames) {
            List<SectionContribution> allChunks = sectionContributions.stream()
                    .filter(sc -> sc.sectionName.equals(sectionName))
                    .collect(Collectors.toList());

            // Find absolute chunks (defined with .ORG) vs relocatable ones
            Optional<SectionContribution> absoluteChunkOpt = allChunks.stream()
                    .filter(c -> c.origin != 0)
                    .findFirst(); // Assuming at most one absolute per section for simplicity

            List<SectionContribution> relocatableChunks = allChunks.stream()
                    .filter(c -> c.origin == 0)
                    .sorted(Comparator.comparing(c -> c.sourceFile)) // Keep relocatable sorted for determinism
                    .collect(Collectors.toList());

            if (allChunks.stream().filter(c -> c.origin != 0).count() > 1) {
                throw new IllegalStateException("Linker Hatası: '" + sectionName + "' bölümü, .ORG ile birden fazla mutlak başlangıç adresine sahip.");
            }

            int currentAddress;

            // First, place the absolute chunk, if it exists. It dictates the base address.
            if (absoluteChunkOpt.isPresent()) {
                SectionContribution absChunk = absoluteChunkOpt.get();
                // Place it at its own origin. The relocation offset will be 0.
                placeChunk(absChunk, absChunk.origin, finalInstructions, finalSymbolTable, finalRelocations);
                currentAddress = absChunk.origin + absChunk.length;
            } else {
                // No absolute chunk, start from the default for this section.
                currentAddress = placementCounters.getOrDefault(sectionName, 0);
            }

            // Now, place all relocatable chunks consecutively after the absolute one (or the default start).
            for (SectionContribution relChunk : relocatableChunks) {
                placeChunk(relChunk, currentAddress, finalInstructions, finalSymbolTable, finalRelocations);
                currentAddress += relChunk.length;
            }

            placementCounters.put(sectionName, currentAddress);
        }
        
        // --- 3. Adım: REF sembollerini final tabloya ekle ---
        // Diğer modüllere referans veren (.ref) ama bu linkleme biriminde tanımlanmamış
        // sembolleri nihai sembol tablosuna ekle. Bu sembollerin adresleri bir sonraki
        // adımda çözülecek.
        originalSymbolTable.getAllSymbols().values().stream()
                .filter(s -> s.getBinding() == SymbolTable.SymbolEntry.Binding.REF)
                .forEach(s -> {
                    if (!finalSymbolTable.contains(s.getLabel())) {
                        finalSymbolTable.define(s.getLabel(), s.getAddress(), s.getBinding());
                    }
                });

        // --- 4. Adım: Relocation'ları çöz ---
        // Sembol referanslarını (örn: CALL #etiket) nihai adreslerle değiştir.
        // Bu, ya bir komutun ekstra kelimesini (extraWord) ya da JUMP komutunun
        // makine kodundaki offset'i güncellemeyi içerir.
        for (RelocationEntry entry : finalRelocations) {
            String symbol = entry.getSymbol();
            int fixupAddr = entry.getAddress();

            if (!finalSymbolTable.contains(symbol)) {
                System.err.println("Linker Hatası: Sembol bulunamadı: " + symbol);
                continue;
            }
            int resolvedAddress = finalSymbolTable.getAddress(symbol);
            Instruction targetInst = findInstructionToPatch(finalInstructions, fixupAddr);

            if (targetInst == null) {
                System.err.println("Linker Hatası: Relocation adresi için instruction bulunamadı: " + String.format("0x%04X", fixupAddr));
                continue;
            }

            switch (entry.getType()) {
                case ABSOLUTE_16BIT:
                    // Bu yama bir extraWord'ü hedefler.
                    int instructionStartAddr = targetInst.getAddress();
                    // Makine kodundan sonraki ilk kelime
                    int firstExtraWordAddr = instructionStartAddr + ((targetInst.getMachineCode() != null && targetInst.getMachineCode() != 0) ? 2 : 0);
                    int byteOffsetFromFirstExtraWord = fixupAddr - firstExtraWordAddr;

                    if (byteOffsetFromFirstExtraWord >= 0 && byteOffsetFromFirstExtraWord % 2 == 0) {
                        int extraWordIndex = byteOffsetFromFirstExtraWord / 2;
                        if (extraWordIndex < targetInst.getExtraWords().size()) {
                            targetInst.getExtraWords().set(extraWordIndex, resolvedAddress);
                        } else {
                            System.err.println("Linker Hatası: extraWord index sınır dışında. Adres: " + String.format("0x%04X", fixupAddr));
                        }
                    } else {
                        System.err.println("Linker Hatası: Geçersiz extraWord adresi. Adres: " + String.format("0x%04X", fixupAddr));
                    }
                    break;

                case PC_RELATIVE_10BIT:
                    // Bu yama, komutun kendi makine kodunu hedefler.
                    int offset = (resolvedAddress - fixupAddr - 2) / 2;
                    int originalMachineCode = targetInst.getMachineCode();
                    // 10 bitlik offset'i makine kodunun alt 10 bitine yerleştir.
                    int newMachineCode = (originalMachineCode & 0xFC00) | (offset & 0x03FF);
                    targetInst.setMachineCode(newMachineCode);
                    break;
            }
        }
        
        // --- 5. Adım: Bellek segmentlerini oluştur ve yaz ---
        // Nihai adreslere göre sıralanmış komut listesini kullanarak,
        // .text, .data gibi son bellek segmentlerini oluştur.
        finalInstructions.sort(Comparator.comparingInt(Instruction::getAddress));
        Map<String, List<Instruction>> finalInstructionsBySection = finalInstructions.stream()
                .collect(Collectors.groupingBy(Instruction::getSection));

        for (String sectionName : sortedSectionNames) {
            List<Instruction> sectionInstructions = finalInstructionsBySection.get(sectionName);
            if (sectionInstructions == null || sectionInstructions.isEmpty()) continue;
            
            int origin = sectionInstructions.get(0).getAddress();
            int maxAddr = sectionInstructions.stream().mapToInt(i -> i.getAddress() + calculateInstructionSize(i)).max().getAsInt();
            int length = maxAddr - origin;
            
            MemorySegment segment = new MemorySegment(sectionName, origin, length);
            segments.put(sectionName, segment);

            for (Instruction inst : sectionInstructions) {
                writeInstructionToSegment(segment, inst);
            }
        }
    }
    
    /**
     * Verilen adresi içeren komutu bulur.
     * Bir relocation entry'sinin adresi, genellikle komutun kendisinin değil,
     * komutun içindeki (veya hemen sonrasındaki) değiştirilecek bir 'extra word'ün adresidir.
     * Bu metot, bu adresi kapsayan doğru komutu bulur.
     */
    private Instruction findInstructionToPatch(List<Instruction> instructions, int address) {
        // fixup adresi bir extraWord'ün adresidir, instruction'ın değil.
        // Adresi 'address' olan extraWord'ü içeren instruction'ı bul.
        // Bu daha sağlam bir implementasyon.
        for(Instruction inst : instructions) {
            int startAddr = inst.getAddress();
            int size = calculateInstructionSize(inst);
            int endAddr = startAddr + size;
            
            // Adres, bu komutun başlangıcına eşit veya büyük OLMALI ve bitiş adresinden önce olmalı.
            if (address >= startAddr && address < endAddr) {
                return inst;
            }
        }
        return null; // bulunamadı
    }

    private void writeInstructionToSegment(MemorySegment mem, Instruction inst) {
        int offset = inst.getAddress() - mem.getOrigin();
        int currentOffset = offset;
        
        boolean isDataDirective = (inst.getMachineCode() == null || inst.getMachineCode() == 0) &&
                (!inst.getExtraWords().isEmpty() || !inst.getExtraBytes().isEmpty());

        if (!isDataDirective && inst.getMachineCode() != null) {
            mem.writeWord(currentOffset, inst.getMachineCode());
            currentOffset += 2;
        }

        if (inst.getExtraWords() != null) {
            for (int extra : inst.getExtraWords()) {
                mem.writeWord(currentOffset, extra);
                currentOffset += 2;
            }
        }

        if (inst.getExtraBytes() != null) {
            for (int b : inst.getExtraBytes()) {
                mem.writeByte(currentOffset, (byte) (b & 0xFF));
                currentOffset += 1;
            }
        }
    }

    private int calculateInstructionSize(Instruction inst) {
        int size = 0;

        // An instruction from an object file is either:
        // 1. A real instruction with a 2-byte machine code.
        // 2. A data directive (.word, .byte, .string) which may have a machine code of 0 but has extra words/bytes.
        // 3. A pure label definition, which has no machine code and no extras. These have zero size.
        
        boolean hasMachineCode = inst.getMachineCode() != null && inst.getMachineCode() != 0;
        boolean hasExtras = !inst.getExtraWords().isEmpty() || !inst.getExtraBytes().isEmpty();
        
        if (!hasMachineCode && !hasExtras) {
            // This is a pure label definition (e.g., "label:"). It has zero size.
            return 0; 
        }

        // A data directive is identified by having extras but no non-zero machine code.
        boolean isDataDirective = !hasMachineCode && hasExtras;

        if (!isDataDirective) {
             // It's a regular instruction, occupies 2 bytes for its main opcode.
            size += 2;
        }

        size += inst.getExtraWords().size() * 2;
        size += inst.getExtraBytes().size();

        return size;
    }

    public static class SectionContribution {
        public final String sourceFile;
        public final String sectionName;
        public final int origin;
        public final int length;
        public int finalAddress; // Linker tarafından doldurulacak

        public SectionContribution(String sourceFile, String sectionName, int origin, int length) {
            this.sourceFile = sourceFile;
            this.sectionName = sectionName;
            this.origin = origin;
            this.length = length;
        }
    }

    public Map<String, MemorySegment> getSegments() {
        return segments;
    }

    public List<SectionContribution> getSectionContributions() {
        return Collections.unmodifiableList(sectionContributions);
    }

    public SymbolTable getSymbolTable() {
        return finalSymbolTable;
    }
}
