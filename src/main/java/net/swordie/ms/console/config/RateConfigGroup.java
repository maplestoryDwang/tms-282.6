package net.swordie.ms.console.config;

import net.swordie.ms.RateSettings;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** 倍率設定 tab: edits the live EXP / meso / drop rates and saves only the rate.* lines of server.properties. */
final class RateConfigGroup {
    private final Map<RateSettings.Rate, JTextField> fields = new EnumMap<>(RateSettings.Rate.class);
    private final JButton applyButton = new JButton("套用倍率");

    JComponent buildPanel() {
        JPanel form = ConfigUi.formPanel();
        GridBagConstraints c = ConfigUi.gbc();
        Runnable dirty = () -> applyButton.setEnabled(true);
        for (RateSettings.Rate rate : RateSettings.Rate.values()) {
            fields.put(rate, ConfigUi.addText(form, c, rate.label() + "（" + rate.key() + "）", String.valueOf(rate.get()), dirty));
        }
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        JLabel tip = new JLabel("說明：數值範圍 " + RateSettings.MIN + "–" + RateSettings.MAX
                + "。按「套用倍率」後立即對本服務端生效，並只改寫 server.properties 的 rate.* 行，其餘內容與註解不變。");
        tip.setForeground(Color.DARK_GRAY);
        form.add(tip, c);
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        JButton reload = new JButton("重新讀取目前值");
        reload.addActionListener(e -> refresh());
        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> apply(form));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(reload);
        buttons.add(applyButton);
        form.add(buttons, c);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(form, BorderLayout.NORTH);
        return ConfigUi.verticalScroll(wrap);
    }

    private void refresh() {
        for (Map.Entry<RateSettings.Rate, JTextField> e : fields.entrySet()) {
            e.getValue().setText(String.valueOf(e.getKey().get()));
        }
        applyButton.setEnabled(false);
    }

    private void apply(Component parent) {
        Map<RateSettings.Rate, Integer> parsed = new EnumMap<>(RateSettings.Rate.class);
        for (Map.Entry<RateSettings.Rate, JTextField> e : fields.entrySet()) {
            String raw = e.getValue().getText().trim();
            int value;
            try {
                value = Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                value = -1;
            }
            if (!RateSettings.valid(value)) {
                JOptionPane.showMessageDialog(parent, e.getKey().label() + " 必須是 " + RateSettings.MIN + "–" + RateSettings.MAX
                        + " 的整數（目前：" + raw + "）。", "倍率設定", JOptionPane.WARNING_MESSAGE);
                e.getValue().requestFocusInWindow();
                return;
            }
            parsed.put(e.getKey(), value);
        }
        String failure = null;
        for (Map.Entry<RateSettings.Rate, Integer> e : parsed.entrySet()) {
            if (e.getKey().get() == e.getValue()) continue;
            try {
                RateSettings.set(e.getKey(), e.getValue(), true);
            } catch (IOException ex) {
                failure = ex.getMessage();
            }
        }
        applyButton.setEnabled(false);
        if (failure == null) {
            JOptionPane.showMessageDialog(parent, "倍率已即時生效並保存。\n\n" + RateSettings.summary(), "倍率設定", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, "倍率已即時生效，但保存到設定檔失敗（重啟後恢復舊值）：\n" + failure,
                    "倍率設定", JOptionPane.ERROR_MESSAGE);
        }
    }
}
