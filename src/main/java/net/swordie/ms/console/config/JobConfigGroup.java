package net.swordie.ms.console.config;

import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.SkillOverrideCache;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.SkillStringInfo;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class JobConfigGroup {
    private static final int INITIAL_JOB_WIDTH = 260;
    private static final int INITIAL_SKILL_WIDTH = 360;

    private JobSkillCatalog catalog = new JobSkillCatalog();
    private JTree jobTree;
    private DefaultListModel<SkillCheckItem> skillListModel;
    private JList<SkillCheckItem> skillList;
    private JTextField searchField;
    private JTextArea header;
    private JPanel form;
    private final Map<SkillStat, JTextField> statFields = new LinkedHashMap<>();
    private final Map<String, JTextField> baseFields = new LinkedHashMap<>();
    private List<SkillCheckItem> currentJobSkills = List.of();
    private String selectedJob;
    private int selectedSkillId;
    private boolean suppressSelection;

    JComponent buildPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        jobTree = buildJobTree();
        JScrollPane jobScroll = ConfigUi.verticalScroll(jobTree);
        jobScroll.setMinimumSize(new Dimension(180, 120));

        skillListModel = new DefaultListModel<>();
        skillList = new JList<>(skillListModel);
        skillList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        skillList.addListSelectionListener(this::onSkillSelected);
        JScrollPane skillScroll = ConfigUi.verticalScroll(skillList);

        JPanel skillColumn = new JPanel(new BorderLayout(0, 6));
        skillColumn.add(buildSearchRow(), BorderLayout.NORTH);
        skillColumn.add(skillScroll, BorderLayout.CENTER);
        skillColumn.setMinimumSize(new Dimension(220, 120));

        header = new JTextArea(5, 20);
        header.setEditable(false);
        header.setLineWrap(true);
        header.setWrapStyleWord(true);
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        form = new JPanel(new GridBagLayout());
        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.add(form, BorderLayout.NORTH);
        JScrollPane formScroll = ConfigUi.verticalScroll(formWrap);

        JPanel detail = new JPanel(new BorderLayout(0, 6));
        detail.setBorder(BorderFactory.createTitledBorder("技能參數"));
        detail.add(header, BorderLayout.NORTH);
        detail.add(formScroll, BorderLayout.CENTER);
        detail.add(buildButtonRow(), BorderLayout.SOUTH);

        JSplitPane directorySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jobScroll, skillColumn);
        directorySplit.setContinuousLayout(true);
        directorySplit.setOneTouchExpandable(true);
        directorySplit.setResizeWeight(0.42);
        directorySplit.setDividerLocation(INITIAL_JOB_WIDTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, directorySplit, detail);
        mainSplit.setContinuousLayout(true);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setResizeWeight(0.48);
        mainSplit.setDividerLocation(INITIAL_JOB_WIDTH + INITIAL_SKILL_WIDTH);

        root.add(mainSplit, BorderLayout.CENTER);
        selectFirstJobLeaf();
        return root;
    }

    private JPanel buildSearchRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel("搜索"), BorderLayout.WEST);
        searchField = new JTextField();
        searchField.setToolTipText("搜索當前職業；無結果時自動搜索全職業");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        row.add(searchField, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reload = new JButton("重載目錄");
        reload.addActionListener(e -> reloadCatalog());
        JButton save = new JButton("保存並實時生效");
        save.addActionListener(e -> saveSelectedSkill());
        row.add(reload);
        row.add(save);
        return row;
    }

    private JTree buildJobTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("職業");
        for (JobSkillCatalog.JobCategory category : catalog.categories()) {
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(category.name());
            for (String job : category.jobs()) {
                categoryNode.add(new DefaultMutableTreeNode(job));
            }
            root.add(categoryNode);
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(e -> onJobSelected());
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        return tree;
    }

    private void onJobSelected() {
        if (suppressSelection) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jobTree.getLastSelectedPathComponent();
        if (node == null || !node.isLeaf()) {
            return;
        }
        String job = String.valueOf(node.getUserObject());
        if (!catalog.isKnownJob(job)) {
            return;
        }
        selectedJob = job;
        fillSkillList(job);
    }

    private void fillSkillList(String job) {
        List<SkillCheckItem> items = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : catalog.skillsFor(job).entrySet()) {
            items.add(new SkillCheckItem(entry.getKey(), entry.getValue()));
        }
        if (items.isEmpty()) {
            items.add(new SkillCheckItem(0, "（該職業暫無已加載技能，啓動服務端完成數據加載後點重載目錄）"));
        }
        currentJobSkills = items;
        clearDetail();
        applyFilter();
    }

    private void applyFilter() {
        if (skillListModel == null) {
            return;
        }
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        skillListModel.clear();
        if (q.isEmpty()) {
            for (SkillCheckItem item : currentJobSkills) {
                skillListModel.addElement(item);
            }
            return;
        }
        List<SkillCheckItem> local = currentJobSkills.stream().filter(item -> matches(item, q)).toList();
        if (!local.isEmpty()) {
            local.forEach(skillListModel::addElement);
            return;
        }
        for (String job : catalog.jobNames()) {
            for (Map.Entry<Integer, String> entry : catalog.skillsFor(job).entrySet()) {
                SkillCheckItem item = new SkillCheckItem(entry.getKey(), entry.getValue(), job);
                if (matches(item, q)) {
                    skillListModel.addElement(item);
                }
            }
        }
        if (skillListModel.isEmpty()) {
            skillListModel.addElement(new SkillCheckItem(0, "（無匹配技能）"));
        }
    }

    private static boolean matches(SkillCheckItem item, String q) {
        return String.valueOf(item.skillId).contains(q)
                || item.name.toLowerCase(Locale.ROOT).contains(q)
                || (item.ownerJob != null && item.ownerJob.toLowerCase(Locale.ROOT).contains(q));
    }

    private void onSkillSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        SkillCheckItem item = skillList.getSelectedValue();
        if (item == null || item.skillId <= 0) {
            return;
        }
        if (item.ownerJob != null && !item.ownerJob.equals(selectedJob)) {
            selectJobInTree(item.ownerJob);
        }
        showSkill(item.skillId);
    }

    private void showSkill(int skillId) {
        SkillInfo info = SkillData.getSkillInfoById(skillId);
        if (info == null) {
            JOptionPane.showMessageDialog(form, "技能數據未加載: " + skillId, "技能管理", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectedSkillId = skillId;
        statFields.clear();
        baseFields.clear();
        form.removeAll();
        GridBagConstraints c = ConfigUi.gbc();
        addBaseField(c, "maxLevel", "最大等級", String.valueOf(info.getMaxLevel()));
        addBaseField(c, "masterLevel", "精通等級", String.valueOf(info.getMasterLevel()));
        addBaseField(c, "fixLevel", "固定等級", String.valueOf(info.getFixLevel()));
        addBaseField(c, "vSkill", "五轉標記", String.valueOf(info.getVSkill()));
        addBaseField(c, "hyper", "Hyper標記", String.valueOf(info.getHyper()));
        addBaseField(c, "boostDamR", "強化傷害%", String.valueOf(info.getBoostDamR()));

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 1;
        JLabel split = new JLabel("SkillStat 公式參數（支持數字或含 x 的等級公式）");
        split.setFont(split.getFont().deriveFont(Font.BOLD));
        form.add(split, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;

        EnumMap<SkillStat, String> stats = info.getSkillStatInfo();
        for (Map.Entry<SkillStat, String> entry : stats.entrySet()) {
            addStatField(c, entry.getKey(), entry.getValue());
        }
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weighty = 1;
        form.add(Box.createVerticalGlue(), c);

        SkillStringInfo stringInfo = StringData.getSkillString().get(skillId);
        String name = stringInfo == null ? "" : stringInfo.getName();
        String desc = stringInfo == null ? "" : stringInfo.getDesc();
        header.setText("技能ID: " + skillId
                + "\n職業節點: " + (selectedJob == null ? "-" : selectedJob)
                + "\n名稱: " + (name == null || name.isBlank() ? "技能" + skillId : name)
                + "\n說明: " + (desc == null ? "" : desc));
        form.revalidate();
        form.repaint();
    }

    private void addBaseField(GridBagConstraints c, String key, String label, String value) {
        JTextField field = ConfigUi.addText(form, c, label, value, () -> { });
        baseFields.put(key, field);
    }

    private void addStatField(GridBagConstraints c, SkillStat stat, String value) {
        JTextField field = ConfigUi.addText(form, c, stat.name(), value, () -> { });
        statFields.put(stat, field);
    }

    private void saveSelectedSkill() {
        if (selectedSkillId <= 0) {
            JOptionPane.showMessageDialog(form, "請先選擇技能。", "技能管理", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SkillInfo info = SkillData.getSkillInfoById(selectedSkillId);
        if (info == null) {
            JOptionPane.showMessageDialog(form, "技能數據不存在: " + selectedSkillId, "技能管理", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            info.setMaxLevel(parseInt("最大等級", baseFields.get("maxLevel")));
            info.setMasterLevel(parseInt("精通等級", baseFields.get("masterLevel")));
            info.setFixLevel(parseInt("固定等級", baseFields.get("fixLevel")));
            info.setVSkill(parseInt("五轉標記", baseFields.get("vSkill")));
            info.setHyper(parseInt("Hyper標記", baseFields.get("hyper")));
            info.setBoostDamR(parseInt("強化傷害%", baseFields.get("boostDamR")));
            for (Map.Entry<SkillStat, JTextField> entry : statFields.entrySet()) {
                String value = entry.getValue().getText();
                info.addSkillStatInfo(entry.getKey(), value == null ? "" : value.trim());
            }
            SkillOverrideCache.saveSkill(info);
            SkillData.saveSkillToDat(info);
            info.clearCalculatedCache();
            info.load();
            JOptionPane.showMessageDialog(form,
                    "技能 " + selectedSkillId + " 已寫入內存，並保存到 data/skill.cg。\n下次啓動會從 skill.cg 快速套用修改；data/dat265/skills 僅作爲兼容緩存。",
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(form, ex.getMessage(), "參數錯誤", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(form, "保存失敗: " + ex.getMessage(), "技能管理", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static int parseInt(String label, JTextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ignored) {
            throw new IllegalArgumentException(label + " 必須是整數。");
        }
    }

    private void reloadCatalog() {
        catalog = new JobSkillCatalog();
        DefaultTreeModel model = (DefaultTreeModel) jobTree.getModel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("職業");
        for (JobSkillCatalog.JobCategory category : catalog.categories()) {
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(category.name());
            for (String job : category.jobs()) {
                categoryNode.add(new DefaultMutableTreeNode(job));
            }
            root.add(categoryNode);
        }
        model.setRoot(root);
        for (int i = 0; i < jobTree.getRowCount(); i++) {
            jobTree.expandRow(i);
        }
        selectFirstJobLeaf();
    }

    private void selectFirstJobLeaf() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) jobTree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode category = (DefaultMutableTreeNode) root.getChildAt(i);
            if (category.getChildCount() > 0) {
                DefaultMutableTreeNode first = (DefaultMutableTreeNode) category.getChildAt(0);
                TreePath path = new TreePath(new Object[]{root, category, first});
                jobTree.setSelectionPath(path);
                selectedJob = String.valueOf(first.getUserObject());
                fillSkillList(selectedJob);
                return;
            }
        }
    }

    private void selectJobInTree(String job) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) jobTree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode category = (DefaultMutableTreeNode) root.getChildAt(i);
            for (int j = 0; j < category.getChildCount(); j++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) category.getChildAt(j);
                if (job.equals(String.valueOf(node.getUserObject()))) {
                    suppressSelection = true;
                    jobTree.setSelectionPath(new TreePath(new Object[]{root, category, node}));
                    selectedJob = job;
                    fillSkillList(job);
                    suppressSelection = false;
                    return;
                }
            }
        }
    }

    private void clearDetail() {
        selectedSkillId = 0;
        statFields.clear();
        baseFields.clear();
        if (header != null) {
            header.setText("請選擇技能。五轉/六轉技能已作爲獨立節點聚合。修改後點擊“保存並實時生效”。");
        }
        if (form != null) {
            form.removeAll();
            form.revalidate();
            form.repaint();
        }
    }

}