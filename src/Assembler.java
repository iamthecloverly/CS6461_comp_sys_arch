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
            System.err.println("Usage: java Assembler <source_file.txt>");
            return;
        }

        String sourceFileName = args[0];
        String listingFileName = sourceFileName.replace(".txt", "_listing.txt");
        String loadFileName = sourceFileName.replace(".txt", "_load.txt");

        initializeOpcodeTable();
        symbolTable.clear();

        try {
            System.out.println("--- Starting Pass 1: Building Symbol Table ---");
            performPass1(sourceFileName);
            System.out.println("Symbol Table constructed successfully.");
            System.out.println("--- Pass 1 Complete ---\n");

            System.out.println("--- Starting Pass 2: Generating Machine Code ---");
            performPass2(sourceFileName, listingFileName, loadFileName);
            System.out.println("--- Pass 2 Complete ---");

            System.out.println("\nAssembly successful!");
            System.out.println("=> Listing File: " + listingFileName);
            System.out.println("=> Load File: " + loadFileName);

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // Opcode table
    // ------------------------------------------------------------
    private static void initializeOpcodeTable() {
        opcodeTable.put("HLT", 0);
        opcodeTable.put("TRAP", 30);

        opcodeTable.put("LDR", 1);
        opcodeTable.put("STR", 2);
        opcodeTable.put("LDA", 3);

        opcodeTable.put("LDX", 41);
        opcodeTable.put("STX", 42);

        opcodeTable.put("JZ", 10);
        opcodeTable.put("JNE", 11);
        opcodeTable.put("JCC", 12);
        opcodeTable.put("JMA", 13);
        opcodeTable.put("JSR", 14);
        opcodeTable.put("RFS", 15);
        opcodeTable.put("SOB", 16);
        opcodeTable.put("JGE", 17);

        opcodeTable.put("AMR", 4);
        opcodeTable.put("SMR", 5);
        opcodeTable.put("AIR", 6);
        opcodeTable.put("SIR", 7);

        opcodeTable.put("MLT", 20);
        opcodeTable.put("DVD", 21);
        opcodeTable.put("TRR", 22);
        opcodeTable.put("AND", 23);
        opcodeTable.put("ORR", 24);
        opcodeTable.put("NOT", 25);

        opcodeTable.put("SRC", 31);
        opcodeTable.put("RRC", 32);

        opcodeTable.put("IN", 61);
        opcodeTable.put("OUT", 62);
        opcodeTable.put("CHK", 63);

        // Alias
        opcodeTable.put("BEQ", 10);
    }

    // ------------------------------------------------------------
    // Pass 1: build symbol table
    // ------------------------------------------------------------
    private static void performPass1(String fileName) throws IOException {
        File sourceFile = new File(fileName);
        Scanner scanner = new Scanner(sourceFile);

        int locationCounter = 0;

        while (scanner.hasNextLine()) {
            String raw = scanner.nextLine();
            String line = stripComments(raw).trim();
            if (line.isEmpty()) continue;

            // label?
            Pattern labelPattern = Pattern.compile("^(\\w+):");
            Matcher labelMatcher = labelPattern.matcher(line);
            if (labelMatcher.find()) {
                String label = labelMatcher.group(1);
                if (symbolTable.containsKey(label)) {
                    throw new IllegalArgumentException("Duplicate label: " + label);
                }
                symbolTable.put(label, locationCounter);
                line = line.substring(label.length() + 1).trim();
            }

            if (!line.isEmpty()) {
                String[] parts = line.split("\\s+", 2);
                String op = parts[0].toUpperCase();
                if (op.equals("LOC")) {
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("LOC requires an address");
                    }
                    locationCounter = parseIntClean(parts[1].trim());
                } else {
                    locationCounter++;
                }
            }
        }
        scanner.close();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private static String stripComments(String line) {
        int idx = line.indexOf(';');
        if (idx >= 0) return line.substring(0, idx);
        return line;
    }

    private static boolean isInteger(String s) {
        if (s == null) return false;
        s = s.trim();
        if (s.isEmpty()) return false;
        return s.matches("-?\\d+");
    }

    // Clean numeric string and parse as decimal
    private static int parseIntClean(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Null numeric operand");
        }
        String cleaned = s.trim();
        cleaned = cleaned.replaceAll("[^\\x00-\\x7F]", "");
        cleaned = cleaned.replaceAll("[^0-9\\-]", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Empty numeric operand after cleaning: '" + s + "'");
        }
        return Integer.parseInt(cleaned);
    }

    // Resolve label or numeric literal
    private static int resolveValue(String operand) {
        if (operand == null) {
            throw new IllegalArgumentException("Null operand");
        }
        String cleaned = operand.trim();
        cleaned = cleaned.replaceAll("[^\\x00-\\x7F]", "").trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Empty operand after cleaning: '" + operand + "'");
        }

        if (symbolTable.containsKey(cleaned)) {
            return symbolTable.get(cleaned);
        }
        return parseIntClean(cleaned);
    }

    // Split comma-separated operands, trimming and dropping empties
    private static String[] splitOperands(String operandsStr) {
        if (operandsStr == null || operandsStr.trim().isEmpty()) {
            return new String[0];
        }
        String[] raw = operandsStr.split(",");
        return java.util.Arrays.stream(raw)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    // ------------------------------------------------------------
    // Pass 2: generate machine code
    // ------------------------------------------------------------
    private static void performPass2(String sourceFileName,
                                     String listingFileName,
                                     String loadFileName) throws IOException {

        File sourceFile = new File(sourceFileName);
        Scanner scanner = new Scanner(sourceFile);

        FileWriter listingWriter = new FileWriter(listingFileName);
        FileWriter loadWriter = new FileWriter(loadFileName);

        int locationCounter = 0;
        int lineNumber = 0;

        while (scanner.hasNextLine()) {
            lineNumber++;
            String originalLine = scanner.nextLine();
            String line = stripComments(originalLine).trim();

            if (line.isEmpty()) {
                listingWriter.write(String.format("\t\t\t%s\n", originalLine));
                continue;
            }

            // label?
            String labelPart = "";
            Pattern labelPattern = Pattern.compile("^(\\w+):");
            Matcher labelMatcher = labelPattern.matcher(line);
            if (labelMatcher.find()) {
                String label = labelMatcher.group(1);
                labelPart = label + ":";
                line = line.substring(labelPart.length()).trim();
            }

            if (line.isEmpty()) {
                if (!labelPart.isEmpty()) {
                    Integer addr = symbolTable.get(labelPart.replace(":", ""));
                    listingWriter.write(String.format("%06o\t\t%s\n",
                            addr == null ? 0 : addr, originalLine));
                } else {
                    listingWriter.write(String.format("\t\t\t%s\n", originalLine));
                }
                continue;
            }

            String[] parts = line.split("\\s+", 2);
            String operation = parts[0].toUpperCase();
            String operandsStr = (parts.length > 1) ? parts[1] : "";

            // LOC directive
            if (operation.equals("LOC")) {
                locationCounter = parseIntClean(operandsStr.trim());
                listingWriter.write(String.format("\t\t\t%s\n", originalLine));
                continue;
            }

            int machineCode;

            if (operation.equals("DATA")) {
                machineCode = resolveValue(operandsStr);
            } else if (operation.equals("HLT")) {
                machineCode = 0;
            } else if (opcodeTable.containsKey(operation)) {
                int opcode = opcodeTable.get(operation);
                String[] ops = splitOperands(operandsStr);

                int r = 0, ix = 0, address = 0, i = 0;
                int rx, ry;
                int immediate, devid;
                int count, lr, al;
                int trapCode;

                switch (operation) {

                    // R,IX,ADDR[,I]  or R,ADDR or ADDR (for jumps & load/store)
                    case "LDR": case "STR": case "LDA":
                    case "AMR": case "SMR":
                    case "JZ": case "JNE": case "JCC":
                    case "JMA": case "JSR": case "SOB":
                    case "JGE": case "BEQ":

                        if (ops.length == 0) {
                            throw new IllegalArgumentException("Missing operands for " + operation +
                                    " at line " + lineNumber);
                        } else if (ops.length == 1) {
                            // single operand → address
                            r = 0;
                            ix = 0;
                            address = resolveValue(ops[0]);
                            i = 0;
                        } else if (ops.length == 2) {
                            // R,ADDR
                            r = parseIntClean(ops[0]);
                            ix = 0;
                            address = resolveValue(ops[1]);
                            i = 0;
                        } else {
                            // R,IX,ADDR[,I]
                            r = parseIntClean(ops[0]);
                            ix = parseIntClean(ops[1]);
                            address = resolveValue(ops[2]);
                            if (ops.length >= 4) {
                                i = parseIntClean(ops[3]) != 0 ? 1 : 0;
                            } else {
                                i = 0;
                            }
                        }
                        machineCode = (opcode << 10) | (r << 8) | (ix << 6) | (i << 5) | address;
                        break;

                    // LDX/STX: IX,ADDR[,I]
                    case "LDX":
                    case "STX":
                        if (ops.length < 2) {
                            throw new IllegalArgumentException(operation + " requires at least 2 operands");
                        }
                        ix = parseIntClean(ops[0]);
                        address = resolveValue(ops[1]);
                        i = (ops.length >= 3 && parseIntClean(ops[2]) != 0) ? 1 : 0;
                        machineCode = (opcode << 10) | (ix << 6) | (i << 5) | address;
                        break;

                    // AIR/SIR: R,IMMED
                    case "AIR":
                    case "SIR":
                        if (ops.length < 2) {
                            throw new IllegalArgumentException(operation + " requires 2 operands");
                        }
                        r = parseIntClean(ops[0]);
                        immediate = parseIntClean(ops[1]);
                        machineCode = (opcode << 10) | (r << 8) | (immediate & 0x1F);
                        break;

                    // MLT/DVD/TRR/AND/ORR: Rx,Ry
                    case "MLT":
                    case "DVD":
                    case "TRR":
                    case "AND":
                    case "ORR":
                        if (ops.length < 2) {
                            throw new IllegalArgumentException(operation + " requires 2 operands");
                        }
                        rx = parseIntClean(ops[0]);
                        ry = parseIntClean(ops[1]);
                        machineCode = (opcode << 10) | (rx << 8) | (ry << 6);
                        break;

                    // NOT Rx
                    case "NOT":
                        if (ops.length < 1) {
                            throw new IllegalArgumentException("NOT requires 1 operand");
                        }
                        rx = parseIntClean(ops[0]);
                        machineCode = (opcode << 10) | (rx << 8);
                        break;

                    // SRC/RRC: R,Count,LR,AL
                    case "SRC":
                    case "RRC":
                        if (ops.length < 4) {
                            throw new IllegalArgumentException(operation + " requires 4 operands");
                        }
                        r = parseIntClean(ops[0]);
                        count = parseIntClean(ops[1]);
                        lr = parseIntClean(ops[2]);
                        al = parseIntClean(ops[3]);
                        machineCode = (opcode << 10) | (r << 8)
                                | ((al & 1) << 7) | ((lr & 1) << 6) | (count & 0xF);
                        break;

                    // IN/OUT/CHK: R,DevID
                    case "IN":
                    case "OUT":
                    case "CHK":
                        if (ops.length < 2) {
                            throw new IllegalArgumentException(operation + " requires 2 operands");
                        }
                        r = parseIntClean(ops[0]);
                        devid = parseIntClean(ops[1]);
                        machineCode = (opcode << 10) | (r << 8) | (devid & 0x1F);
                        break;

                    // TRAP: TrapCode
                    case "TRAP":
                        if (ops.length < 1) {
                            throw new IllegalArgumentException("TRAP requires trap code");
                        }
                        trapCode = parseIntClean(ops[0]);
                        machineCode = (opcode << 10) | (trapCode & 0xF);
                        break;

                    default:
                        throw new IllegalArgumentException("Unsupported instruction '" + operation + "'");
                }

            } else {
                throw new IllegalArgumentException("Unknown operation '" + operation + "'");
            }

            String octalAddress = String.format("%06o", locationCounter);
            String octalContent = String.format("%06o", machineCode & 0xFFFF);
            listingWriter.write(String.format("%s\t%s\t%s\n", octalAddress, octalContent, originalLine));
            loadWriter.write(String.format("%s %s\n", octalAddress, octalContent));

            locationCounter++;
        }

        scanner.close();
        listingWriter.close();
        loadWriter.close();
    }
}
