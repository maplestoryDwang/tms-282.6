package net.swordie.ms.console.config;

import net.swordie.ms.constants.JobConstants;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

final class JobCreationConfigGroup {
    private final Map<JobConstants.LoginJob, JCheckBox> switches = new LinkedHashMap<>();

    JComponent buildPanel() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(new JLabel("僅控制新角色創建。保存後立即影響登錄界面職業列表與服務端創建校驗。"), BorderLayout.NORTH);

        JPanel jobs = new JPanel(new GridLayout(0, 3, 12, 8));
        for (JobConstants.LoginJob job : JobConstants.LoginJob.values()) {
            JCheckBox checkBox = new JCheckBox(label(job), ServerRuntimeConfig.isJobCreationEnabled(job));
            checkBox.setToolTipText("職業類型 " + job.getJobType() + "，起始職業ID " + job.getBeginJob().getJobId());
            switches.put(job, checkBox);
            jobs.add(checkBox);
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(jobs, BorderLayout.NORTH);
        content.add(ConfigUi.verticalScroll(wrap), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reload = new JButton("重載配置");
        reload.addActionListener(e -> reload());
        JButton save = new JButton("保存並實時生效");
        save.addActionListener(e -> save(content));
        buttons.add(reload);
        buttons.add(save);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }

    private static String label(JobConstants.LoginJob job) {
        return JobCatalog.loginJobName(job) + "  [類型 " + job.getJobType() + "]";
    }

    private void reload() {
        ServerRuntimeConfig.reload();
        switches.forEach((job, checkBox) -> checkBox.setSelected(ServerRuntimeConfig.isJobCreationEnabled(job)));
    }

    private void save(Component parent) {
        Map<String, String> values = new LinkedHashMap<>();
        switches.forEach((job, checkBox) -> values.put(ServerRuntimeConfig.jobCreationKey(job), String.valueOf(checkBox.isSelected())));
        try {
            ServerRuntimeConfig.save(values);
            ServerRuntimeConfig.reload();
            JOptionPane.showMessageDialog(parent,
                    "職業創建開關已保存並實時生效。\n玩家重新進入角色選擇頁後會看到最新職業列表。",
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "保存失敗: " + ex.getMessage(), "職業管理", JOptionPane.ERROR_MESSAGE);
        }
    }
}