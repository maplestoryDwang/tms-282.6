package net.swordie.ms.console.config;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;

final class ConfigUi {
    private ConfigUi() {
    }

    static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    static JTextField addText(JPanel panel, GridBagConstraints c, String label, String value, Runnable dirty) {
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        JTextField field = new JTextField(value == null ? "" : value, 32);
        field.getDocument().addDocumentListener(onChange(dirty));
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
        c.gridy++;
        return field;
    }

    static JScrollPane verticalScroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    static DocumentListener onChange(Runnable dirty) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                dirty.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                dirty.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                dirty.run();
            }
        };
    }
}