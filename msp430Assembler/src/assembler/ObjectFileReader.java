package assembler;

import java.io.*;
import java.util.*;
import org.json.*;

public class ObjectFileReader {
    public static class ObjData {
      public final List<Instruction> instructions;
      public final SymbolTable symbolTable;
      public final List<RelocationEntry> relocations;
      public ObjData(List<Instruction> i, SymbolTable s, List<RelocationEntry> r) {
        instructions=i; symbolTable=s; relocations=r;
      }
    }

    public static ObjData readJson(String filename) throws IOException {
        String txt;
        try (BufferedReader r = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line=r.readLine())!=null) sb.append(line);
            txt = sb.toString();
        }
        JSONObject root = new JSONObject(txt);

        // 1) Symbols
        SymbolTable symTab = new SymbolTable();
        JSONArray symArr = root.getJSONArray("symbols");
        for (int i=0; i<symArr.length(); i++) {
            JSONObject o = symArr.getJSONObject(i);
            String name = o.getString("name");
            String addrStr = o.getString("address").replace("0x", "");
            int addr = (int)Long.parseLong(addrStr, 16);
            SymbolTable.SymbolEntry.Binding b =
              SymbolTable.SymbolEntry.Binding.valueOf(o.getString("binding"));

            String section = o.has("section") && !o.getString("section").equals("null") ? o.getString("section") : null;

            if (symTab.contains(name)) { // Sembol zaten varsa (muhtemelen REF)
                 SymbolTable.SymbolEntry existing = symTab.getAllSymbols().get(name);
                 existing.setAddress(addr);
                 existing.setBinding(b);
                 existing.setSection(section);
            } else {
                 symTab.define(name, addr, b);
                 symTab.getAllSymbols().get(name).setSection(section);
            }

            // Backwards compatibility: if "defined" field exists, read it.
            if (o.has("defined") && o.getBoolean("defined")) {
                symTab.getAllSymbols().get(name).setDefined(true);
            }
        }

        // 2) Instructions
        List<Instruction> insts = new ArrayList<>();
        JSONArray instArr = root.getJSONArray("instructions");
        for (int i=0; i<instArr.length(); i++) {
            JSONObject o = instArr.getJSONObject(i);
            String section = o.getString("section");
            String addrStr = o.getString("address").replace("0x", "");
            String codeStr = o.getString("machineCode").replace("0x", "").split(" ")[0];
            int address    = (int)Long.parseLong(addrStr, 16);
            int code       = (int)Long.parseLong(codeStr, 16);
            String raw     = o.getString("rawLine");
            Instruction inst = new Instruction(null, null, null, address, 0);
            inst.setSection(section);
            inst.setMachineCode(code);
            inst.setRawLine(raw);
            // extraWords oku
            if (o.has("extraWords")) {
                JSONArray extraArr = o.getJSONArray("extraWords");
                List<Integer> extraWords = new ArrayList<>();
                for (int j = 0; j < extraArr.length(); j++) {
                    String ew = extraArr.getString(j).replace("0x", "");
                    extraWords.add((int)Long.parseLong(ew, 16));
                }
                inst.setExtraWords(extraWords);
            }
            // extraBytes oku
            if (o.has("extraBytes")) {
                JSONArray extraArr = o.getJSONArray("extraBytes");
                List<Integer> extraBytes = new ArrayList<>();
                for (int j = 0; j < extraArr.length(); j++) {
                    String eb = extraArr.getString(j).replace("0x", "");
                    extraBytes.add(Integer.parseInt(eb, 16));
                }
                inst.setExtraBytes(extraBytes);
            }
            insts.add(inst);
        }

        // 3) Relocations
        List<RelocationEntry> rels = new ArrayList<>();
        JSONArray relArr = root.getJSONArray("relocations");
        for (int i=0; i<relArr.length(); i++) {
            JSONObject o = relArr.getJSONObject(i);
            String addrStr = o.getString("address").replace("0x", "");
            int address = (int)Long.parseLong(addrStr, 16);

            // Geriye dönük uyumluluk için, eğer `type` alanı yoksa,
            // bunun eski bir .obj dosyası olduğunu varsay ve mutlak adresleme kullan.
            RelocationEntry.RelocationType type = RelocationEntry.RelocationType.ABSOLUTE_16BIT;
            if (o.has("type")) {
                type = RelocationEntry.RelocationType.valueOf(o.getString("type"));
            }
            rels.add(new RelocationEntry(o.getString("symbol"), address, type));
        }

        return new ObjData(insts, symTab, rels);
    }

    public static ObjData readMultiple(List<String> filenames) throws IOException {
        List<Instruction> allInstructions = new ArrayList<>();
        SymbolTable combinedSymbolTable = new SymbolTable();
        List<RelocationEntry> allRelocations = new ArrayList<>();

        for (String file : filenames) {
            ObjData obj = readJson(file);
            String sourceObjName = new File(file).getName();

            // Her instruction'a kaynak dosya adını ekle
            for(Instruction inst : obj.instructions) {
                inst.setSourceFile(sourceObjName);
            }
            // Her sembole de kaynak dosya adını ekle
            for(SymbolTable.SymbolEntry sym : obj.symbolTable.getAllSymbols().values()) {
                sym.setSourceFile(sourceObjName);
            }

            // Instruction'ları ekle
            allInstructions.addAll(obj.instructions);

            // Sembolleri akıllıca birleştir
            for (Map.Entry<String, SymbolTable.SymbolEntry> entry : obj.symbolTable.getAllSymbols().entrySet()) {
                String label = entry.getKey();
                SymbolTable.SymbolEntry newEntry = entry.getValue();

                    if (combinedSymbolTable.contains(label)) {
                    SymbolTable.SymbolEntry existingEntry = combinedSymbolTable.getAllSymbols().get(label);
                    
                    if (newEntry.isDefined() && existingEntry.isDefined()) {
                        if (newEntry.getBinding() == SymbolTable.SymbolEntry.Binding.DEF && existingEntry.getBinding() == SymbolTable.SymbolEntry.Binding.DEF) {
                            throw new IllegalArgumentException("Linker Hatası: '" + label + "' sembolü birden fazla dosyada tanımlanmış.");
                        }
                    }
                    
                    if (newEntry.isDefined() && !existingEntry.isDefined()) {
                        existingEntry.setAddress(newEntry.getAddress());
                        existingEntry.setBinding(newEntry.getBinding());
                        existingEntry.setSection(newEntry.getSection());
                        existingEntry.setSourceFile(newEntry.getSourceFile());
                        existingEntry.setDefined(true);
                    }
                } else {
                    combinedSymbolTable.define(label, newEntry.getAddress(), newEntry.getBinding());
                    SymbolTable.SymbolEntry definedEntry = combinedSymbolTable.getAllSymbols().get(label);
                    definedEntry.setSection(newEntry.getSection());
                    definedEntry.setSourceFile(newEntry.getSourceFile());
                    if (newEntry.isDefined()) {
                        definedEntry.setDefined(true);
                    }
                }
            }

            // Relocation'ları ekle
            allRelocations.addAll(obj.relocations);
        }

        return new ObjData(allInstructions, combinedSymbolTable, allRelocations);
    }
} 