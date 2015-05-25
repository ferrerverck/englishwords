package com.words.gui.stats;

import com.words.controller.Controller;
import com.words.controller.sound.PlayWav;
import com.words.controller.utils.DateTimeUtils;
import com.words.gui.guiutils.GuiUtils;
import com.words.gui.guiutils.ComplexityGuiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * Panel with comprehensive word statistics.
 * @author vlad
 */
public class StatsPanel extends JPanel {
    
    private static final int WORD_LIST_LENGTH = 12;
    
    // class is required to work with lists
    private static class Pair {
        
        private String key;
        private Object value;
        private ImageIcon icon;
        
        public Pair(String key, Object value) {
            this(key, value, null);
        }
        
        public Pair(String key, Object value, ImageIcon icon) {
            this.key = key;
            this.value = value;
            this.icon = icon;
        }
        
        @Override
        public String toString() { return key + "=" + value.toString(); }
    }
    
    // delays statistic loading
    private boolean firstShow = true;
    
    private final JPanel centerPanel = new JPanel(new GridBagLayout());
    private final JScrollPane scrollPane;
    
    private final JButton updateButton = GuiUtils.newNotFocusableButton("Update statistics");
    
    private final JList<Pair> statsList;
    private final StatsListModel statsListModel;
    
    private final JList<Pair> frequentList;
    private final StatsListModel frequentListModel;
    
    private final JList<Pair> oldestList;
    private final StatsListModel oldestListModel;
    
    private final ComplexityPanel complPanel;
    
    private transient final Controller controller;
    
