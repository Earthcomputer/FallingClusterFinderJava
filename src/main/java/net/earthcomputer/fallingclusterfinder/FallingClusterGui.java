package net.earthcomputer.fallingclusterfinder;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

public class FallingClusterGui {
    private static final String[] NAMES = {"Xcom", "Kerb", "Cheater", "Earthcomputer", "coolmann", "punchster", "cortex", "Myren"};
    private static final String[] WITTY_COMMENTS = {
            "Don't listen to %s!",
            "%s was here!",
            "%s was crushed by a falling end portal frame!",
            "%s didn't know anything about the project, so he placed this hopper instead.",
            "%s claims this will be his final project. Lol.",
            "A wild %s appeared!",
            "%s claims this project will never see the light of day."
    };

    private JPanel mainPanel;
    private JSpinner hashSizeInput;
    private JSpinner renderDistanceInput;
    private JTextField spawnXInput;
    private JTextField spawnZInput;
    private JTextField glassXInput;
    private JTextField glassZInput;
    private JTextField unloadChunkSearchFromXInput;
    private JTextField unloadChunkSearchFromZInput;
    private JSpinner clusterChunkCountInput;
    private JTextField clusterChunksSearchFromXInput;
    private JTextField clusterChunksSearchFromZInput;
    private JSpinner rectangleWidthInput;
    private JSpinner searchLimitInput;
    private JButton addLineButton;
    private JButton removeLineButton;
    private JButton duplicateLineButton;
    private JButton removeAllLinesButton;
    private JScrollPane permaloaderScrollPane;
    private JPanel permaloaderComponent;
    private JPanel outputPanel;
    private JPanel unloadChunkPanel;
    private JPanel clusterChunksPanel;
    private JPanel permaloaderPanel;
    private JButton searchButton;
    private JButton cancelButton;
    private JScrollPane outputScrollPane;
    private JProgressBar progressBar;
    private final List<PermaloaderLinePanel> permaloaderLines = new ArrayList<>();

    public FallingClusterGui() {
        addLineButton.addActionListener(e -> {
            PermaloaderLinePanel newLine = new PermaloaderLinePanel();
            permaloaderComponent.add(newLine, permaloaderLines.size());
            permaloaderLines.add(newLine);
            permaloaderComponent.revalidate();
            permaloaderComponent.repaint();
            JScrollBar verticalScrollBar = permaloaderScrollPane.getVerticalScrollBar();
            if (verticalScrollBar != null) {
                verticalScrollBar.setValue(verticalScrollBar.getMaximum());
            }
        });
        removeLineButton.addActionListener(e -> {
            boolean changed = false;
            for (int i = 0; i < permaloaderLines.size(); i++) {
                if (permaloaderLines.get(i).checkBox.isSelected()) {
                    permaloaderComponent.remove(i);
                    permaloaderLines.remove(i--);
                    changed = true;
                }
            }
            if (changed) {
                permaloaderComponent.revalidate();
                permaloaderComponent.repaint();
            }
        });
        duplicateLineButton.addActionListener(e -> {
            PermaloaderLinePanel selectedLine = null;
            int selectedIndex = -1;
            for (int i = 0; i < permaloaderLines.size(); i++) {
                PermaloaderLinePanel line = permaloaderLines.get(i);
                if (line.checkBox.isSelected()) {
                    if (selectedLine != null) {
                        return;
                    }
                    selectedLine = line;
                    selectedIndex = i;
                }
            }
            if (selectedLine == null) {
                return;
            }
            selectedLine.checkBox.setSelected(false);
            PermaloaderLinePanel newLine = new PermaloaderLinePanel();
            newLine.xInput1.setText(selectedLine.xInput1.getText());
            newLine.zInput1.setText(selectedLine.zInput1.getText());
            newLine.xInput2.setText(selectedLine.xInput2.getText());
            newLine.zInput2.setText(selectedLine.zInput2.getText());
            permaloaderLines.add(selectedIndex + 1, newLine);
            permaloaderComponent.add(newLine, selectedIndex + 1);
            permaloaderComponent.revalidate();
            permaloaderComponent.repaint();
        });
        removeAllLinesButton.addActionListener(e -> {
            Component verticalBox = permaloaderComponent.getComponent(permaloaderLines.size());
            permaloaderComponent.removeAll();
            permaloaderComponent.add(verticalBox);
            permaloaderLines.clear();
            permaloaderComponent.revalidate();
            permaloaderComponent.repaint();
        });
        searchButton.addActionListener(e -> {
            FallingClusterFinder.cancel();
            FallingClusterFinder.start();
        });
        cancelButton.addActionListener(e -> FallingClusterFinder.cancel());

        mainPanel.setPreferredSize(new Dimension(Math.max(1024, mainPanel.getPreferredSize().width), mainPanel.getPreferredSize().height));
    }

