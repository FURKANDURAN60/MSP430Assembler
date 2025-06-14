package assembler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import assembler.SymbolTable.SymbolEntry;

public class PassTwo {

    private final SymbolTable symbolTable;
    private final OpcodeTable opcodeTable;
    private final StringBuilder outputBuilder = new StringBuilder();
    private final List<RelocationEntry> relocationTable = new ArrayList<>();




    public String getFormattedOutput() {
        return outputBuilder.toString();
    }

    public List<RelocationEntry> getRelocationTable() {
        return Collections.unmodifiableList(relocationTable);
    }

    public PassTwo(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.opcodeTable = new OpcodeTable();
    }

    public void assemble(List<Instruction> instructions) {
        int lineCounter = 0;
        outputBuilder.setLength(0);
        outputBuilder.append(String.format("%-5s %-6s %-8s %s\n", "Line", "SPC", "Code", "Assembly"));
        outputBuilder.append("----- ------ -------- ------------------------\n");

        for (Instruction inst : instructions) {
            // Sadece etiket olan satırları atla (örn: "ETIKET:")
            if (inst.getMnemonic() == null && inst.getOperandString() == null && inst.getLabel() != null) {
                // Etiket tanımlarının kendisi listelemede bir satır kaplamaz,
                // bir sonraki komutun SPC'si ile birlikte gösterilirler.
                continue;
            }
             if (inst.getMnemonic() == null) continue; // Genel güvenlik kontrolü


            TransformedInstruction ti = transformPseudoInstruction(inst);
            int addr = inst.getAddress();

            if (ti.mnemonic.startsWith(".")) {
                // .usect çıktıda görünmemeli — sadece yer ayırır
                if (ti.mnemonic.equalsIgnoreCase(".usect")) {
                    // Çıktı üretilmeden atlanır
                    continue;
                }

// .sect çıktıda görünmesin — sadece section değiştirir
                if (ti.mnemonic.equalsIgnoreCase(".sect")) {
                    // Genellikle sadece section adı içerir, çıktı üretilmez
                    continue;
                }

                if (ti.mnemonic.equalsIgnoreCase(".space")) {
                    int size = resolveValue(ti.operands.trim());
                    outputBuilder.append(String.format("%-5d %04X            (space %d bytes)\n",
                            lineCounter++, addr, size));
                }
                else if (ti.mnemonic.equalsIgnoreCase(".string")) {
                    String str = ti.operands.trim();
                    if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
                        str = str.substring(1, str.length() - 1); // Tırnakları çıkar
                        for (int i = 0; i < str.length(); i++) {
                            char c = str.charAt(i);
                            outputBuilder.append(String.format("%-5d %04X   %02X       '%c'\n",
                                    lineCounter++, addr++, (int) c, c));
                            inst.getExtraBytes().add((int) c & 0xFF);
                        }
                        // NULL terminator
                        outputBuilder.append(String.format("%-5d %04X   %02X       '\\0'\n",
                                lineCounter++, addr++, 0));
                        inst.getExtraBytes().add(0);
                    } else {
                        outputBuilder.append(String.format("%-5d %04X   HATA     Geçersiz .string literal\n",
                                lineCounter++, addr));
                    }
                }

                else if (ti.mnemonic.equalsIgnoreCase(".float")) {
                    try {
                        float floatVal;

                        // Sembolse çöz, değilse direkt float olarak parse et
                        if (symbolTable.contains(ti.operands.trim())) {
                            int val = symbolTable.getAddress(ti.operands.trim());
                            floatVal = (float) val;
                        } else {
                            floatVal = Float.parseFloat(ti.operands.trim());
                        }

                        int floatBits = Float.floatToIntBits(floatVal);
                        for (int i = 0; i < 4; i++) {
                            int byteVal = (floatBits >> (i * 8)) & 0xFF;
                            outputBuilder.append(String.format("%-5d %04X   %02X\n", lineCounter++, addr++, byteVal));
                            inst.getExtraBytes().add(byteVal & 0xFF);
                        }
                    } catch (Exception e) {
                        outputBuilder.append(String.format("%-5d %04X   HATA     .float değeri geçersiz\n",
                                lineCounter++, addr));
                    }
                }
                else if (ti.mnemonic.equalsIgnoreCase(".word")) {
                    String[] values = ti.operands.split(",");
                    for (String val : values) {
                        int wordVal = resolveValue(val.trim());
                        outputBuilder.append(String.format("%-5d %04X   %04X\n",
                                lineCounter++, addr, wordVal & 0xFFFF));
                        inst.getExtraWords().add(wordVal & 0xFFFF);
                        addr += 2;
                    }
                }

                else if (ti.mnemonic.equalsIgnoreCase(".byte")) {
                    String[] values = ti.operands.split(",");
                    for (String val : values) {
                        int byteVal = resolveValue(val.trim());
                        outputBuilder.append(String.format("%-5d %04X   %02X\n",
                                lineCounter++, addr, byteVal & 0xFF));
                        inst.getExtraBytes().add(byteVal & 0xFF);
                        addr += 1;
                    }
                }

                else if (ti.mnemonic.equalsIgnoreCase(".resw")) {
                    int wordCount = resolveValue(ti.operands.trim());
                    for (int i = 0; i < wordCount; i++) {
                        outputBuilder.append(String.format("%-5d %04X   0000\n",
                                lineCounter++, addr));
                        addr += 2;
                    }
                }


                else {
                    outputBuilder.append(String.format("%-5d %04X            %s %s\n",
                            lineCounter++, addr, ti.mnemonic, ti.operands));
                }
                continue;
            }

            try {
                int machineCode = generateMachineCode(inst, ti);
                inst.setMachineCode(machineCode);

                int nextAddr = addr + 2;
                outputBuilder.append(String.format("%-5d %04X   %04X     %s\n",
                        lineCounter++, addr, machineCode, inst.getRawLine()));

                if (ti.format == 1) {
                    String[] ops = ti.operands.split(",");
                    OperandInfo src = parseOperand(ops[0].trim());
                    OperandInfo dst = parseOperand(ops[1].trim());

                    if (src.requiresExtraWord) {
                        outputBuilder.append(String.format("%-5d %04X   %04X\n",
                                lineCounter++, nextAddr, src.extraValue & 0xFFFF));
                        inst.getExtraWords().add(src.extraValue & 0xFFFF);
                        
                        String potentialSymbol = getSymbolFromOperand(ops[0].trim());
                        if (potentialSymbol != null) {
                            relocationTable.add(new RelocationEntry(potentialSymbol, nextAddr, RelocationEntry.RelocationType.ABSOLUTE_16BIT));
                        }

                        nextAddr += 2;
                    }

                    if (dst.requiresExtraWord) {
                        outputBuilder.append(String.format("%-5d %04X   %04X\n",
                                lineCounter++, nextAddr, dst.extraValue & 0xFFFF));
                        inst.getExtraWords().add(dst.extraValue & 0xFFFF);
                        
                        String potentialSymbol = getSymbolFromOperand(ops[1].trim());
                        if (potentialSymbol != null) {
                            relocationTable.add(new RelocationEntry(potentialSymbol, nextAddr, RelocationEntry.RelocationType.ABSOLUTE_16BIT));
                        }
                    }

                } else if (ti.format == 2) {
                    OperandInfo op = ti.operands.trim().isEmpty() ? null : parseOperand(ti.operands.trim());
                    if (op != null && op.requiresExtraWord) {
                        outputBuilder.append(String.format("%-5d %04X   %04X\n",
                                lineCounter++, addr + 2, op.extraValue & 0xFFFF));
                        inst.getExtraWords().add(op.extraValue & 0xFFFF);
                        
                        String potentialSymbol = getSymbolFromOperand(ti.operands.trim());
                        if (potentialSymbol != null) {
                            relocationTable.add(new RelocationEntry(potentialSymbol, addr + 2, RelocationEntry.RelocationType.ABSOLUTE_16BIT));
                        }
                    }

                } else if (ti.format == 3) {
                     String potentialSymbol = getSymbolFromOperand(ti.operands.trim());
                     if (potentialSymbol != null) {
                         // JMP komutları için adres farkı PC-göreli olarak hesaplanır
                         // ama relocation tablosuna yine de sembolün kendisi eklenmeli
                         // ki linker nihai adresi bilsin.
                         relocationTable.add(new RelocationEntry(potentialSymbol, addr, RelocationEntry.RelocationType.PC_RELATIVE_10BIT)); // Not: JMP'nin adresi
                     }
                }

            } catch (Exception e) {
                outputBuilder.append(String.format("%-5d %04X   HATA     %s\n",
                        lineCounter++, addr, e.getMessage()));
            }
        }
    }

    private int generateMachineCode(Instruction inst, TransformedInstruction ti) {
        if (ti.mnemonic.equalsIgnoreCase("NOP")) return 0x4303;
        if (ti.mnemonic.equalsIgnoreCase("RETI") && ti.operands.trim().isEmpty()) return 0x1300;

        String opcodeBinary = opcodeTable.getOpcode(ti.mnemonic);

        switch (ti.format) {
            case 1:
                String[] ops = ti.operands.split(",");
                if (ops.length != 2)
                    throw new IllegalArgumentException("Format 1 komutunda 2 operand olmalı.");

                OperandInfo src = parseOperand(ops[0].trim());
                OperandInfo dst = parseOperand(ops[1].trim());

                return (Integer.parseInt(opcodeBinary, 2) << 12)
                        | (src.reg << 8)
                        | (dst.ad << 7)
                        | (ti.bw << 6)
                        | (src.ad << 4)
                        | dst.reg;

            case 2:
                OperandInfo op = parseOperand(ti.operands.trim());
                return (Integer.parseInt(opcodeBinary, 2) << 7)
                        | (op.ad << 4)
                        | op.reg;

            case 3:
                int offset = resolveJumpOffset(ti.operands.trim(), inst.getAddress());
                return (Integer.parseInt(opcodeBinary, 2) << 10) | (offset & 0x03FF);

            default:
                throw new IllegalArgumentException("Geçersiz format: " + ti.format);
        }
    }

    private int resolveJumpOffset(String label, int currentAddr) {
        // Eğer sembol ref ise, offset'i 0 olarak kabul et. Linker düzeltecek.
        if (isRefSymbol(label)) {
            return 0;
        }
        int targetAddr = symbolTable.getAddress(label);
        return (targetAddr - currentAddr - 2) / 2;
    }

    private OperandInfo parseOperand(String operand) {
        String trimmedOperand = operand.trim();
        // Macro'dan gelen ##count gibi durumları ele al
        if (trimmedOperand.startsWith("##")) {
            trimmedOperand = trimmedOperand.substring(1);
        }
        String upperOperand = trimmedOperand.toUpperCase();

        // Adresleme Modu: Register Direct (Rn) -> ad=0
        if (RegisterTable.isRegister(upperOperand)) {
            return new OperandInfo(RegisterTable.getRegisterNumber(upperOperand), 0);
        }

        // Adresleme Modu: Register Indirect Auto-increment (@Rn+) -> ad=3
        if (upperOperand.startsWith("@") && upperOperand.endsWith("+")) {
            String reg = upperOperand.substring(1, upperOperand.length() - 1);
            return new OperandInfo(RegisterTable.getRegisterNumber(reg), 3);
        }

        // Adresleme Modu: Register Indirect (@Rn) -> ad=2
        if (upperOperand.startsWith("@")) {
            String reg = upperOperand.substring(1);
            return new OperandInfo(RegisterTable.getRegisterNumber(reg), 2);
        }

        // Adresleme Modu: Immediate (#imm)
        if (trimmedOperand.startsWith("#")) {
            String imm = trimmedOperand.substring(1);

            // Eğer sembol bir dış referans (.ref) ise, linker'ın çözmesi için
            // her zaman bir ekstra kelime gerekir. Bu, başlangıçta adresi 0 olan REF sembollerinin
            // #0 sabitiyle karıştırılmasını önler.
            if (isRefSymbol(imm)) {
                OperandInfo info = new OperandInfo(0, 3); // Immediate mode: @PC+
                info.requiresExtraWord = true;
                info.extraValue = 0; // Değeri linker dolduracak
                return info;
            }

            int value = resolveValue(imm);

            // Sabit Sayı Üreteci (Constant Generator) Kontrolü
            // MSP430, bazı küçük sabitleri (0, 1, 2, 4, 8, -1) ekstra kelime kullanmadan
            // doğrudan makine koduna gömebilir. Bu, kod boyutundan tasarruf sağlar.
            switch(value) {
                case 0:  return new OperandInfo(3, 0); // Mode 0, R3 (CG) -> #0
                case 1:  return new OperandInfo(3, 1); // Mode 1, R3 (CG) -> #1
                case 2:  return new OperandInfo(3, 2); // Mode 2, R3 (CG) -> #2
                case -1: return new OperandInfo(3, 3); // Mode 3, R3 (CG) -> #-1
                case 4:  return new OperandInfo(2, 2); // Mode 2, R2 (SR) -> #4
                case 8:  return new OperandInfo(2, 3); // Mode 3, R2 (SR) -> #8
            }

            // Eğer sabit sayı üreteci ile üretilemiyorsa, standart anlık mod kullanılır (PC-relative).
            // Bu mod, komuttan sonra bir ekstra kelime (extraWord) gerektirir.
            OperandInfo info = new OperandInfo(0, 3); // PC (R0) ile mod 3 -> @PC+
            info.requiresExtraWord = true;
            info.extraValue = value;
            return info;
        }

        // Adresleme Modu: Indexed (x(Rn)) -> ad=1
        if (trimmedOperand.contains("(") && trimmedOperand.contains(")")) {
            Pattern pattern = Pattern.compile("(.*)\\((R[0-9]+)\\)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(trimmedOperand);
            if (matcher.matches()) {
                int offset = resolveValue(matcher.group(1).trim());
                String reg = matcher.group(2).toUpperCase();
                OperandInfo info = new OperandInfo(RegisterTable.getRegisterNumber(reg), 1);
                info.requiresExtraWord = true;
                info.extraValue = offset;
                return info;
            }
        }

        // Adresleme Modu: Absolute (&addr) -> ad=1, R2 (SR) ile
        if (trimmedOperand.startsWith("&")) {
            int address = resolveValue(trimmedOperand.substring(1));
            OperandInfo info = new OperandInfo(2, 1); // SR (R2) ile mod 1 -> &etiket
            info.requiresExtraWord = true;
            info.extraValue = address;
            return info;
        }

        // Adresleme Modu: Symbolic (etiket) -> ad=1, PC (R0) ile
        // PC-relative adreslemedir.
        OperandInfo info = new OperandInfo(0, 1); // PC (R0) ile mod 1 -> etiket(PC)
        info.requiresExtraWord = true;
        info.extraValue = resolveValue(trimmedOperand);
        return info;
    }

    private int resolveValue(String operand) {
        return LiteralResolver.resolve(operand, symbolTable);
    }

    private boolean isCharLiteral(String s) {
        return s.matches("^'.'$");
    }


    private boolean isNumeric(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String tryResolveSymbol(int address) {
        for (String label : symbolTable.getAllSymbols().keySet()) {
            SymbolEntry entry = symbolTable.getAllSymbols().get(label);
            if (entry.getAddress() == address) {
                return label;
            }
        }
        return null;
    }

    private boolean isRefSymbol(String label) {
        if (label == null) return false;
        if (!symbolTable.contains(label)) return false;
        SymbolEntry e = symbolTable.getAllSymbols().get(label);
        return e.getBinding() == SymbolEntry.Binding.REF;
    }

    private String getSymbolFromOperand(String operand) {
        operand = operand.trim();
        if (operand.startsWith("##")) {
            operand = operand.substring(1);
        }

        // Handle #symbol, &symbol
        if (operand.startsWith("#") || operand.startsWith("&")) {
            String potentialSymbol = operand.substring(1);
            // Check if it's a pure symbol name, not a number or complex expression
            if (symbolTable.contains(potentialSymbol) && !isNumeric(potentialSymbol)) {
                return potentialSymbol;
            }
            return null;
        }

        // Handle symbol(Rx)
        if (operand.contains("(") && operand.endsWith(")")) {
            String potentialSymbol = operand.split("\\(")[0].trim();
            if (symbolTable.contains(potentialSymbol) && !isNumeric(potentialSymbol)) {
                return potentialSymbol;
            }
            return null;
        }
        
        // Handle direct symbol
        if (symbolTable.contains(operand) && !isNumeric(operand)) {
            return operand;
        }

        return null; // Not a relocatable symbol
    }

    private static class OperandInfo {
        int reg;
        int ad;
        boolean requiresExtraWord;
        int extraValue;

        public OperandInfo(int reg, int ad) {
            this.reg = reg;
            this.ad = ad;
            this.requiresExtraWord = false;
            this.extraValue = 0;
        }
    }

    // PassOne'un da kullanabilmesi için public ve static yapıldı
    public static record TransformedInstruction(String mnemonic, String operands, int format, int bw) {}

    public static TransformedInstruction transformPseudoInstruction(Instruction inst) {
        String mnemonic = inst.getMnemonic();
        String operands = inst.getOperandString() == null ? "" : inst.getOperandString();
        int format = inst.getFormat();
        int bw = inst.getBw();

        if (mnemonic.equalsIgnoreCase("JMP") || mnemonic.equalsIgnoreCase("BR")) {
            return new TransformedInstruction("MOV", "#" + operands.trim() + "," + "R0", 1, bw);
        } else if (mnemonic.equalsIgnoreCase("CLR") || mnemonic.equalsIgnoreCase("CLRW")) {
            return new TransformedInstruction("MOV", "#0," + operands, 1, bw);
        } else if (mnemonic.equalsIgnoreCase("CLRB")) {
            return new TransformedInstruction("MOV.B", "#0," + operands, 1, 1);
        } else if (mnemonic.equalsIgnoreCase("INC") || mnemonic.equalsIgnoreCase("INCW")) {
            return new TransformedInstruction("ADD", "#1," + operands, 1, bw);
        } else if (mnemonic.equalsIgnoreCase("INCB")) {
            return new TransformedInstruction("ADD.B", "#1," + operands, 1, 1);
        } else if (mnemonic.equalsIgnoreCase("DEC") || mnemonic.equalsIgnoreCase("DECW")) {
            return new TransformedInstruction("SUB", "#1," + operands, 1, bw);
        } else if (mnemonic.equalsIgnoreCase("DECB")) {
            return new TransformedInstruction("SUB.B", "#1," + operands, 1, 1);
        } else if (mnemonic.equalsIgnoreCase("TST") || mnemonic.equalsIgnoreCase("TSTW")) {
            return new TransformedInstruction("CMP", operands.trim() + "," + operands.trim(), 1, bw);
        } else if (mnemonic.equalsIgnoreCase("TSTB")) {
            return new TransformedInstruction("CMP.B", operands.trim() + "," + operands.trim(), 1, 1);
        }

        // Handle .B/.W in mnemonic
        if (mnemonic.endsWith(".B")) {
            bw = 1;
            mnemonic = mnemonic.substring(0, mnemonic.length() - 2);
        } else if (mnemonic.endsWith(".W")) {
            bw = 0;
            mnemonic = mnemonic.substring(0, mnemonic.length() - 2);
        }

        return new TransformedInstruction(mnemonic, operands, format, bw);
    }
}