    public StatsPanel(Controller ctrl, Font font) {
        this.controller = ctrl;
        complPanel = new ComplexityPanel(font);
        
//        setPreferredSize(new Dimension(300, 500));
        
        setBorder(null);
        
        setLayout(new BorderLayout());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(updateButton);
        bottomPanel.setBorder(null);
        add(bottomPanel, BorderLayout.SOUTH);
        
        updateButton.addActionListener(e -> {
            update();
            PlayWav.notification();
        });
        
        scrollPane = new JScrollPane(centerPanel,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.setLayout(new GridBagLayout());
        add(scrollPane, BorderLayout.CENTER);
        
//        scrollPane.addComponentListener(new ComponentAdapter() {
//
//            @Override
//            public void componentResized(ComponentEvent e) {
//                Dimension d = centerPanel.getPreferredSize();
//                d.width = scrollPane.getWidth() - 10;
//                centerPanel.setPreferredSize(d);
//            }
//        });
        
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(6, 6, 6, 14);
        c.anchor = GridBagConstraints.NORTHWEST;
        
        Border complBorder = new TitledBorder(new LineBorder(Color.GRAY),
            "Amount of words grouped by Complexity", TitledBorder.CENTER,
            TitledBorder.TOP, font);
        
        complPanel.setBorder(complBorder);
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(complPanel, c);
        
        statsListModel = new StatsListModel(ctrl) {
            
            @Override
            protected void setValues() {
                list.addAll(Arrays.asList(
                    new Pair("Total number of words",
                        controller.getTotalWordAmount()),
                    new Pair("Repeat words", controller.getRepeatWordAmount()),
                    new Pair("Bundles", controller.getTotalBundleAmount()),
                    new Pair("Last bundle", DateTimeUtils.localDateToString(
                        controller.getLastBundleName())),
                    new Pair("Average word length", String.format("%.1f",
                        controller.getAverageWordLength())),
                    new Pair("Future words", controller.getFutureWordsAmount()),
                    new Pair("Today iterations",
                        controller.getTodayIterations()),
                    new Pair("Total iteration amount",
                        controller.getTotalIterations()),
                    new Pair("This week iterations",
                        controller.getThisWeekIterations()),
                    new Pair("Last 7 days iterations",
                        controller.getIterationsForDays(7)),
                    new Pair("Average complexity weight",
                        controller.getAverageComplexityWeight())
                ));
            }
        };
        
        statsList = new JList<>(statsListModel);
        statsList.setOpaque(false);
        statsList.setCellRenderer(new StatsListCellRenderer(font));
        statsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
//        statsList.setVisibleRowCount(-1);
        
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        centerPanel.add(statsList, c);
        
        oldestListModel = new StatsListModel((ctrl)) {
            
            @Override
            protected void setValues() {
                controller.getOldestPickedWords(WORD_LIST_LENGTH).stream().forEach(w -> {
                    list.add(new Pair(w.getWord(),
                        DateTimeUtils.getStringFromMillis(w.getLastPickedTimestamp()),
                        ComplexityGuiUtils.getIcon(w.getComplexity())));
                });
            }
        };
        
        JPanel wordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wordPanel.setBorder(null);
        
        oldestList = new JList<>(oldestListModel);
        oldestList.setOpaque(false);
        oldestList.setCellRenderer(new StatsListCellRenderer(font));
        oldestList.setLayoutOrientation(JList.VERTICAL);
        
        Border titledBorder = new TitledBorder(new LineBorder(Color.GRAY),
            "Oldest picked words", TitledBorder.LEFT, TitledBorder.TOP, font);
        oldestList.setBorder(titledBorder);
        
        wordPanel.add(oldestList);
        wordPanel.add(Box.createHorizontalStrut(10));
        
        frequentListModel = new StatsListModel(ctrl) {
            
            @Override
            protected void setValues() {
                controller.getMostFrequentlyUsedWords(WORD_LIST_LENGTH).stream().forEach(w -> {
                    list.add(new Pair(w.getWord(), w.getTimesPicked(),
                        ComplexityGuiUtils.getIcon(w.getComplexity())));
                });
            }
        };
        
        frequentList = new JList<>(frequentListModel);
        frequentList.setOpaque(false);
        frequentList.setCellRenderer(new StatsListCellRenderer(font));
        frequentList.setLayoutOrientation(JList.VERTICAL);
        
        Border frequentBorder = new TitledBorder(new LineBorder(Color.GRAY),
            "Frequently picked words", TitledBorder.LEFT, TitledBorder.TOP,
            font);
        frequentList.setBorder(frequentBorder);
        
        wordPanel.add(frequentList);
        
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0;
        c.weighty = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        centerPanel.add(wordPanel, c);
    }
    
//    @Override
//    public Dimension getPreferredSize() {
////        int width = 1000;
//        int height = complPanel.getHeight() + statsList.getHeight() + oldestList.getHeight();
//        return new Dimension(1000, height);
//    }
    
    public void showPanel() {
        if (firstShow) {
            firstShow = false;
            update();
            scrollPane.revalidate();
        }
    }
    
    private void update() {
        statsListModel.update();
        frequentListModel.update();
        oldestListModel.update();
        complPanel.updateValues(controller.groupWordsByComplexity());
    }
    
    private abstract static class StatsListModel extends AbstractListModel<Pair> {
        
        protected final List<Pair> list = new ArrayList<>();
        protected final Controller controller;
        
        protected abstract void setValues();
        
        public StatsListModel(Controller ctrl) {
            this.controller = ctrl;
        }
        
        @Override
        public int getSize() {
            return list.size();
        }
        
        @Override
        public Pair getElementAt(int index) {
            return list.get(index);
        }
        
        public void update() {
            if (controller == null) return;
            
            list.clear();
            
            setValues();
            
            fireContentsChanged(this, 0, getSize() - 1);
        }
    }
    
    private static class StatsListCellRenderer extends JPanel implements
        ListCellRenderer<Pair> {
        
        private static final Dimension PREFERRED_SIZE = new Dimension(300, 30);
        private static final int GAP_BETWEEN_WORDS = 14;
        
        private final JPanel panel = new JPanel();
        private final JLabel descriptionLabel = new JLabel("description");
        private final JLabel valueLabel = new JLabel("value");
        
        public StatsListCellRenderer(Font font) {
//            setMinimumSize(PREFERRED_SIZE);
            setPreferredSize(PREFERRED_SIZE);
            
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(2, 2, 2, 2));
            
            panel.setLayout(new BorderLayout(GAP_BETWEEN_WORDS, 0));
            panel.setOpaque(true);
            panel.setBorder(new CompoundBorder(new LineBorder(Color.GRAY, 1),
                new EmptyBorder(3, 4, 3, 4)));
            
            descriptionLabel.setFont(font);
            valueLabel.setFont(font);
            
            panel.add(descriptionLabel, BorderLayout.WEST);
            panel.add(valueLabel, BorderLayout.EAST);
            add(panel, BorderLayout.CENTER);
        }
        
        private void setCellForeground(Color color) {
            descriptionLabel.setForeground(color);
            valueLabel.setForeground(color);
        }
        
        @Override
        public Component getListCellRendererComponent(JList<? extends Pair> list,
            Pair value, int index, boolean isSelected, boolean cellHasFocus) {
            descriptionLabel.setText(value.key);
            descriptionLabel.setIcon(value.icon);
            valueLabel.setText(value.value.toString());
            
            if (isSelected && cellHasFocus) {
                setCellForeground(Color.WHITE);
                panel.setBackground(GuiUtils.SELECTION_COLOR);
            } else {
                setCellForeground(Color.BLACK);
                panel.setBackground(index % 2 == 0 ?
                    GuiUtils.EVEN_COLOR : GuiUtils.ODD_COLOR);
            }
            
            return this;
        }
    }
}
