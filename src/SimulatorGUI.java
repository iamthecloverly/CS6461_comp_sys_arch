import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class SimulatorGUI extends JFrame {

    // --- GUI Components ---
    private JTextField pcField, irField, marField, mbrField, mfrField, ccField;
    private final JTextField[] gprFields = new JTextField[4]; // General Purpose Registers R0-R3
    private final JTextField[] ixrFields = new JTextField[3]; // Index Registers X1-X3
    private JTextArea consoleOutputArea;
    private JTextArea memoryDisplayArea;
    private JTextArea cacheDisplayArea; // <-- NEW
    private JTextField memoryAddressField, memoryValueField;

    // --- CPU Instance ---
    private final CPU cpu;
    private boolean isRunning = false; // Flag to control the run loop

    public SimulatorGUI() {
        // --- Frame Setup ---
        setTitle("TEAM 7 - CSCI 6461 CPU Simulator (Part 2)");
        setSize(1200, 750); // <-- Increased width for cache
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout(10, 10));

        // --- South Panel: Console (Create this first for CPU) ---
        JPanel consolePanel = createConsolePanel();
        add(consolePanel, BorderLayout.SOUTH);

        // --- CPU Instance (Pass console to it) ---
        cpu = new CPU(consoleOutputArea); // <-- UPDATED

        // --- Top Panel: Control Buttons ---
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // --- Center Panel: Registers and Memory ---
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        // --- Finalize ---
        setVisible(true);
        updateGUI(); // Initial update to show all zeros
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton iplButton = new JButton("IPL");
        JButton runButton = new JButton("Run");
        JButton singleStepButton = new JButton("Single Step");
        JButton haltButton = new JButton("Halt");

        panel.add(iplButton);
        panel.add(runButton);
        panel.add(singleStepButton);
        panel.add(haltButton);

        // --- Action Listeners for Control Buttons ---
        iplButton.addActionListener(e -> iplAction());
        singleStepButton.addActionListener(e -> singleStepAction());
        runButton.addActionListener(e -> runAction());
        haltButton.addActionListener(e -> {
            isRunning = false;
            consoleOutputArea.append("Halt button pressed. Execution stopped.\n");
        });

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0)); // <-- 3 columns
        panel.add(createRegisterPanel());
        panel.add(createMemoryPanel());
        panel.add(createCachePanel()); // <-- NEW
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5)); // Two columns for labels and fields
        panel.setBorder(BorderFactory.createTitledBorder("CPU Registers (Octal)"));

        // Add fields for all registers
        pcField = addRegisterField(panel, "PC (Program Counter):");
        irField = addRegisterField(panel, "IR (Instruction Register):");
        marField = addRegisterField(panel, "MAR (Memory Address Reg):");
        mbrField = addRegisterField(panel, "MBR (Memory Buffer Reg):");
        mfrField = addRegisterField(panel, "MFR (Machine Fault Reg):");
        ccField = addRegisterField(panel, "CC (Condition Code):");

        for (int i = 0; i < 4; i++) {
            gprFields[i] = addRegisterField(panel, "GPR" + i + ":");
        }
        for (int i = 0; i < 3; i++) {
            ixrFields[i] = addRegisterField(panel, "IXR" + (i + 1) + ":");
        }
        return panel;
    }

    private JTextField addRegisterField(JPanel panel, String label) {
        panel.add(new JLabel(label));
        JTextField field = new JTextField(6);
        panel.add(field);
        return field;
    }

    private JPanel createMemoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Main Memory (Octal)"));

        JPanel memoryInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memoryInputPanel.add(new JLabel("Address:"));
        memoryAddressField = new JTextField(5);
        memoryInputPanel.add(memoryAddressField);
        JButton goButton = new JButton("Go");
        memoryInputPanel.add(goButton);

        memoryInputPanel.add(new JLabel("Value:"));
        memoryValueField = new JTextField(6);
        memoryInputPanel.add(memoryValueField);
        JButton depositButton = new JButton("Deposit");
        memoryInputPanel.add(depositButton);
        panel.add(memoryInputPanel, BorderLayout.NORTH);

        memoryDisplayArea = new JTextArea(15, 30);
        memoryDisplayArea.setEditable(false);
        memoryDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(memoryDisplayArea), BorderLayout.CENTER);

        // --- Action Listeners for Memory Buttons ---
        goButton.addActionListener(e -> memoryGoAction());
        depositButton.addActionListener(e -> memoryDepositAction());

        return panel;
    }

    // --- NEW PANEL FOR CACHE ---
    private JPanel createCachePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Cache Content"));
        cacheDisplayArea = new JTextArea(15, 30);
        cacheDisplayArea.setEditable(false);
        cacheDisplayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(cacheDisplayArea), BorderLayout.CENTER);
        return panel;
    }


    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Console I/O"));
        consoleOutputArea = new JTextArea(5, 40);
        consoleOutputArea.setEditable(false);
        panel.add(new JScrollPane(consoleOutputArea), BorderLayout.CENTER);
        return panel;
    }

    // --- All Action Logic ---

    private void iplAction() {
        consoleOutputArea.append("IPL pressed! Resetting CPU and loading program...\n");
        cpu.reset();

        JFileChooser fileChooser = new JFileChooser("."); // Start in current directory
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (Scanner fileScanner = new Scanner(selectedFile)) {
                boolean firstLine = true;
                while (fileScanner.hasNextLine()) {
                    String line = fileScanner.nextLine().trim();
                    if (line.isEmpty())
                        continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2) {
                        int address = Integer.parseInt(parts[0], 8);
                        int value = Integer.parseInt(parts[1], 8);
                        cpu.writeMemory(address, value); // Will write to mem and cache
                        if (firstLine) {
                            cpu.PC = address; // Set PC to the first address in the file
                            firstLine = false;
                        }
                    }
                }
                consoleOutputArea.append("File '" + selectedFile.getName() + "' loaded successfully.\n");
            } catch (IOException | NumberFormatException ex) {
                consoleOutputArea.append("Error loading program: " + ex.getMessage() + "\n");
                JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage(), "File Load Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        updateGUI();
    }

    private void singleStepAction() {
        if (cpu.MFR != 0) {
            consoleOutputArea.append("CPU is in a fault state. Reset required.\n");
            return;
        }
        consoleOutputArea.append("Executing single step...\n");
        boolean shouldContinue = cpu.executeInstruction();
        updateGUI();
        if (!shouldContinue) {
            consoleOutputArea.append("Execution halted by HLT instruction or fault.\n");
        }
    }

    private void runAction() {
        if (isRunning)
            return; // Prevent multiple run loops
        isRunning = true;
        consoleOutputArea.append("Run pressed! Executing...\n");

        // Use a SwingWorker to prevent the GUI from freezing during execution
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Loop until isRunning is false (Halt button), a fault occurs, or HLT is
                // executed
                while (isRunning && cpu.MFR == 0) {
                    boolean shouldContinue = cpu.executeInstruction();
                    if (!shouldContinue) {
                        isRunning = false; // Stop the loop if HLT is encountered
                    }
                    publish(); // Triggers process() to update GUI on the Event Dispatch Thread
                    Thread.sleep(50); // Small delay to make execution visible
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Void> chunks) {
                // This is called on the GUI thread to update it safely
                updateGUI();
            }

            @Override
            protected void done() {
                isRunning = false; // Ensure flag is reset
                updateGUI();
                if (cpu.MFR != 0) {
                    consoleOutputArea.append("Execution halted due to Machine Fault: " + cpu.MFR + "\n");
                } else {
                    consoleOutputArea.append("Execution finished or was halted.\n");
                }
            }
        };
        worker.execute();
    }

    private void memoryGoAction() {
        try {
            int address = Integer.parseInt(memoryAddressField.getText(), 8);
            int value = cpu.readMemory(address); // Assumes readMemory handles bounds checks
            memoryValueField.setText(String.format("%06o", value));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid octal address.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void memoryDepositAction() {
        try {
            int address = Integer.parseInt(memoryAddressField.getText(), 8);
            int value = Integer.parseInt(memoryValueField.getText(), 8);
            cpu.writeMemory(address, value);
            updateGUI(); // Refresh memory and cache view
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid octal address or value.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Update GUI Method ---
    private void updateGUI() {
        pcField.setText(String.format("%04o", cpu.PC));
        irField.setText(String.format("%06o", cpu.IR));
        marField.setText(String.format("%04o", cpu.MAR));
        mbrField.setText(String.format("%06o", cpu.MBR));
        mfrField.setText(String.format("%o", cpu.MFR));
        ccField.setText(String.format("%o", cpu.CC));

        for (int i = 0; i < 4; i++) {
            gprFields[i].setText(String.format("%06o", cpu.getGPR(i)));
        }
        for (int i = 0; i < 3; i++) {
            ixrFields[i].setText(String.format("%06o", cpu.getIXR(i + 1)));
        }

        // Update memory display to show 20 lines around the PC
        StringBuilder memText = new StringBuilder();
        int startAddr = Math.max(0, cpu.PC - 10);
        for (int i = 0; i < 20; i++) {
            int currentAddr = startAddr + i;
            if (currentAddr < 2048) {
                // We must read directly from memory for this display,
                // or else the display itself will fill the cache.
                memText.append(String.format("%04o: %06o\n", currentAddr, cpu.memory[currentAddr]));
            }
        }
        memoryDisplayArea.setText(memText.toString());

        // Update cache display
        cacheDisplayArea.setText(cpu.getCacheContents());
    }

    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(SimulatorGUI::new);
    }
}