    private void createUIComponents() {
        hashSizeInput = new JSpinner(new SpinnerNumberModel(1 << 12, 4, 1 << 24, 1) {
            @Override
            public Object getNextValue() {
                return Integer.parseInt(super.getValue().toString()) * 2;
            }

            @Override
            public Object getPreviousValue() {
                return Integer.parseInt(super.getValue().toString()) / 2;
            }
        });
        ((JSpinner.DefaultEditor) hashSizeInput.getEditor()).getTextField().setEditable(false);
        renderDistanceInput = new JSpinner(new SpinnerNumberModel(16, 2, 64, 1));
        clusterChunkCountInput = new JSpinner(new SpinnerNumberModel(100, 0, 10000, 1));
        searchLimitInput = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));

        permaloaderComponent = new JPanel();
        permaloaderComponent.setLayout(new BoxLayout(permaloaderComponent, BoxLayout.Y_AXIS));
        permaloaderComponent.add(Box.createVerticalGlue());

        unloadChunkPanel = new JPanel(new FlowLayout());
        unloadChunkPanel.setBorder(BorderFactory.createTitledBorder("Unload Chunk"));
        clusterChunksPanel = new JPanel(new FlowLayout());
        clusterChunksPanel.setBorder(BorderFactory.createTitledBorder("Cluster Chunks"));
        permaloaderPanel = new JPanel(new FlowLayout());
        permaloaderPanel.setBorder(BorderFactory.createTitledBorder("Permaloader"));
        outputScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outputScrollPane.setPreferredSize(new Dimension(25, 150));
        outputScrollPane.setMinimumSize(new Dimension(25, 150));
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Output"));
        outputPanel = new JPanel();
        outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));
    }

    private OptionalInt parseInt(JTextField input) {
        try {
            return OptionalInt.of(Integer.parseInt(input.getText()));
        } catch (NumberFormatException e) {
            markInvalid(input);
            return OptionalInt.empty();
        }
    }

    private void markInvalid(JTextField input) {
        DocumentListener prevListener = (DocumentListener) input.getClientProperty("falling.docListener");
        if (prevListener != null) {
            input.getDocument().removeDocumentListener(prevListener);
        }
        Border prevBorder = input.getBorder();
        input.setBorder(BorderFactory.createLineBorder(Color.RED));
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                input.setBorder(prevBorder);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                input.setBorder(prevBorder);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                input.setBorder(prevBorder);
            }
        };
        input.putClientProperty("falling.docListener", listener);
        input.getDocument().addDocumentListener(listener);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public int getHashSize() {
        return (int) hashSizeInput.getValue();
    }

    public int getRenderDistance() {
        return (int) renderDistanceInput.getValue();
    }

    public OptionalInt getSpawnX() {
        return parseInt(spawnXInput);
    }

    public OptionalInt getSpawnZ() {
        return parseInt(spawnZInput);
    }

    public OptionalInt getGlassX() {
        return parseInt(glassXInput);
    }

    public OptionalInt getGlassZ() {
        return parseInt(glassZInput);
    }

    public OptionalInt getUnloadChunkSearchFromX() {
        return parseInt(unloadChunkSearchFromXInput);
    }

    public OptionalInt getUnloadChunkSearchFromZ() {
        return parseInt(unloadChunkSearchFromZInput);
    }

    public int getClusterChunkCount() {
        return (int) clusterChunkCountInput.getValue();
    }

    public OptionalInt getClusterChunksSearchFromX() {
        return parseInt(clusterChunksSearchFromXInput);
    }

    public OptionalInt getClusterChunksSearchFromZ() {
        return parseInt(clusterChunksSearchFromZInput);
    }

    public int getRectangleWidth() {
        return (int) rectangleWidthInput.getValue();
    }

    public int getSearchLimit() {
        return (int) searchLimitInput.getValue();
    }

    public Optional<List<PermaloaderLine>> getPermaloaderLines() {
        List<PermaloaderLine> lines = new ArrayList<>(permaloaderLines.size());
        boolean valid = true;
        for (PermaloaderLinePanel line : permaloaderLines) {
            OptionalInt x1 = parseInt(line.xInput1);
            OptionalInt z1 = parseInt(line.zInput1);
            OptionalInt x2 = parseInt(line.xInput2);
            OptionalInt z2 = parseInt(line.zInput2);
            if (x1.isPresent() && z1.isPresent() && x2.isPresent() && z2.isPresent()) {
                if (Math.abs(x1.getAsInt() - x2.getAsInt()) != Math.abs(z1.getAsInt() - z2.getAsInt())) {
                    markInvalid(line.xInput1);
                    markInvalid(line.zInput1);
                    markInvalid(line.xInput2);
                    markInvalid(line.zInput2);
                    valid = false;
                } else {
                    lines.add(new PermaloaderLine(x1.getAsInt(), z1.getAsInt(), x2.getAsInt(), z2.getAsInt()));
                }
            } else {
                valid = false;
            }
        }
        return valid ? Optional.of(lines) : Optional.empty();
    }

    public void setRunning(boolean running) {
        searchButton.setEnabled(!running);
        cancelButton.setEnabled(running);
        if (running) {
            outputPanel.removeAll();
            outputPanel.revalidate();
            outputPanel.repaint();
        }
    }

    private void addOutput(JComponent output) {
        if (output instanceof JLabel) {
            JComponent c = output;
            output = new JPanel(new FlowLayout(FlowLayout.LEFT));
            output.add(c);
        }
        outputPanel.add(output);
        outputPanel.revalidate();
        outputPanel.repaint();
        JScrollBar verticalScrollBar = outputScrollPane.getVerticalScrollBar();
        if (verticalScrollBar != null) {
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        }
    }

    public void addChunkOutput(String label, Point chunk) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(label + ": "));
        JTextPane xPane = new JTextPane();
        xPane.setContentType("text/html");
        xPane.setEditable(false);
        xPane.setBackground(null);
        xPane.setBorder(null);
        xPane.setText(String.valueOf(chunk.x));
        panel.add(xPane);
        panel.add(new JLabel(","));
        JTextPane zPane = new JTextPane();
        zPane.setContentType("text/html");
        zPane.setEditable(false);
        zPane.setBackground(null);
        zPane.setBorder(null);
        zPane.setText(String.valueOf(chunk.y));
        panel.add(zPane);
        addOutput(panel);
    }

    public void addClusterChunksOutput(Point rectangleOrigin, Point rectangleSize, int glassHash, int hashSize, List<Point> chunks) {
        addOutput(new JLabel(String.format(
                "<html>Cluster chunks found between chunks (%d, %d) to (%d, %d)</html>",
                rectangleOrigin.x, rectangleOrigin.y,
                rectangleOrigin.x + rectangleSize.x, rectangleOrigin.y + rectangleSize.y
        )));
        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton saveToCsvButton = new JButton("Save to CSV");
        saveToCsvButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("cluster_chunks.csv"));
            if (fileChooser.showSaveDialog(this.mainPanel) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                    writer.println("x,z");
                    for (Point chunk : chunks) {
                        writer.println(String.format("%d,%d", chunk.x, chunk.y));
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this.mainPanel, "Failed to save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        outputPanel.add(saveToCsvButton);

        JButton saveSetupMcFunctionButton = new JButton("Save Setup MC Function");
        saveSetupMcFunctionButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("cluster_setup.mcfunction"));
            if (fileChooser.showSaveDialog(this.mainPanel) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                    writer.println("# File generated by falling-cluster-finder");
                    writer.println("kill @e[tag=cluster]");
                    writer.println("summon armor_stand ~ ~ ~ {Tags:[\"cluster\"]}");
                    writer.println("scoreboard objectives add numchunks dummy");
                    writer.println("tellraw @a \"Run \\\"scoreboard players set @e[tag=cluster] numchunks <n>\\\" to set the number of chunks in the cluster you want to load.\"");
                    for (Point chunk : chunks) {
                        writer.println("loadchunk " + chunk.x + " " + chunk.y);
                        Random rand = new Random(OpenHashMap.hash(chunk, 0));
                        String hopperName = String.format(WITTY_COMMENTS[rand.nextInt(WITTY_COMMENTS.length)], NAMES[rand.nextInt(NAMES.length)]);
                        writer.println(String.format("setblock %d 0 %d hopper default replace {CustomName:\"%s\", " +
                                "Items:[{Slot: 2b, id: \"minecraft:bedrock\", tag:{display:{Name: \"Gray Concrete\"}}}]}",
                                chunk.x * 16 + 8, chunk.y * 16 + 8, hopperName));
                        writer.println(String.format("setblock %d 1 %d dropper", chunk.x * 16 + 8, chunk.y * 16 + 8));
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this.mainPanel, "Failed to save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        outputPanel.add(saveSetupMcFunctionButton);

        JButton loadClusterMcFunctionButton = new JButton("Save Cluster Load MC Function");
        loadClusterMcFunctionButton.addActionListener(e -> {
            List<Point> clusterChunks = new ArrayList<>(chunks);
            clusterChunks.sort(Comparator.comparingInt(p -> (OpenHashMap.hash(p, hashSize) + hashSize - glassHash) & (hashSize - 1)));
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("cluster_load.mcfunction"));
            if (fileChooser.showSaveDialog(this.mainPanel) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                    writer.println("# File generated by falling-cluster-finder");
                    for (int i = 0; i < clusterChunks.size(); i++) {
                        Point chunk = clusterChunks.get(i);
                        writer.println(String.format("execute @e[score_numchunks_min=%d] ~ ~ ~ loadchunk %d %d", i + 1, chunk.x, chunk.y));
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this.mainPanel, "Failed to save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        outputPanel.add(loadClusterMcFunctionButton);

        addOutput(outputPanel);
    }

    public void addRehashWarning(int sizeBefore, int hashSizeAfter) {
        JLabel warning = new JLabel("<html>Warning: upwards rehash will occur while loading player chunks.</html>");
        warning.setForeground(Color.ORANGE);
        addOutput(warning);
        addOutput(new JLabel("<html>To mitigate this, load " + (hashSizeAfter * 3 / 8 - sizeBefore) + " chunks " +
                "before you load the cluster chunks, and unload them as you load the player chunks.<br>Make sure " +
                "during the player loading, you always stay above " + (hashSizeAfter / 4) + " loaded chunks in " +
                "the world.</html>"));
        if (sizeBefore <= hashSizeAfter / 4) {
            warning = new JLabel("<html>Warning: downwards rehash will occur when unloading player chunks.</html>");
            warning.setForeground(Color.ORANGE);
            addOutput(warning);
            addOutput(new JLabel("<html>To mitigate, do the above after every re-attempt.</html>"));
        }
    }

    public void addHashMapFullError() {
        JLabel output = new JLabel("<html>HashMap exceeded rehash threshold. Consider using a larger hash size.</html>");
        output.setForeground(Color.RED);
        addOutput(output);
    }

    public void addGlassChunkCollidesError(List<Point> nearbyValid) {
        addOutput(new JLabel("<html>Glass chunk has hash collision with the spawn chunks or permaloader!<br>Nearby valid chunks:</html>"));
        for (int i = 0; i < nearbyValid.size(); i++) {
            addChunkOutput(String.valueOf(i + 1), nearbyValid.get(i));
        }
    }

    public static class PermaloaderLine {
        private final int x1;
        private final int z1;
        private final int x2;
        private final int z2;

        public PermaloaderLine(int x1, int z1, int x2, int z2) {
            this.x1 = x1;
            this.z1 = z1;
            this.x2 = x2;
            this.z2 = z2;
        }

        public int getX1() {
            return x1;
        }

        public int getZ1() {
            return z1;
        }

        public int getX2() {
            return x2;
        }

        public int getZ2() {
            return z2;
        }
    }

    private static class PermaloaderLinePanel extends JPanel {
        private final JCheckBox checkBox = new JCheckBox();
        private final JTextField xInput1 = new JTextField("0");
        private final JTextField zInput1 = new JTextField("0");
        private final JTextField xInput2 = new JTextField("0");
        private final JTextField zInput2 = new JTextField("0");

        public PermaloaderLinePanel() {
            setBorder(BorderFactory.createLineBorder(Color.BLACK));
            setLayout(new FlowLayout(FlowLayout.LEFT));
            add(checkBox);
            JPanel inputPanel = new JPanel();
            inputPanel.setLayout(new GridBagLayout());
            inputPanel.add(new JLabel("From:"), gridBag(0, 0, 0));
            xInput1.setColumns(4);
            inputPanel.add(xInput1, gridBag(1, 0, 1));
            inputPanel.add(new JLabel(","), gridBag(2, 0, 0));
            zInput1.setColumns(4);
            inputPanel.add(zInput1, gridBag(3, 0, 1));
            inputPanel.add(new JLabel("To:"), gridBag(0, 1, 0));
            xInput2.setColumns(4);
            inputPanel.add(xInput2, gridBag(1, 1, 1));
            inputPanel.add(new JLabel(","), gridBag(2, 1, 0));
            zInput2.setColumns(4);
            inputPanel.add(zInput2, gridBag(3, 1, 1));
            add(inputPanel);
        }

        private static GridBagConstraints gridBag(int x, int y, double weight) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = x;
            c.gridy = y;
            c.weightx = weight;
            return c;
        }
    }
}
