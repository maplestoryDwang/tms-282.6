package net.swordie.ms.console.datamanage;

import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 發物對話框：物品搜索、類型篩選、裝備屬性編輯。 */
public final class GiveItemDialog extends JDialog {
    private static final int SEARCH_LIMIT = 300;

    private final ConsoleCharacterService.PlayerTarget target;
    private final GiveItemSpec spec = new GiveItemSpec();

    private final DefaultTableModel resultModel = new DefaultTableModel(
            new String[]{"ID", "名稱", "類型"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(resultModel);
    private final JTextField searchField = new JTextField();
    private final JComboBox<TypeFilter> typeFilterBox = new JComboBox<>(TypeFilter.values());
    private final JLabel selectedLabel = new JLabel("未選擇物品");
    private final JLabel typeHintLabel = new JLabel("—");
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JSpinner periodSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3650, 1));
    private final JPanel equipOptionsHost = new JPanel(new BorderLayout());
    private final GiveItemEquipPanel equipPanel;
    private final JLabel equipHintLabel = new JLabel(
            "<html><font color=#555>選擇裝備後可在下方配置星力、潛能等；數量與有效期見上方。</font></html>");
    private SwingWorker<List<ItemRow>, Void> searchWorker;
    private int selectedItemId = -1;
    private String selectedItemName = "";

    private enum TypeFilter {
        ALL("全部"), EQUIP("裝備"), USE("消耗"), SETUP("設置"), ETC("其他"), CASH("現金"), DECORATION("時裝");

        final String label;

        TypeFilter(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        boolean matches(int itemId) {
            if (this == ALL) {
                return true;
            }
            InvType t = ItemConstants.getInvTypeByItemID(itemId);
            return switch (this) {
                case EQUIP -> t == InvType.EQUIP;
                case USE -> t == InvType.CONSUME;
                case SETUP -> t == InvType.INSTALL;
                case ETC -> t == InvType.ETC;
                case CASH -> t == InvType.CASH;
                case DECORATION -> t == InvType.DECORATION;
                default -> true;
            };
        }
    }

