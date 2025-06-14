package assembler;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class AssemblerGUI {
    private static RSyntaxTextArea codeEditor;
    private static JTabbedPane outputTabbedPane;
    private static JTextArea listingArea, tiTxtArea, mapArea, logArea;
    private static JTree projectTree;
    private static JSplitPane mainSplitPane;
    private static int lastExplorerSize = 200;

    private static JButton compileButton, clearButton, copyButton, linkButton, createMapButton, loadButton;
    private static JToggleButton toggleExplorerButton; // Proje gezginini aç/kapa butonu
    private static JMenu themeMenu, languageMenu, fileMenu, helpMenu;
    private static JMenuItem lightMode, darkMode, turkishLang, englishLang, userGuideItem;
    private static JMenuItem openFile, saveFile, saveAsFile;
    private static JFrame frame;
    private static File currentFile = null;
    private static Linker lastSuccessfulLinker = null;
    private static String lastOutputBaseName = null;
    private static String currentLanguage = "tr";
    private static ImageIcon compileIcon, clearIcon, copyIcon, linkIcon, mapIcon, fileIcon, themeIcon, languageIcon, projectIcon, loadIcon, helpIcon;
    private static JPopupMenu projectPopup;
    private static JMenuItem compileSelected, linkSelected, refreshTree;
    private static MemoryView memoryView;

    public static void main(String[] args) {
        frame = new JFrame("MSP430 Assembler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

        JMenuBar menuBar = new JMenuBar();
        setupMenus(menuBar);

        // --- Ana Panel ---
        JPanel mainPanel = new JPanel(new BorderLayout());

        // --- Sağ Panel (Editör ve Çıktı) ---
        JPanel rightPanel = new JPanel(new BorderLayout());

        codeEditor = new RSyntaxTextArea(20, 50);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setText("; Dosya açmak için soldaki proje gezginini kullanın.");
        RTextScrollPane editorScrollPane = new RTextScrollPane(codeEditor);

        setupOutputTabs();
        memoryView = new MemoryView();
        setupButtons();
        setupActionListeners();
        loadAndSetIcons();

        JSplitPane editorMemorySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScrollPane, memoryView);
        editorMemorySplit.setResizeWeight(0.65);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(toggleExplorerButton);
        buttonPanel.add(compileButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(linkButton);
        buttonPanel.add(createMapButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(copyButton);

        JSplitPane editorOutputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorMemorySplit, outputTabbedPane);
        editorOutputSplit.setResizeWeight(0.6);

        rightPanel.add(editorOutputSplit, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- Sol Panel (Proje Gezgini) ---
        projectTree = new JTree();
        createProjectTree();
        JScrollPane treeScrollPane = new JScrollPane(projectTree);

        // --- Ana Ekran Bölmesi ---
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, rightPanel);
        mainSplitPane.setResizeWeight(0.2);
        mainSplitPane.setOneTouchExpandable(true); // Kolay aç/kapa için oklar
        mainSplitPane.setDividerLocation(0); // Başlangıçta gizli

        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        frame.setJMenuBar(menuBar);
        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);

        setLightMode(); // Set a default theme
        setLanguage(currentLanguage); // Set default language
        frame.setVisible(true);
        editorOutputSplit.setDividerLocation(0.5); // Açılışta tam ortala
    }

    private static void setupMenus(JMenuBar menuBar) {
        // File Menu
        fileMenu = new JMenu();
        openFile = new JMenuItem();
        saveFile = new JMenuItem();
        saveAsFile = new JMenuItem();
        fileMenu.add(openFile);
        fileMenu.add(saveFile);
        fileMenu.add(saveAsFile);
        menuBar.add(fileMenu);

        // Theme Menu
        themeMenu = new JMenu();
        lightMode = new JMenuItem();
        darkMode = new JMenuItem();
        themeMenu.add(lightMode);
        themeMenu.add(darkMode);
        menuBar.add(themeMenu);

        // Language Menu
        languageMenu = new JMenu();
        turkishLang = new JMenuItem();
        englishLang = new JMenuItem();
        languageMenu.add(turkishLang);
        languageMenu.add(englishLang);
        menuBar.add(languageMenu);

        // Help Menu
        helpMenu = new JMenu();
        userGuideItem = new JMenuItem();
        helpMenu.add(userGuideItem);
        menuBar.add(helpMenu);
    }

    private static void setupOutputTabs() {
        outputTabbedPane = new JTabbedPane();
        listingArea = new JTextArea();
        tiTxtArea = new JTextArea();
        mapArea = new JTextArea();
        logArea = new JTextArea();

        JTextArea[] textAreas = {listingArea, tiTxtArea, mapArea, logArea};
        for (JTextArea ta : textAreas) {
            ta.setEditable(false);
            ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        }

        outputTabbedPane.addTab("Assembler", new JScrollPane(listingArea));
        outputTabbedPane.addTab("TI-TXT", new JScrollPane(tiTxtArea));
        outputTabbedPane.addTab("Map", new JScrollPane(mapArea));
        outputTabbedPane.addTab("Log", new JScrollPane(logArea));
    }

    private static void setupButtons() {
        toggleExplorerButton = new JToggleButton();
        compileButton = new JButton();
        clearButton = new JButton();
        copyButton = new JButton();
        linkButton = new JButton();
        createMapButton = new JButton();
        loadButton = new JButton();

        // Proje gezgini butonu sadece ikon
        toggleExplorerButton.setPreferredSize(new Dimension(45, 35));

        // Diğer butonlar ikon ve metin içerecek
        Dimension buttonSize = new Dimension(130, 35);
        Dimension mapButtonSize = new Dimension(190, 35);
        compileButton.setPreferredSize(buttonSize);
        clearButton.setPreferredSize(buttonSize);
        linkButton.setPreferredSize(buttonSize);
        copyButton.setPreferredSize(buttonSize);
        createMapButton.setPreferredSize(mapButtonSize);
        loadButton.setPreferredSize(buttonSize); // Boyutunu ayarla
        createMapButton.setEnabled(false);
        loadButton.setEnabled(false); // Başlangıçta pasif
    }

    private static void setupActionListeners() {
        lightMode.addActionListener(e -> setLightMode());
        darkMode.addActionListener(e -> setDarkMode());
        turkishLang.addActionListener(e -> setLanguage("tr"));
        englishLang.addActionListener(e -> setLanguage("en"));
        userGuideItem.addActionListener(e -> showUserGuide());

        openFile.addActionListener(e -> openFileAction());
        saveFile.addActionListener(e -> saveFileAction(false));
        saveAsFile.addActionListener(e -> saveFileAction(true));

        toggleExplorerButton.addActionListener(e -> {
            if (toggleExplorerButton.isSelected()) {
                mainSplitPane.setDividerLocation(lastExplorerSize > 0 ? lastExplorerSize : 200);
            } else {
                lastExplorerSize = mainSplitPane.getDividerLocation();
                mainSplitPane.setDividerLocation(0);
            }
        });

        compileButton.addActionListener(e -> compileCodeAction());
        clearButton.addActionListener(e -> {
            listingArea.setText("");
            tiTxtArea.setText("");
            mapArea.setText("");
            logArea.setText("");
            createMapButton.setEnabled(false);
            loadButton.setEnabled(false);
            lastSuccessfulLinker = null;
        });
        linkButton.addActionListener(e -> linkObjectFilesAction());
        createMapButton.addActionListener(e -> createMapFileAction());
        loadButton.addActionListener(e -> loadIntoMemoryAction());
        copyButton.addActionListener(e -> {
            Component selectedComponent = outputTabbedPane.getSelectedComponent();
            if (selectedComponent instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) selectedComponent;
                JTextArea activeTextArea = (JTextArea) scrollPane.getViewport().getView();
                String outputText = activeTextArea.getText();
                if (outputText != null && !outputText.isEmpty()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(outputText), null);
                }
            }
        });
    }

    private static ImageIcon loadIcon(String path, int width, int height) {
        java.net.URL imgURL = AssemblerGUI.class.getResource(path);
        if (imgURL != null) {
            ImageIcon icon = new ImageIcon(imgURL);
            // Daha kaliteli ölçeklendirme için SCALE_AREA_AVERAGING kullan
            Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
            return new ImageIcon(image);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    private static void loadAndSetIcons() {
        int iconSize = 20; // 20x20 pixels
        int menuIconSize = 16; // 16x16 pixels for menus

        // Load icons using the class loader to find resources
        projectIcon = loadIcon("/resources/icons/file.png", iconSize, iconSize);
        compileIcon = loadIcon("/resources/icons/compile.png", iconSize, iconSize);
        clearIcon = loadIcon("/resources/icons/clear.png", iconSize, iconSize);
        copyIcon = loadIcon("/resources/icons/copy.png", iconSize, iconSize);
        fileIcon = loadIcon("/resources/icons/file.png", menuIconSize, menuIconSize);
        themeIcon = loadIcon("/resources/icons/theme.png", menuIconSize, menuIconSize);
        languageIcon = loadIcon("/resources/icons/language.png", menuIconSize, menuIconSize);
        helpIcon = loadIcon("/resources/icons/userGuide.png", menuIconSize, menuIconSize);
        linkIcon = loadIcon("/resources/icons/linker.png", iconSize, iconSize);
        mapIcon = loadIcon("/resources/icons/map.png", iconSize, iconSize);
        loadIcon = loadIcon("/resources/icons/compile.png", iconSize, iconSize); // Placeholder, change later

        // Set icons on components
        fileMenu.setIcon(fileIcon);
        themeMenu.setIcon(themeIcon);
        languageMenu.setIcon(languageIcon);
        helpMenu.setIcon(helpIcon);
        userGuideItem.setIcon(helpIcon);

        toggleExplorerButton.setIcon(projectIcon);
        compileButton.setIcon(compileIcon);
        clearButton.setIcon(clearIcon);
        copyButton.setIcon(copyIcon);
        linkButton.setIcon(linkIcon);
        createMapButton.setIcon(mapIcon);
        loadButton.setIcon(loadIcon);
    }

    private static void compileCodeAction() {
        if (currentFile == null) {
            logArea.setText("Derlemek için önce bir dosya açın veya kaydedin.");
            outputTabbedPane.setSelectedIndex(3);
            return;
        }
        try {
            // Editördeki içeriği dosyaya kaydet
            saveFileAction(false);
            // Dosyayı derle
            compileFile(currentFile);
        } catch (Exception ex) {
            logArea.setText("HATA: " + ex.getMessage() + "\n");
            ex.printStackTrace(new PrintStream(new JTextAreaOutputStream(logArea)));
            outputTabbedPane.setSelectedIndex(3);
        }
    }

    private static void compileSelectedFiles() {
        TreePath[] paths = projectTree.getSelectionPaths();
        if (paths == null) return;

        logArea.setText(""); // Logu temizle
        int successCount = 0;
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            FileNode fileNode = (FileNode) node.getUserObject();
            File file = fileNode.getFile();
            if (file.isFile() && file.getName().endsWith(".asm")) {
                try {
                    logArea.append("--- Derleniyor: " + file.getName() + " ---\n");
                    compileFile(file);
                    logArea.append("--- " + file.getName() + " başarıyla derlendi. ---\n\n");
                    successCount++;
                } catch (Exception e) {
                    logArea.append("### HATA: " + file.getName() + " derlenemedi! ###\n");
                    logArea.append(e.getMessage() + "\n\n");
                }
            }
        }
        logArea.append("Toplam " + successCount + " dosya derlendi.\n");
        outputTabbedPane.setSelectedIndex(3);
    }

    private static void compileFile(File sourceFile) throws IOException {
        logArea.append("Derleme işlemi başlatıldı: " + sourceFile.getPath() + "\n");
        PassOne passOne = new PassOne();
        passOne.processFile(sourceFile.getPath());
        List<Instruction> instructions = passOne.getInstructions();
        SymbolTable symbolTable = passOne.getSymbolTable();

        PassTwo passTwo = new PassTwo(symbolTable);
        passTwo.assemble(instructions);

        String output = passTwo.getFormattedOutput();
        listingArea.setText(output);
        outputTabbedPane.setSelectedIndex(0);
        logArea.append("Assembler listeleme çıktısı oluşturuldu.\n");

        String baseName = sourceFile.getName().replaceFirst("[.][^.]+$", "");
        File objDir = new File("obj");
        if (!objDir.exists()) objDir.mkdirs();
        File objFile = new File(objDir, baseName + ".obj");

        ObjectFileWriter.writeJson(objFile.getPath(), instructions, symbolTable, passTwo.getRelocationTable());
        logArea.append(".obj dosyası başarıyla oluşturuldu: " + objFile.getPath() + "\n");
    }

    private static void linkObjectFilesAction() {
        logArea.setText("[Linker] 'obj' klasöründeki tüm dosyalar linkleniyor...\n");
        File objDir = new File("obj/");
        File[] objFiles = objDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".obj"));

        if (objFiles == null || objFiles.length == 0) {
            logArea.append("HATA: 'obj/' klasöründe linklenecek .obj dosyası bulunamadı!");
            outputTabbedPane.setSelectedIndex(3);
            return;
        }

        List<File> fileList = new ArrayList<>(List.of(objFiles));
        linkFiles(fileList);
    }

    private static void linkSelectedFiles() {
        TreePath[] paths = projectTree.getSelectionPaths();
        if (paths == null) return;

        List<File> filesToLink = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            FileNode fileNode = (FileNode) node.getUserObject();
            File file = fileNode.getFile();
            if (file.isFile() && file.getName().endsWith(".obj")) {
                filesToLink.add(file);
            }
        }

        if (filesToLink.isEmpty()) {
            logArea.setText("Linklemek için .obj dosyası seçilmedi.");
            outputTabbedPane.setSelectedIndex(3);
            return;
        }

        linkFiles(filesToLink);
    }

    private static void linkFiles(List<File> objFiles) {
        logArea.setText("[Linker] İşlem başlatıldı...\n");
        lastSuccessfulLinker = null;
        createMapButton.setEnabled(false);
        loadButton.setEnabled(false);

        List<String> paths = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();
        for (File f : objFiles) {
            logArea.append("- Linklenecek dosya: " + f.getName() + "\n");
            paths.add(f.getPath());
            baseNames.add(f.getName().replaceFirst("[.][^.]+$", ""));
        }

        try {
            ObjectFileReader.ObjData data = ObjectFileReader.readMultiple(paths);
            logArea.append("[Linker] Tüm .obj dosyaları okundu ve birleştirildi.\n");

            Linker linker = new Linker(data.instructions, data.symbolTable, data.relocations);
            linker.link();
            logArea.append("[Linker] Semboller çözüldü ve bellek segmentleri oluşturuldu.\n");

            File tiTxtDir = new File("TI-TXT");
            if (!tiTxtDir.exists()) {
                tiTxtDir.mkdirs();
                logArea.append("-> 'TI-TXT/' klasörü oluşturuldu.\n");
            }
            lastOutputBaseName = String.join("-", baseNames);
            String outputFileName = lastOutputBaseName + ".txt";
            File outputFile = new File(tiTxtDir, outputFileName);

            ExecutableWriter.writeTiTxt(linker.getSegments(), outputFile.getPath());
            logArea.append("BAŞARILI: Linkleme tamamlandı! Çıktı: " + outputFile.getPath() + "\n");

            lastSuccessfulLinker = linker;
            createMapButton.setEnabled(true);
            loadButton.setEnabled(true);

            String content = new String(Files.readAllBytes(Paths.get(outputFile.getPath())));
            tiTxtArea.setText(content);
            outputTabbedPane.setSelectedIndex(1);

        } catch (Exception e) {
            logArea.append("\nHATA: Linkleme sırasında bir sorun oluştu!\n" + e.getMessage());
            e.printStackTrace(new PrintStream(new JTextAreaOutputStream(logArea)));
            outputTabbedPane.setSelectedIndex(3);
        }
    }

    private static void createMapFileAction() {
        if (lastSuccessfulLinker == null) {
            logArea.append("\nHATA: Map dosyası oluşturmak için önce başarılı bir linkleme işlemi yapılmalıdır.");
            outputTabbedPane.setSelectedIndex(3);
            return;
        }
        logArea.append("\n[Map Oluşturucu] İşlem başlatıldı...\n");
        try {
            File tiTxtDir = new File("TI-TXT");
            if (!tiTxtDir.exists()) tiTxtDir.mkdirs();

            String mapFileName = lastOutputBaseName + ".map";
            String tiTxtFileName = lastOutputBaseName + ".txt";
            File mapFile = new File(tiTxtDir, mapFileName);

            MapFileWriter.writeMapFile(mapFile.getPath(), tiTxtFileName, lastSuccessfulLinker);
            logArea.append("BAŞARILI: Map dosyası oluşturuldu! Çıktı: " + mapFile.getPath() + "\n");

            String content = new String(Files.readAllBytes(Paths.get(mapFile.getPath())));
            mapArea.setText(content);
            outputTabbedPane.setSelectedIndex(2);

        } catch (IOException e) {
            logArea.append("\nHATA: Map dosyası yazılırken bir sorun oluştu!\n" + e.getMessage());
            e.printStackTrace(new PrintStream(new JTextAreaOutputStream(logArea)));
            outputTabbedPane.setSelectedIndex(3);
        }
    }

    private static void loadIntoMemoryAction() {
        if (lastSuccessfulLinker == null) {
            logArea.append("\nHATA: Belleğe yüklemek için önce başarılı bir linkleme işlemi yapılmalıdır.");
            outputTabbedPane.setSelectedIndex(3);
            return;
        }
        logArea.append("\n[Loader] Bellek görüntüsü yükleniyor...\n");
        try {
            Loader loader = new Loader(lastSuccessfulLinker.getSegments());
            byte[] memoryImage = loader.getMemoryImage();
            memoryView.loadMemory(memoryImage);

            logArea.append("BAŞARILI: Program belleğe yüklendi. Bellek görünümü sağ tarafta güncellendi.\n");
            // Bellek görünümü artık her zaman görünür olduğu için sekmeyi değiştirmeye gerek yok.

        } catch (Exception e) {
            logArea.append("\nHATA: Belleğe yükleme sırasında bir sorun oluştu!\n" + e.getMessage());
            e.printStackTrace(new PrintStream(new JTextAreaOutputStream(logArea)));
            outputTabbedPane.setSelectedIndex(3);
        }
    }

    private static void setLightMode() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception e) { e.printStackTrace(); }

        Color lightBg = new Color(242, 242, 242);
        Color lightFg = Color.BLACK;
        codeEditor.setBackground(Color.WHITE);
        codeEditor.setForeground(lightFg);
        codeEditor.setCurrentLineHighlightColor(new Color(232, 242, 254));

        JTextArea[] textAreas = {listingArea, tiTxtArea, mapArea, logArea};
        for (JTextArea ta : textAreas) {
            ta.setBackground(lightBg);
            ta.setForeground(lightFg);
        }

        outputTabbedPane.setTitleAt(0, "Assembler");
        outputTabbedPane.setTitleAt(1, "TI-TXT");
        outputTabbedPane.setTitleAt(2, "Bellek Haritası");
        outputTabbedPane.setTitleAt(3, "Günlük");
        helpMenu.setText("Yardım");
        userGuideItem.setText("Kullanıcı Kılavuzu");
    }

    private static void setDarkMode() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception e) { e.printStackTrace(); }

        Color darkBg = new Color(43, 43, 43);
        Color darkFg = new Color(187, 187, 187);
        codeEditor.setBackground(darkBg);
        codeEditor.setForeground(darkFg);
        codeEditor.setCurrentLineHighlightColor(new Color(55, 55, 55));

        JTextArea[] textAreas = {listingArea, tiTxtArea, mapArea, logArea};
        for (JTextArea ta : textAreas) {
            ta.setBackground(darkBg);
            ta.setForeground(darkFg);
        }
    }

    private static void openFileAction() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                codeEditor.read(reader, null);
                frame.setTitle("MSP430 Assembler - " + currentFile.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Dosya okunamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
                currentFile = null;
            }
        }
    }

    private static void saveFileAction(boolean saveAs) {
        if (currentFile == null || saveAs) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
            } else {
                return;
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
            codeEditor.write(writer);
            frame.setTitle("MSP430 Assembler - " + currentFile.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Dosya kaydedilemedi!", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void setLanguage(String lang) {
        currentLanguage = lang;
        memoryView.setLanguage(lang); // Bellek görünümünün dilini de ayarla
        if (lang.equals("tr")) {
            frame.setTitle("MSP430 Assembler");
            themeMenu.setText("Tema");
            lightMode.setText("Açık Mod");
            darkMode.setText("Koyu Mod");
            languageMenu.setText("Dil Seçimi");
            turkishLang.setText("Türkçe");
            englishLang.setText("English");
            toggleExplorerButton.setToolTipText("Proje Gezginini Aç/Kapa");
            compileButton.setText("Derle");
            compileButton.setToolTipText("Derle");
            clearButton.setText("Temizle");
            clearButton.setToolTipText("Temizle");
            copyButton.setText("Kopyala");
            copyButton.setToolTipText("Kopyala");
            linkButton.setText("Linker");
            linkButton.setToolTipText("Linker");
            createMapButton.setText("Harita Oluştur");
            createMapButton.setToolTipText("Harita Oluştur");
            loadButton.setText("Yükle");
            loadButton.setToolTipText("Programı Belleğe Yükle");
            fileMenu.setText("Dosya");
            openFile.setText("Aç");
            saveFile.setText("Kaydet");
            saveAsFile.setText("Farklı Kaydet");
            outputTabbedPane.setTitleAt(0, "Assembler");
            outputTabbedPane.setTitleAt(1, "TI-TXT");
            outputTabbedPane.setTitleAt(2, "Bellek Haritası");
            outputTabbedPane.setTitleAt(3, "Günlük");
            helpMenu.setText("Yardım");
            userGuideItem.setText("Kullanıcı Kılavuzu");
        } else {
            frame.setTitle("MSP430 Assembler");
            themeMenu.setText("Theme");
            lightMode.setText("Light Mode");
            darkMode.setText("Dark Mode");
            languageMenu.setText("Language");
            turkishLang.setText("Turkish");
            englishLang.setText("English");
            toggleExplorerButton.setToolTipText("Toggle Project Explorer");
            compileButton.setText("Compile");
            compileButton.setToolTipText("Compile");
            clearButton.setText("Clear");
            clearButton.setToolTipText("Clear");
            copyButton.setText("Copy");
            copyButton.setToolTipText("Copy");
            linkButton.setText("Link");
            linkButton.setToolTipText("Link");
            createMapButton.setText("Generate Map");
            createMapButton.setToolTipText("Generate Map");
            loadButton.setText("Load");
            loadButton.setToolTipText("Load Program into Memory");
            fileMenu.setText("File");
            openFile.setText("Open");
            saveFile.setText("Save");
            saveAsFile.setText("Save As");
            outputTabbedPane.setTitleAt(0, "Assembler");
            outputTabbedPane.setTitleAt(1, "TI-TXT");
            outputTabbedPane.setTitleAt(2, "Map");
            outputTabbedPane.setTitleAt(3, "Log");
            helpMenu.setText("Help");
            userGuideItem.setText("User Guide");
        }
    }

    private static void createProjectTree() {
        File projectDir = new File(".");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode(projectDir));
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        projectTree.setModel(treeModel);
        projectTree.setCellRenderer(new FileTreeCellRenderer());

        createNodes(root, projectDir);

        // --- Sağ Tık Menüsü ---
        projectPopup = new JPopupMenu();
        compileSelected = new JMenuItem("Seçilenleri Derle");
        linkSelected = new JMenuItem("Seçilenleri Linkle");
        refreshTree = new JMenuItem("Yenile");

        // Set icons for the popup menu items right after creation
        compileSelected.setIcon(loadIcon("/resources/icons/compile.png", 16, 16));
        linkSelected.setIcon(loadIcon("/resources/icons/compile.png", 16, 16)); // TODO: Ayrı ikon
        refreshTree.setIcon(loadIcon("/resources/icons/clear.png", 16, 16)); // TODO: Ayrı ikon

        compileSelected.addActionListener(e -> compileSelectedFiles());
        linkSelected.addActionListener(e -> linkSelectedFiles());
        refreshTree.addActionListener(e -> {
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) projectTree.getModel().getRoot();
            rootNode.removeAllChildren();
            createNodes(rootNode, ((FileNode) rootNode.getUserObject()).getFile());
            ((DefaultTreeModel) projectTree.getModel()).reload();
        });

        projectPopup.add(compileSelected);
        projectPopup.add(linkSelected);
        projectPopup.add(new JSeparator());
        projectPopup.add(refreshTree);

        projectTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // Sağ tık menüsünü göstermeden önce, seçili dosya türlerine göre
                    // "Derle" ve "Linkle" seçeneklerini etkinleştir veya devre dışı bırak.
                    // Bu, kullanıcının geçersiz bir işlem yapmasını engeller.
                    TreePath[] paths = projectTree.getSelectionPaths();
                    if (paths == null) {
                        compileSelected.setEnabled(false);
                        linkSelected.setEnabled(false);
                        projectPopup.show(e.getComponent(), e.getX(), e.getY());
                        return;
                    }

                    boolean hasAsm = false;
                    boolean hasObj = false;
                    for (TreePath path : paths) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        FileNode fileNode = (FileNode) node.getUserObject();
                        if (fileNode.getFile().getName().endsWith(".asm")) hasAsm = true;
                        if (fileNode.getFile().getName().endsWith(".obj")) hasObj = true;
                    }

                    compileSelected.setEnabled(hasAsm);
                    linkSelected.setEnabled(hasObj);

                    projectPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        projectTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) projectTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.isLeaf()) {
                FileNode fileNode = (FileNode) selectedNode.getUserObject();
                File file = fileNode.getFile();
                if (file.isFile() && (file.getName().endsWith(".asm") || file.getName().endsWith(".txt") || file.getName().endsWith(".map") || file.getName().endsWith(".lib"))) {
                    openFileInEditor(file);
                }
            }
        });
    }

    private static void createNodes(DefaultMutableTreeNode node, File file) {
        // .git gibi istenmeyen klasörleri atla
        if (file.getName().equals(".git") || file.getName().equals(".idea") || file.getName().equals("lib")) {
            return;
        }

        if (file.isDirectory()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
            node.add(childNode);
            File[] files = file.listFiles();
            if (files != null) {
                // Önce klasörleri, sonra dosyaları ekle (alfabetik)
                java.util.Arrays.sort(files, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                for (File f : files) {
                    createNodes(childNode, f);
                }
            }
        } else {
            // Sadece belirli dosya türlerini ağaca ekle
            if (file.getName().endsWith(".asm") || file.getName().endsWith(".obj") || file.getName().endsWith(".txt") || file.getName().endsWith(".map") || file.getName().endsWith(".java") || file.getName().endsWith(".iml") || file.getName().endsWith(".lib")) {
                node.add(new DefaultMutableTreeNode(new FileNode(file)));
            }
        }
    }

    private static void openFileInEditor(File fileToOpen) {
        currentFile = fileToOpen;
        try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
            codeEditor.read(reader, null);
            codeEditor.setCaretPosition(0); // Metni en başa kaydır
            frame.setTitle("MSP430 Assembler - " + fileToOpen.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Dosya okunamadı!", "Hata", JOptionPane.ERROR_MESSAGE);
            currentFile = null;
        }
    }

    private static void showUserGuide() {
        // Kılavuz içeriğini mevcut dile göre al
        String helpContent = getHelpContentHtml(currentLanguage);

        // HTML içeriğini gösterebilen bir JEditorPane oluştur
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(helpContent);
        editorPane.setEditable(false);

        // Uzun içerikler için kaydırma çubuğu ekle
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(600, 500));

        // Kılavuzu bir dialog penceresinde göster
        JOptionPane.showMessageDialog(
                frame,
                scrollPane,
                currentLanguage.equals("tr") ? "Kullanıcı Kılavuzu" : "User Guide",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private static String getHelpContentHtml(String lang) {
        if (lang.equals("tr")) {
            return """
                    <html><body style='font-family: "Courier New", monospace; padding: 10px;'>
                        <h1>MSP430 Assembler Kullanıcı Kılavuzu</h1>
                        <h2>Giriş</h2>
                        <p>Bu program, MSP430 mikrokontrolcüleri için yazılmış assembly kodlarını derlemek, linklemek ve analiz etmek için geliştirilmiş bir IDE'dir. Program, linklenmiş kodun belleğe nasıl yerleştiğini gösteren basitleştirilmiş bir loader simülasyonu da içerir.</p>
                        
                        <h2>Temel Kullanım</h2>
                        <ol>
                            <li><b>Proje Gezgini:</b> Sol taraftaki gezgin aracılığıyla proje dosyalarınıza göz atabilirsiniz. Bir <code>.asm</code> dosyasına çift tıklayarak editörde açabilirsiniz.</li>
                            <li><b>Derleme:</b> Bir veya daha fazla <code>.asm</code> dosyasını seçip sağ tıklayarak veya üstteki <b>Derle</b> butonuna basarak seçili dosyayı derleyebilirsiniz. Bu işlem, <code>obj</code> klasöründe her dosya için bir <code>.obj</code> nesne dosyası oluşturur.</li>
                            <li><b>Linkleme:</b> Bir veya daha fazla <code>.obj</code> dosyasını seçip sağ tıklayarak veya üstteki <b>Linker</b> butonuna basarak tüm <code>obj</code> dosyalarını linkleyebilirsiniz. Bu işlem, belleğe yüklenebilecek nihai makine kodunu içeren bir <code>.txt</code> dosyasını <code>TI-TXT</code> klasöründe oluşturur.</li>
                            <li><b>Harita Dosyası:</b> Başarılı bir linkleme işleminden sonra <b>Harita Oluştur</b> butonu aktif olur. Bu buton, sembollerin ve segmentlerin bellek adreslerini gösteren bir <code>.map</code> dosyası üretir.</li>
                            <li><b>Belleğe Yükleme:</b> <b>Yükle</b> butonu, linklenmiş programı sağdaki bellek görünümüne yükler.</li>
                        </ol>
                        
                        <h2>Arayüz Bileşenleri</h2>
                        <ul>
                            <li><b>Kod Editörü:</b> Assembly kodlarınızı yazdığınız ana alandır.</li>
                            <li><b>Bellek Görünümü:</b> Program belleğe yüklendiğinde içeriğini hex formatında gösterir. Üstteki arama kutusuna hex adres yazıp Enter'a basarak istediğiniz adrese gidebilirsiniz.</li>
                            <li><b>Çıktı Panelleri:</b>
                                <ul>
                                    <li><b>Assembler:</b> Derleme sonrası üretilen, adres ve makine kodlarını içeren listeleme çıktısını gösterir.</li>
                                    <li><b>TI-TXT:</b> Linker tarafından üretilen ve MSP430'a yüklenebilen son makine kodunu içerir.</li>
                                    <li><b>Bellek Haritası:</b> Segmentlerin ve sembollerin bellek yerleşimini gösterir.</li>
                                    <li><b>Günlük:</b> Yapılan işlemler, hatalar ve uyarılar hakkında bilgi verir.</li>
                                </ul>
                            </li>
                        </ul>
                        
                        <h2>Desteklenen Direktifler</h2>
                        <p>Aşağıdaki direktifler desteklenmektedir:</p>
                        <ul>
                            <li><code>.text, .data, .bss</code>: Bellek segmentlerini belirtir.</li>
                            <li><code>.org</code>: Bir sonraki komutun başlayacağı mutlak adresi belirler.</li>
                            <li><code>.word, .byte, .float, .string</code>: Belleğe veri yazar.</li>
                            <li><code>.ref, .def</code>: Modüller arası sembol paylaşımı için kullanılır.</li>
                            <code>.mlib "dosya_adi.lib"</code>: Harici macro kütüphanesini dahil eder.</li>
                            <li><code>.macro / .endm</code>: Macro tanımlamak için kullanılır.</li>
                            <li><code>etiket?</code>: Macro içinde benzersiz yerel etiketler oluşturur.</li>
                        </ul>
                        <hr>
                        <h2>İletişim</h2>
                        <h3>Geliştiriciler</h3>
                        <ul>
                            <li>Furkan Duran</li>
                            <li>Ahmet Vatansever</li>
                            <li>Mehmet Ekici</li>
                            <li>Ahmet Çalışır</li>
                            <li>Ahmet Çoban</li>
                        </ul>
                        <p>
                            <b>Kurum:</b> Sakarya Uygulamalı Bilimler Üniversitesi, Teknoloji Fakültesi, Bilgisayar Mühendisliği<br>
                            <b>İletişim E-posta:</b> b210109048@subu.edu.tr
                        </p>
                    </body></html>
                    """;
        } else {
            return """
                    <html><body style='font-family: "Courier New", monospace; padding: 10px;'>
                        <h1>MSP430 Assembler User Guide</h1>
                        <h2>Introduction</h2>
                        <p>This program is an IDE developed to assemble, link, and analyze assembly code written for MSP430 microcontrollers.</p>
                        
                        <h2>Basic Usage</h2>
                        <ol>
                            <li><b>Project Explorer:</b> You can browse your project files using the explorer on the left. Double-click an <code>.asm</code> file to open it in the editor.</li>
                            <li><b>Compile:</b> You can compile one or more selected <code>.asm</code> files by right-clicking them or compile the current file by pressing the <b>Compile</b> button. This process creates an <code>.obj</code> object file for each source file in the <code>obj</code> folder.</li>
                            <li><b>Link:</b> You can link one or more <code>.obj</code> files by right-clicking them or link all <code>.obj</code> files by pressing the <b>Link</b> button. This process creates a <code>.txt</code> file in the <code>TI-TXT</code> folder, which contains the final machine code that can be loaded into memory.</li>
                            <li><b>Map File:</b> After a successful link operation, the <b>Generate Map</b> button becomes active. This button produces a <code>.map</code> file showing the memory addresses of symbols and segments.</li>
                            <li><b>Load to Memory:</b> The <b>Load</b> button loads the linked program into the memory view on the right.</li>
                        </ol>
                        
                        <h2>Interface Components</h2>
                        <ul>
                            <li><b>Code Editor:</b> The main area where you write your assembly code.</li>
                            <li><b>Memory View:</b> Displays the content of the program memory in hex format after loading. You can navigate to a specific address by entering a hex address in the search box above and pressing Enter.</li>
                            <li><b>Output Panes:</b>
                                <ul>
                                    <li><b>Assembler:</b> Shows the listing output generated after compilation, including addresses and machine codes.</li>
                                    <li><b>TI-TXT:</b> Contains the final machine code produced by the linker, ready to be loaded onto an MSP430.</li>
                                    <li><b>Map:</b> Displays the memory layout of segments and symbols.</li>
                                    <li><b>Log:</b> Provides information about operations, errors, and warnings.</li>
                                </ul>
                            </li>
                        </ul>
                        
                        <h2>Supported Directives</h2>
                        <p>The following directives are supported:</p>
                        <ul>
                            <li><code>.text, .data, .bss</code>: Specify memory segments.</li>
                            <li><code>.org</code>: Sets the absolute address for the next instruction.</li>
                            <li><code>.word, .byte, .float, .string</code>: Writes data into memory.</li>
                            <li><code>.ref, .def</code>: Used for sharing symbols between modules.</li>
                            <li><code>.mlib "filename.lib"</code>: Includes an external macro library.</li>
                            <li><code>.macro / .endm</code>: Used to define macros.</li>
                            <li><code>label?</code>: Creates unique local labels within a macro.</li>
                        </ul>
                        <hr>
                        <h2>Contact</h2>
                        <h3>Developers</h3>
                        <ul>
                            <li>Furkan Duran</li>
                            <li>Ahmet Vatansever</li>
                            <li>Mehmet Ekici</li>
                            <li>Ahmet Çalışır</li>
                            <li>Ahmet Çoban</li>
                        </ul>
                        <p>
                            <b>Institution:</b> Sakarya University of Applied Sciences, Faculty of Technology, Computer Engineering<br>
                            <b>Contact Email:</b> b210109048@subu.edu.tr
                        </p>
                    </body></html>
                    """;
        }
    }
}

// JTree için dosya adını göstermek ve File nesnesini saklamak için yardımcı sınıf
class FileNode {
    private final File file;
    public FileNode(File file) { this.file = file; }
    public File getFile() { return file; }
    @Override
    public String toString() { return file.getName(); }
}

// JTree'de klasör ve dosya ikonlarını göstermek için renderer
class FileTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
        if (userObject instanceof FileNode) {
            File file = ((FileNode) userObject).getFile();
            if (file.isDirectory()) {
                setIcon(UIManager.getIcon("FileView.directoryIcon"));
            } else {
                setIcon(UIManager.getIcon("FileView.fileIcon"));
            }
        }
        return this;
    }
}

// Helper class to redirect PrintStream to a JTextArea
class JTextAreaOutputStream extends OutputStream {
    private final JTextArea textArea;
    public JTextAreaOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }
    @Override
    public void write(int b) throws IOException {
        textArea.append(String.valueOf((char)b));
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}