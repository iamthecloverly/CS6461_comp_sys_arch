import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assembler {

    private static final Map<String, Integer> symbolTable = new HashMap<>();
    private static final Map<String, Integer> opcodeTable = new HashMap<>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar Assembler.jar <source_file.txt>");
            return;
        }
        String sourceFileName = args[0];
        String listingFileName = sourceFileName.replace(".txt", "_listing.txt");
        String loadFileName = sourceFileName.replace(".txt", "_load.txt");

        initializeOpcodeTable();

        try {
            System.out.println("--- Starting Pass 1: Building Symbol Table ---");
            performPass1(sourceFileName);
            System.out.println("Symbol Table constructed successfully:");
            symbolTable.forEach((label, address) ->
                    System.out.printf("  Label: %-10s Address: %d (0o%o)\n", label, address, address));
            System.out.println("--- Pass 1 Complete ---\n");

            System.out.println("--- Starting Pass 2: Generating Machine Code ---");
            performPass2(sourceFileName, listingFileName, loadFileName);
            System.out.println("--- Pass 2 Complete ---");

            System.out.println("\nAssembly successful!");
            System.out.println("=> Listing File generated: " + listingFileName);
            System.out.println("=> Load File generated: " + loadFileName);

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeOpcodeTable() {
        // (Opcodes from previous version, no changes here)
        opcodeTable.put("HLT", 0); opcodeTable.put("TRAP", 30);
        opcodeTable.put("LDR", 1); opcodeTable.put("STR", 2); opcodeTable.put("LDA", 3);
        opcodeTable.put("LDX", 41); opcodeTable.put("STX", 42);
        opcodeTable.put("JZ", 10); opcodeTable.put("JNE", 11); opcodeTable.put("JCC", 12);
        opcodeTable.put("JMA", 13); opcodeTable.put("JSR", 14); opcodeTable.put("RFS", 15);
        opcodeTable.put("SOB", 16); opcodeTable.put("JGE", 17);
        opcodeTable.put("AMR", 4); opcodeTable.put("SMR", 5); opcodeTable.put("AIR", 6);
        opcodeTable.put("SIR", 7);
        opcodeTable.put("MLT", 20); opcodeTable.put("DVD", 21); opcodeTable.put("TRR", 22);
        opcodeTable.put("AND", 23); opcodeTable.put("ORR", 24); opcodeTable.put("NOT", 25);
        opcodeTable.put("SRC", 31); opcodeTable.put("RRC", 32);
        opcodeTable.put("IN", 61); opcodeTable.put("OUT", 62); opcodeTable.put("CHK", 63);
    }

    private static void performPass1(String fileName) throws IOException {
        // (Pass 1 from previous version, no changes here)
        File sourceFile = new File(fileName);
        Scanner scanner = new Scanner(sourceFile);
        int locationCounter = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty() || line.startsWith(";")) continue;
            if (line.contains(";")) line = line.substring(0, line.indexOf(';')).trim();
            Pattern labelPattern = Pattern.compile("^(\\w+):");
            Matcher labelMatcher = labelPattern.matcher(line);
            if (labelMatcher.find()) {
                String label = labelMatcher.group(1);
                symbolTable.put(label, locationCounter);
                line = line.substring(label.length() + 1).trim();
            }
            if (!line.isEmpty()) {
                String[] parts = line.split("\\s+", 2);
                String operation = parts[0].toUpperCase();
                if (operation.equals("LOC")) {
                    locationCounter = Integer.parseInt(parts[1]);
                } else {
                    locationCounter++;
                }
            }
        }
        scanner.close();
    }

    /**
     * Helper function to resolve an operand that can be either a number or a label.
     */
    private static int resolveValue(String operand) {
        operand = operand.trim();
        if (symbolTable.containsKey(operand)) {
            return symbolTable.get(operand);
        }
        return Integer.parseInt(operand);
    }


    private static void performPass2(String sourceFileName, String listingFileName, String loadFileName) throws IOException {
        File sourceFile = new File(sourceFileName);
        Scanner scanner = new Scanner(sourceFile);
        FileWriter listingWriter = new FileWriter(listingFileName);
        FileWriter loadWriter = new FileWriter(loadFileName);
        int locationCounter = 0;
        int lineNumber = 0;

        while (scanner.hasNextLine()) {
            lineNumber++;
            String originalLine = scanner.nextLine();
            String line = originalLine.trim();

            if (line.isEmpty() || line.startsWith(";")) {
                listingWriter.write(String.format("\t\t\t%s\n", originalLine));
                continue;
            }
            if (line.contains(";")) line = line.substring(0, line.indexOf(';')).trim();

            String labelPart = "";
            if (line.matches("^(\\w+):.*")) {
                labelPart = line.substring(0, line.indexOf(':') + 1);
                line = line.substring(line.indexOf(':') + 1).trim();
            }

            if (line.isEmpty()) {
                listingWriter.write(String.format("%06o\t\t%s\n", symbolTable.get(labelPart.replace(":", "")), originalLine));
                continue;
            }

            String[] parts = line.split("\\s+", 2);
            String operation = parts[0].toUpperCase();
            String operandsStr = (parts.length > 1) ? parts[1] : "";

            if (operation.equals("LOC")) {
                locationCounter = Integer.parseInt(operandsStr);
                listingWriter.write(String.format("\t\t\t%s\n", originalLine));
                continue;
            }

            Integer machineCode = null;

            if (operation.equals("DATA")) {
                // *** FIX IS HERE: Use the helper function ***
                machineCode = resolveValue(operandsStr);
            } else if (operation.equals("HLT")) {
                machineCode = 0;
            } else if (opcodeTable.containsKey(operation)) {
                int opcode = opcodeTable.get(operation);
                String[] operands = operandsStr.split(",");

                switch (operation) {
                    case "LDR": case "STR": case "LDA": case "AMR": case "SMR":
                    case "JZ": case "JNE": case "JCC": case "JMA": case "JSR":
                    case "SOB": case "JGE":
                        int r = Integer.parseInt(operands[0].trim());
                        int ix = Integer.parseInt(operands[1].trim());
                        // *** FIX IS HERE: Use helper to resolve the address ***
                        int address = resolveValue(operands[2]);
                        int i = (operands.length == 4 && "1".equals(operands[3].trim())) ? 1 : 0;
                        machineCode = (opcode << 10) | (r << 8) | (ix << 6) | (i << 5) | address;
                        break;

                    case "LDX": case "STX":
                        ix = Integer.parseInt(operands[0].trim());
                        // *** FIX IS HERE: Use helper to resolve the address ***
                        address = resolveValue(operands[1]);
                        i = (operands.length == 3 && "1".equals(operands[2].trim())) ? 1 : 0;
                        machineCode = (opcode << 10) | (ix << 6) | (i << 5) | address;
                        break;

                    case "AIR": case "SIR":
                        r = Integer.parseInt(operands[0].trim());
                        int immed = Integer.parseInt(operands[1].trim());
                        machineCode = (opcode << 10) | (r << 8) | immed;
                        break;

                    case "MLT": case "DVD": case "TRR": case "AND": case "ORR":
                        int rx = Integer.parseInt(operands[0].trim());
                        int ry = Integer.parseInt(operands[1].trim());
                        machineCode = (opcode << 10) | (rx << 8) | (ry << 6);
                        break;

                    case "NOT":
                        rx = Integer.parseInt(operands[0].trim());
                        machineCode = (opcode << 10) | (rx << 8);
                        break;

                    default:
                        throw new IllegalArgumentException("Unsupported instruction '" + operation + "' on line " + lineNumber);
                }
            } else {
                throw new IllegalArgumentException("Unknown operation '" + operation + "' on line " + lineNumber);
            }

            if (machineCode != null) {
                String octalAddress = String.format("%06o", locationCounter);
                String octalContent = String.format("%06o", machineCode);
                String binaryContent = String.format("%16s", Integer.toBinaryString(machineCode)).replace(' ', '0');

                listingWriter.write(String.format("%s\t%s\t%s\n", octalAddress, octalContent, originalLine));
                loadWriter.write(String.format("%s %s\n", octalAddress, octalContent));

                System.out.printf("Processed Address %s: %-30s -> %s (Binary: %s)\n", octalAddress, originalLine.trim(), octalContent, binaryContent);
                locationCounter++;
            }
        }
        scanner.close();
        listingWriter.close();
        loadWriter.close();
    }
}

