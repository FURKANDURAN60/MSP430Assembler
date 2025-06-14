package assembler;

import java.util.List;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import assembler.ExecutableWriter;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Kullanım: java assembler.Main <assembly_dosyasi.asm>");
            return;
        }

        String sourceFile = args[0];
        System.out.println("Çalışma dizini: " + new File(".").getAbsolutePath());

        System.out.println("""
                [Pass 1] Sembol tablosu ve Instruction listesi oluşturuluyor...
                ---------------------------------------------------------------
                """);

        PassOne passOne = new PassOne();
        passOne.processFile(sourceFile);

        List<Instruction> instructions = passOne.getInstructions();
        SymbolTable symbolTable = passOne.getSymbolTable();

        System.out.println("[Pass 1 Sonucu] Sembol Tablosu:\n");
        symbolTable.printSymbolTable();

        System.out.println("""
                [Pass 2] Makine kodu üretiliyor...
                ----------------------------------
                """);

        PassTwo passTwo = new PassTwo(symbolTable);
        passTwo.assemble(instructions);

        System.out.println("\n[Assembler tamamlandı.]");

        String baseName = new File(sourceFile).getName().replaceFirst("[.][^.]+$", "");
        File objDir = new File("obj");
        if (!objDir.exists()) objDir.mkdirs();
        String objFile = new File(objDir, baseName + ".obj").getPath();

        // Write object file
        try {
            System.out.println("Object dosyası oluşturuluyor: " + new File(objFile).getAbsolutePath());
            ObjectFileWriter.writeJson(objFile, instructions, symbolTable, passTwo.getRelocationTable());
            System.out.println("[.obj dosyası oluşturuldu: " + objFile + "]");
            
            // Dosyanın gerçekten oluşturulup oluşturulmadığını kontrol et
            File f = new File(objFile);
            if (f.exists()) {
                System.out.println("Dosya boyutu: " + f.length() + " bytes");
            } else {
                System.out.println("HATA: Dosya oluşturulamadı!");
            }
        } catch (IOException e) {
            System.err.println("Hata: .obj dosyası yazılamadı: " + e.getMessage());
            e.printStackTrace(); // Stack trace'i göster
            return;
        }

        System.out.println("""
                [Linker] Program bağlanıyor...
                ----------------------------
                """);

        try {
            // 1. obj/ klasöründen tüm .obj dosyalarını oku
            File[] objFiles = objDir.listFiles((dir, name) -> name.endsWith(".obj"));
            if (objFiles == null || objFiles.length == 0) {
                System.err.println("HATA: obj/ klasöründe .obj dosyası bulunamadı!");
                return;
            }

            List<String> paths = new ArrayList<>();
            for (File f : objFiles) {
                System.out.println("[Linker] .obj dosyası bulundu: " + f.getName());
                paths.add(f.getPath());
            }

            // 2. Hepsini birleştirerek linker'a ver
            ObjectFileReader.ObjData data = ObjectFileReader.readMultiple(paths);
            Linker linker = new Linker(data.instructions, data.symbolTable, data.relocations);
            linker.link();

            System.out.println("\n[Linkleme tamamlandı.]");

            for (Map.Entry<String, MemorySegment> entry : linker.getSegments().entrySet()) {
                System.out.println("\n[" + entry.getKey() + "] Segment İçeriği:");
                System.out.println("--------------------------");
                System.out.println(entry.getValue().toHexDump());
            }

            ExecutableWriter.writeTiTxt(linker.getSegments(), "linked.txt");

        } catch (IOException e) {
            System.err.println("Hata: .obj dosyası okunamadı veya linklenemedi: " + e.getMessage());
            e.printStackTrace(); // Stack trace'i göster
            return;
        }
    }

}
