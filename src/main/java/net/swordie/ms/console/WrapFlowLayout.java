package net.swordie.ms.console;

import java.awt.*;

/**
 * 自動換行的 FlowLayout：寬度不足時折到下一行。
 */
public class WrapFlowLayout extends FlowLayout {

    public WrapFlowLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getWidth();
            if (targetWidth == 0) {
                Container parent = target.getParent();
                while (parent != null && targetWidth == 0) {
                    targetWidth = parent.getWidth();
                    parent = parent.getParent();
                }
            }
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);
            int x = 0;
            int y = insets.top + vgap;
            int rowHeight = 0;

            for (Component component : target.getComponents()) {
                if (!component.isVisible()) {
                    continue;
                }
                Dimension size = preferred ? component.getPreferredSize() : component.getMinimumSize();
                if (x > 0 && x + size.width > maxWidth) {
                    y += rowHeight + vgap;
                    x = 0;
                    rowHeight = 0;
                }
                if (size.height > rowHeight) {
                    rowHeight = size.height;
                }
                x += size.width + hgap;
            }
            if (rowHeight > 0) {
                y += rowHeight;
            }
            y += insets.bottom;
            return new Dimension(targetWidth, y);
        }
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxWidth = target.getWidth() - (insets.left + insets.right + getHgap() * 2);
            if (maxWidth <= 0) {
                return;
            }
            int hgap = getHgap();
            int vgap = getVgap();
            int x = insets.left + hgap;
            int y = insets.top + vgap;
            int rowHeight = 0;
            int rowStartX = x;

            for (Component component : target.getComponents()) {
                if (!component.isVisible()) {
                    continue;
                }
                Dimension size = component.getPreferredSize();
                if (x > rowStartX && x + size.width > insets.left + maxWidth + hgap) {
                    y += rowHeight + vgap;
                    x = rowStartX;
                    rowHeight = 0;
                }
                component.setBounds(x, y, size.width, size.height);
                if (size.height > rowHeight) {
                    rowHeight = size.height;
                }
                x += size.width + hgap;
            }
        }
    }
}
