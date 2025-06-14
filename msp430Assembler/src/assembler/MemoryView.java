package assembler;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;

public class MemoryView extends JPanel {
    private final JTable memoryTable;
    private final MemoryTableModel tableModel;
    private final JTextField searchField;
    private final JLabel searchLabel;

    public MemoryView() {
        super(new BorderLayout());

        // --- Arama Paneli (Üst) ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        searchField = new JTextField(8);
        searchLabel = new JLabel();
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        add(searchPanel, BorderLayout.NORTH);

        setLanguage("tr");

        tableModel = new MemoryTableModel();
        memoryTable = new JTable(tableModel);

        // --- Table Styling ---
        memoryTable.setFont(new Font("Courier New", Font.PLAIN, 12));
        memoryTable.setFillsViewportHeight(true);
        memoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Center align hex values
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Address column
        memoryTable.getColumnModel().getColumn(0).setPreferredWidth(70);

        // Hex value columns
        for (int i = 1; i <= 16; i++) {
            memoryTable.getColumnModel().getColumn(i).setPreferredWidth(30);
            memoryTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(memoryTable);
        add(scrollPane, BorderLayout.CENTER);

        // Arama alanında Enter'a basıldığında adresi bul
        searchField.addActionListener(e -> findAddress());
    }

    private void findAddress() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) return;

        try {
            // "0x" önekini temizle
            if (searchText.toLowerCase().startsWith("0x")) {
                searchText = searchText.substring(2);
            }
            int address = Integer.parseInt(searchText, 16);

            // Adresin bellek sınırları içinde olup olmadığını kontrol et
            if (address < 0 || address >= tableModel.getMemorySize()) {
                Toolkit.getDefaultToolkit().beep(); // Hata sesi
                return;
            }

            int rowIndex = address / 16;

            if (rowIndex >= 0 && rowIndex < tableModel.getRowCount()) {
                memoryTable.setRowSelectionInterval(rowIndex, rowIndex);
                // Seçili satırı görünümün ortasına kaydır
                memoryTable.scrollRectToVisible(memoryTable.getCellRect(rowIndex, 0, true));
            }

        } catch (NumberFormatException e) {
            Toolkit.getDefaultToolkit().beep(); // Geçersiz format için hata sesi
        }
    }

    public void setLanguage(String lang) {
        if (lang.equals("tr")) {
            searchLabel.setText("Adres Bul (Hex):");
            searchField.setToolTipText("Hex formatında bir adres girin ve Enter'a basın");
        } else {
            searchLabel.setText("Find Address (Hex):");
            searchField.setToolTipText("Enter a hex address and press Enter");
        }
    }

    public void loadMemory(byte[] memoryImage) {
        tableModel.setMemoryImage(memoryImage);
        // Go to top after loading new data
        SwingUtilities.invokeLater(() -> memoryTable.scrollRectToVisible(memoryTable.getCellRect(0, 0, true)));
    }
}

class MemoryTableModel extends AbstractTableModel {
    private byte[] memoryImage = new byte[0];
    private final String[] columnNames = {
            "Address", "00", "01", "02", "03", "04", "05", "06", "07",
            "08", "09", "0A", "0B", "0C", "0D", "0E", "0F"
    };

    public int getMemorySize() {
        return memoryImage.length;
    }

    public void setMemoryImage(byte[] memoryImage) {
        this.memoryImage = memoryImage;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return (memoryImage.length + 15) / 16; // Round up
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int address = rowIndex * 16;

        if (columnIndex == 0) { // Address column
            return String.format("0x%04X", address);
        }

        if (columnIndex >= 1 && columnIndex <= 16) { // Hex value columns
            int byteIndex = address + (columnIndex - 1);
            if (byteIndex < memoryImage.length) {
                return String.format("%02X", memoryImage[byteIndex]);
            } else {
                return "";
            }
        }

        return "";
    }
} 