import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class SimulatorGUI extends JFrame {

    private JTextField pcField, irField;
    private final JTextField[] gprFields = new JTextField[4];
    private final JTextField[] ixrFields = new JTextField[3];
    private JTextArea consoleOutputArea, memoryDisplayArea, cacheDisplayArea, printerOutputArea;
    private JTextField keyboardInputField, memoryAddressField, memoryValueField;

    private JButton keyboardSubmitButton;
    private final CPU cpu;
    private boolean isRunning = false;
    private boolean waitingForInput = false;

    private int keyboardInputBuffer = -1;
    private final Queue<Integer> fileInputBuffer = new LinkedList<>();

    public SimulatorGUI() {
        cpu = new CPU();
        cpu.setGUI(this);
        setTitle("TEAM 7 - CSCI 6461 CPU Simulator (Part 3)");
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(createControlPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createConsolePanel(), BorderLayout.SOUTH);

        setVisible(true);
        updateGUI();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        JButton iplButton = new JButton("IPL");
        JButton loadParagraphButton = new JButton("Load Paragraph");
        JButton runButton = new JButton("Run");
        JButton singleStepButton = new JButton("Single Step");
        JButton haltButton = new JButton("Halt");

        panel.add(iplButton);
        panel.add(loadParagraphButton);
        panel.add(runButton);
        panel.add(singleStepButton);
        panel.add(haltButton);

        iplButton.addActionListener(e -> iplAction());
        loadParagraphButton.addActionListener(e -> loadParagraphAction());
        singleStepButton.addActionListener(e -> singleStepAction());
        runButton.addActionListener(e -> runAction());
        haltButton.addActionListener(e -> { isRunning = false; waitingForInput = false; });
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));
        panel.add(createRegisterPanel());
        panel.add(createMemoryPanel());
        panel.add(createCachePanel());
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("CPU Registers (Octal)"));
        pcField = addRegisterField(panel, "PC:");
        irField = addRegisterField(panel, "IR:");
        addRegisterField(panel, "MAR:"); addRegisterField(panel, "MBR:");
        addRegisterField(panel, "MFR:"); addRegisterField(panel, "CC:");
        for(int i=0; i<4; i++) gprFields[i] = addRegisterField(panel, "GPR"+i+":");
        for(int i=0; i<3; i++) ixrFields[i] = addRegisterField(panel, "IXR"+(i+1)+":");
        return panel;
    }

    private JTextField addRegisterField(JPanel p, String l) {
        p.add(new JLabel(l)); JTextField f = new JTextField(6); p.add(f); return f;
    }

    private JPanel createMemoryPanel() {
        JPanel p = new JPanel(new BorderLayout(5,5));
        p.setBorder(BorderFactory.createTitledBorder("Main Memory"));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memoryAddressField = new JTextField(5); memoryValueField = new JTextField(6);
        JButton go = new JButton("Go"); JButton dep = new JButton("Deposit");
        top.add(new JLabel("Addr:")); top.add(memoryAddressField); top.add(go);
        top.add(new JLabel("Val:")); top.add(memoryValueField); top.add(dep);
        p.add(top, BorderLayout.NORTH);
        memoryDisplayArea = new JTextArea(15,30); p.add(new JScrollPane(memoryDisplayArea), BorderLayout.CENTER);
        go.addActionListener(e -> updateMemoryView());
        dep.addActionListener(e -> {
            try { cpu.writeMemory(Integer.parseInt(memoryAddressField.getText(),8), Integer.parseInt(memoryValueField.getText(),8)); updateGUI(); }
            catch(Exception ex){}
        });
        return p;
    }

    private JPanel createCachePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Cache"));
        cacheDisplayArea = new JTextArea(15,30); p.add(new JScrollPane(cacheDisplayArea));
        return p;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        consoleOutputArea = new JTextArea(5, 40);
        panel.add(new JScrollPane(consoleOutputArea), BorderLayout.CENTER);

        JPanel ioPanel = new JPanel(new GridLayout(1, 2));
        JPanel keyPanel = new JPanel(new BorderLayout());
        keyPanel.setBorder(BorderFactory.createTitledBorder("Keyboard (Dev 0)"));
        keyboardInputField = new JTextField();

        keyboardSubmitButton = new JButton("Submit");

        keyPanel.add(keyboardInputField, BorderLayout.CENTER);
        keyPanel.add(keyboardSubmitButton, BorderLayout.EAST);

        JPanel prnPanel = new JPanel(new BorderLayout());
        prnPanel.setBorder(BorderFactory.createTitledBorder("Printer (Dev 1)"));
        printerOutputArea = new JTextArea(5, 20);
        prnPanel.add(new JScrollPane(printerOutputArea), BorderLayout.CENTER);

        ioPanel.add(keyPanel); ioPanel.add(prnPanel);
        panel.add(ioPanel, BorderLayout.SOUTH);

        keyboardSubmitButton.addActionListener(e -> {
            String text = keyboardInputField.getText().trim();
            if (text.matches("-?[0-9]+")) {
                keyboardInputBuffer = Integer.parseInt(text) & 0xFFFF;
            } else if (text.length() == 1) {
                keyboardInputBuffer = text.charAt(0);
            } else {
                if (!text.isEmpty()) keyboardInputBuffer = text.charAt(0);
            }

            consoleOutputArea.append("Input buffered: " + keyboardInputBuffer + "\n");
            keyboardInputField.setText("");
            if(waitingForInput) { waitingForInput = false; runAction(); }
        });

        return panel;
    }

    public int readFromDevice(int devId) {
        if (devId == 0) {
            if (keyboardInputBuffer == -1) {
                if (isRunning) { isRunning = false; waitingForInput = true; }
                consoleOutputArea.append("Waiting for Keyboard Input...\n");
                return 0;
            }
            int val = keyboardInputBuffer; keyboardInputBuffer = -1;
            return val;
        } else if (devId == 2) {
            if (fileInputBuffer.isEmpty()) return 0;
            return fileInputBuffer.poll();
        }
        return 0;
    }

    public void writeToDevice(int devId, int val) {
        if (devId == 1) { // Printer
            // FIX: Simply cast to char and append. This handles letters, spaces, and newlines.
            printerOutputArea.append(String.valueOf((char)val));
        }
    }

    public boolean isWaitingForInput() { return waitingForInput; }

    private void loadParagraphAction() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (Scanner s = new Scanner(fc.getSelectedFile())) {
                fileInputBuffer.clear();
                while(s.hasNextLine()) {
                    String line = s.nextLine() + "\n";
                    for(char c : line.toCharArray()) fileInputBuffer.add((int)c);
                }
                fileInputBuffer.add(0);
                consoleOutputArea.append("Paragraph file loaded into Device 2 buffer.\n");
            } catch (Exception ex) {
                consoleOutputArea.append("Error loading paragraph: " + ex.getMessage() + "\n");
            }
        }
    }

    private void iplAction() {
        cpu.reset();
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (Scanner s = new Scanner(fc.getSelectedFile())) {
                boolean first=true;
                while(s.hasNextLine()) {
                    String line = s.nextLine().trim();
                    if(line.isEmpty()) continue;
                    String[] p = line.split("\\s+");
                    if(p.length==2) {
                        int addr = Integer.parseInt(p[0],8);
                        int val = Integer.parseInt(p[1],8);
                        cpu.writeToMemory(addr, val);
                        if(first) { cpu.PC = addr; first=false; }
                    }
                }
                consoleOutputArea.append("Program loaded.\n");
                updateGUI();
            } catch (Exception ex) {
                consoleOutputArea.append("Error loading program: " + ex.getMessage() + "\n");
            }
        }
    }

    private void runAction() {
        if(isRunning) return;
        isRunning = true;
        SwingWorker<Void,Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                while(isRunning && cpu.MFR == 0) {
                    boolean cont = cpu.executeInstruction();
                    if (cpu.gui.isWaitingForInput()) { isRunning=false; waitingForInput=true; }
                    if (!cont) isRunning=false;
                    publish();
                    try { Thread.sleep(2); } catch(Exception e){}
                }
                return null;
            }
            @Override protected void process(java.util.List<Void> c) { updateGUI(); }
            @Override protected void done() {
                updateGUI();
                if(cpu.MFR != 0) consoleOutputArea.append("Fault: " + cpu.MFR + "\n");
                else if(!waitingForInput) consoleOutputArea.append("Halted.\n");
            }
        };
        worker.execute();
    }

    private void singleStepAction() {
        if(waitingForInput) { consoleOutputArea.append("Waiting for input.\n"); return; }
        cpu.executeInstruction();
        updateGUI();
    }

    private void updateGUI() {
        pcField.setText(String.format("%04o", cpu.PC));
        irField.setText(String.format("%06o", cpu.IR));
        for(int i=0; i<4; i++) gprFields[i].setText(String.format("%06o", cpu.getGPR(i)));
        for(int i=0; i<3; i++) ixrFields[i].setText(String.format("%06o", cpu.getIXR(i+1)));

        if (cpu.cache != null) {
            cacheDisplayArea.setText(cpu.cache.getCacheStateForGUI());
        }

        updateMemoryView();
    }

    private void updateMemoryView() {
        try {
            int start = Integer.parseInt(memoryAddressField.getText().isEmpty()?"0":memoryAddressField.getText(), 8);
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<20; i++) {
                if(start+i < 2048) sb.append(String.format("%04o: %06o\n", start+i, cpu.fetchFromMemory(start+i)));
            }
            memoryDisplayArea.setText(sb.toString());
        } catch(Exception e) {}
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(SimulatorGUI::new); }
}