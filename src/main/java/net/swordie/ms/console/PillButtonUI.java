package net.swordie.ms.console;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

final class PillButtonUI extends BasicButtonUI {
    private static final int PAD_X = 14;

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

        int w = c.getWidth();
        int h = c.getHeight();
        RoundRectangle2D shape = new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, h - 2f, h - 2f);
        g2.setColor(resolveFill(button));
        g2.fill(shape);
        if (button.isEnabled()) {
            g2.setColor(new Color(0, 0, 0, button.getModel().isPressed() ? 40 : 18));
            g2.draw(shape);
        }

        FontMetrics fm = g2.getFontMetrics(button.getFont());
        String text = button.getText();
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        Color textColor = button.isEnabled() ? button.getForeground() : new Color(0x999999);
        if (button instanceof JToggleButton && button.getModel().isSelected()) {
            textColor = Color.WHITE;
        }
        g2.setColor(textColor);
        g2.setFont(button.getFont());
        g2.drawString(text, tx, ty);
        g2.dispose();
    }

    private static Color resolveFill(AbstractButton button) {
        if (!button.isEnabled()) {
            return new Color(0xD8D8D8);
        }
        Color base = button.getBackground() == null ? new Color(0xF2F2F2) : button.getBackground();
        if (button.getModel().isPressed()) {
            return darken(base, 0.88f);
        }
        if (button instanceof JToggleButton && button.getModel().isSelected()) {
            return ConsoleUiStyle.ACCENT;
        }
        return base;
    }

    private static Color darken(Color c, float factor) {
        return new Color(
                Math.max(0, (int) (c.getRed() * factor)),
                Math.max(0, (int) (c.getGreen() * factor)),
                Math.max(0, (int) (c.getBlue() * factor)),
                c.getAlpha());
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        AbstractButton button = (AbstractButton) c;
        FontMetrics fm = button.getFontMetrics(button.getFont());
        return new Dimension(fm.stringWidth(button.getText()) + PAD_X * 2, 34);
    }
}