    public GiveItemDialog(Window owner, ConsoleCharacterService.PlayerTarget target) {
        super(owner, "發送物品 — " + target.name, ModalityType.APPLICATION_MODAL);
        this.target = target;
        this.equipPanel = new GiveItemEquipPanel(spec);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        setMinimumSize(new Dimension(1000, 640));
        setSize(1040, 700);
        setLocationRelativeTo(owner);
        searchField.requestFocusInWindow();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String status = target.isOnline() ? "在線，發送後立即到賬" : "離線，下次登錄生效";
        root.add(new JLabel("<html>目標玩家: <b>" + escapeHtml(target.name) + "</b> &nbsp;|&nbsp; " + status
                + "</html>"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.4);
        split.setLeftComponent(buildSearchPanel());
        split.setRightComponent(buildOptionsPanel());
        root.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton sendBtn = new JButton("發送");
        sendBtn.setPreferredSize(new Dimension(90, 30));
        sendBtn.addActionListener(e -> sendSelected());
        bottom.add(sendBtn);
        JButton cancelBtn = new JButton("關閉");
        cancelBtn.addActionListener(e -> dispose());
        bottom.add(cancelBtn);
        root.add(bottom, BorderLayout.SOUTH);

        getContentPane().add(root);
    }

    private JPanel buildSearchPanel() {
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(BorderFactory.createTitledBorder("物品搜索"));

        JPanel searchBar = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        searchBar.add(new JLabel("關鍵詞:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        searchField.setPreferredSize(new Dimension(200, 28));
        searchBar.add(searchField, c);
        c.gridx = 2;
        c.weightx = 0;
        JButton searchBtn = new JButton("搜索");
        searchBtn.setPreferredSize(new Dimension(72, 28));
        searchBtn.addActionListener(e -> runSearch());
        searchBar.add(searchBtn, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        searchBar.add(new JLabel("類型:"), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        typeFilterBox.setPreferredSize(new Dimension(160, 28));
        searchBar.add(typeFilterBox, c);

        searchField.addActionListener(e -> runSearch());
        left.add(searchBar, BorderLayout.NORTH);

        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(72);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(52);
        resultTable.setRowHeight(24);
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onItemSelectionChanged();
            }
        });
        left.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        selectedLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        left.add(selectedLabel, BorderLayout.SOUTH);
        return left;
    }

    private JPanel buildOptionsPanel() {
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("發物選項"));

        JPanel common = new JPanel(new GridBagLayout());
        common.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 8, 4, 8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        common.add(new JLabel("物品類型:"), gc);
        gc.gridx = 1;
        typeHintLabel.setFont(typeHintLabel.getFont().deriveFont(Font.BOLD, 13f));
        common.add(typeHintLabel, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        common.add(new JLabel("數量:"), gc);
        gc.gridx = 1;
        quantitySpinner.setPreferredSize(new Dimension(100, 28));
        common.add(quantitySpinner, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        common.add(new JLabel("有效期(天):"), gc);
        gc.gridx = 1;
        periodSpinner.setPreferredSize(new Dimension(100, 28));
        JPanel periodRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        periodRow.add(periodSpinner);
        periodRow.add(new JLabel("（0 = 永久）"));
        common.add(periodRow, gc);

        right.add(common, BorderLayout.NORTH);

        equipOptionsHost.add(equipHintLabel, BorderLayout.NORTH);
        equipOptionsHost.add(equipPanel, BorderLayout.CENTER);
        equipOptionsHost.setVisible(false);
        right.add(equipOptionsHost, BorderLayout.CENTER);

        return right;
    }

    private void onItemSelectionChanged() {
        int row = resultTable.getSelectedRow();
        if (row < 0) {
            selectedItemId = -1;
            selectedItemName = "";
            selectedLabel.setText("未選擇物品");
            typeHintLabel.setText("—");
            equipOptionsHost.setVisible(false);
            return;
        }
        selectedItemId = (Integer) resultModel.getValueAt(row, 0);
        selectedItemName = String.valueOf(resultModel.getValueAt(row, 1));
        String typeName = String.valueOf(resultModel.getValueAt(row, 2));
        selectedLabel.setText("已選: " + selectedItemId + " — " + selectedItemName);
        typeHintLabel.setText(typeName);
        boolean equip = EquipGrantHelper.isGrantableEquip(selectedItemId);
        equipOptionsHost.setVisible(equip);
        if (equip) {
            equipPanel.onItemSelected(selectedItemId);
            quantitySpinner.setValue(1);
        }
        revalidate();
        repaint();
    }

    private void runSearch() {
        final String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入搜索關鍵詞或物品ID。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
        }
        final TypeFilter filter = (TypeFilter) typeFilterBox.getSelectedItem();
        resultModel.setRowCount(0);
        selectedLabel.setText("搜索中…");
        searchWorker = new SwingWorker<>() {
            @Override
            protected List<ItemRow> doInBackground() {
                List<ItemRow> rows = ConsoleCharacterService.searchItems(keyword, SEARCH_LIMIT);
                List<ItemRow> filtered = new ArrayList<>();
                for (ItemRow row : rows) {
                    if (filter.matches(row.itemId)) {
                        filtered.add(row);
                    }
                }
                return filtered;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    List<ItemRow> rows = get();
                    resultModel.setRowCount(0);
                    for (ItemRow row : rows) {
                        resultModel.addRow(new Object[]{row.itemId, row.name, inventoryTypeName(row.itemId)});
                    }
                    if (rows.isEmpty()) {
                        selectedLabel.setText("未找到匹配物品");
                    } else {
                        selectedLabel.setText("共 " + rows.size() + " 條，類型篩選: " + filter.label);
                        resultTable.setRowSelectionInterval(0, 0);
                    }
                } catch (Exception ex) {
                    selectedLabel.setText("搜索失敗");
                    JOptionPane.showMessageDialog(GiveItemDialog.this, "搜索失敗: " + ex.getMessage(), "錯誤",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        searchWorker.execute();
    }

    private static String inventoryTypeName(int itemId) {
        InvType t = ItemConstants.getInvTypeByItemID(itemId);
        if (t == InvType.EQUIP) {
            return "裝備";
        }
        if (t == InvType.CONSUME) {
            return "消耗";
        }
        if (t == InvType.INSTALL) {
            return "設置";
        }
        if (t == InvType.ETC) {
            return "其他";
        }
        if (t == InvType.CASH) {
            return "現金";
        }
        if (t == InvType.DECORATION) {
            return "時裝";
        }
        return "未知";
    }

    private void sendSelected() {
        if (selectedItemId <= 0) {
            JOptionPane.showMessageDialog(this, "請先搜索並選擇物品。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        spec.quantity = ((Number) quantitySpinner.getValue()).intValue();
        spec.periodDays = ((Number) periodSpinner.getValue()).intValue();
        if (spec.quantity <= 0) {
            JOptionPane.showMessageDialog(this, "數量必須大於 0。", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (EquipGrantHelper.isGrantableEquip(selectedItemId)) {
            equipPanel.syncFromUi();
        }
        if (!confirmSend()) {
            return;
        }
        ConsoleCharacterService.OpResult result = ConsoleCharacterService.giveItem(target, selectedItemId, spec);
        JOptionPane.showMessageDialog(this, result.message, result.success ? "發送成功" : "發送失敗",
                result.success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (result.success) {
            dispose();
        }
    }

    private boolean confirmSend() {
        String itemLine = selectedItemId + " — " + selectedItemName;
        String typeLine = typeHintLabel.getText();
        String qtyLine = String.valueOf(spec.quantity);
        String periodLine = spec.periodDays <= 0 ? "永久" : spec.periodDays + " 天";
        String playerLine = target.name + (target.isOnline() ? "（在線）" : "（離線）");
        String equipExtra = "";
        if (EquipGrantHelper.isGrantableEquip(selectedItemId)) {
            equipExtra = "\n裝備模板: " + spec.equipPreset.label;
            if (spec.equipPreset == GiveItemSpec.EquipPreset.CUSTOM) {
                equipExtra += "\n（已啓用自定義屬性，按勾選項寫入）";
            }
            if (spec.quantity > 1) {
                equipExtra += "\n將逐件發放 " + spec.quantity + " 件裝備。";
            }
        }
        String message = "請確認以下發放信息：\n\n" + "玩家: " + playerLine + "\n" + "物品: " + itemLine + "\n"
                + "類型: " + typeLine + "\n" + "數量: " + qtyLine + "\n" + "有效期: " + periodLine + equipExtra
                + "\n\n確認發送？";
        int choice = JOptionPane.showConfirmDialog(this, message, "確認發送物品", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static final class ItemRow {
        public final int itemId;
        public final String name;

        public ItemRow(int itemId, String name) {
            this.itemId = itemId;
            this.name = name;
        }
    }
}
