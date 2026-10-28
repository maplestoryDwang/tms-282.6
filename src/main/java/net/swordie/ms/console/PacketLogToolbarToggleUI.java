package net.swordie.ms.console;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

final class PacketLogToolbarToggleUI extends BasicButtonUI {
    private static final Color FILL_NORMAL = new Color(0xF2F2F2);
    private static final Color FILL_SELECTED = new Color(0xB8B8B8);

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        if (c instanceof AbstractButton button) {
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setOpaque(false);
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton button = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(1f, 1f, c.getWidth() - 2f, c.getHeight() - 2f, c.getHeight() - 2f, c.getHeight() - 2f);
        g2.setColor(button.isSelected() ? FILL_SELECTED : FILL_NORMAL);
        if (button.getModel().isPressed()) {
            g2.setColor(FILL_SELECTED.darker());
        }
        g2.fill(shape);
        g2.setColor(new Color(0, 0, 0, 22));
        g2.draw(shape);

        FontMetrics fm = g2.getFontMetrics(button.getFont());
        String text = button.getText();
        int tx = (c.getWidth() - fm.stringWidth(text)) / 2;
        int ty = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(button.isEnabled() ? new Color(0x333333) : new Color(0x999999));
        g2.setFont(button.getFont());
        g2.drawString(text, tx, ty);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        AbstractButton button = (AbstractButton) c;
        FontMetrics fm = button.getFontMetrics(button.getFont());
        return new Dimension(fm.stringWidth(button.getText()) + 20, 26);
    }
}