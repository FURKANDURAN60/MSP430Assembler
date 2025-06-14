package assembler;

import java.io.*;
import java.util.List;
import java.util.Map;

public class ObjectFileWriter {

    public static void writeJson(String filename,
                                 List<Instruction> instructions,
                                 SymbolTable symbolTable,
                                 List<RelocationEntry> relocations) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filename))) {
            w.write("{\n");

            // 1) Instructions
            w.write("  \"instructions\": [\n");
            for (int i = 0; i < instructions.size(); i++) {
                Instruction inst = instructions.get(i);
                Integer machineCode = inst.getMachineCode();

                // For directives that have no machine code, use 0x0000 as a placeholder in the JSON
                int codeToWrite = (machineCode != null) ? machineCode : 0;

                w.write(String.format(
                        "    {\"section\":\"%s\",\"address\":\"0x%04X\",\"machineCode\":\"0x%04X\",\"extraWords\":%s,\"extraBytes\":%s,\"rawLine\":\"%s\"}%s\n",
                        inst.getSection(),
                        inst.getAddress(),
                        codeToWrite,
                        extraWordsToJson(inst.getExtraWords()),
                        extraBytesToJson(inst.getExtraBytes()),
                        inst.getRawLine().replace("\"", "\\\""),
                        i + 1 < instructions.size() ? "," : ""
                ));
            }
            w.write("  ],\n");

            // 2) Symbols
            w.write("  \"symbols\": [\n");
            Map<String, SymbolTable.SymbolEntry> syms = symbolTable.getAllSymbols();
            int si = 0, sn = syms.size();
            for (SymbolTable.SymbolEntry e : syms.values()) {
                w.write(String.format(
                        "    {\"name\":\"%s\",\"address\":\"0x%04X\",\"binding\":\"%s\",\"defined\":%b,\"section\":\"%s\"}%s\n",
                        e.getLabel(), e.getAddress(), e.getBinding(), e.isDefined(), e.getSection() == null ? "null" : e.getSection(),
                        ++si < sn ? "," : ""
                ));
            }
            w.write("  ],\n");

            // 3) Relocations
            w.write("  \"relocations\": [\n");
            for (int i = 0; i < relocations.size(); i++) {
                RelocationEntry re = relocations.get(i);
                w.write(String.format(
                        "    {\"symbol\":\"%s\",\"address\":\"0x%04X\",\"type\":\"%s\"}%s\n",
                        re.getSymbol(), re.getAddress(), re.getType().name(),
                        i + 1 < relocations.size() ? "," : ""
                ));
            }
            w.write("  ]\n");

            w.write("}\n");
        }
    }

    private static String extraWordsToJson(List<Integer> extraWords) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < extraWords.size(); i++) {
            sb.append(String.format("\"0x%04X\"", extraWords.get(i)));
            if (i + 1 < extraWords.size()) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String extraBytesToJson(List<Integer> extraBytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < extraBytes.size(); i++) {
            sb.append(String.format("\"0x%02X\"", extraBytes.get(i)));
            if (i + 1 < extraBytes.size()) sb.append(",");
                        }
        sb.append("]");
        return sb.toString();
    }
}
