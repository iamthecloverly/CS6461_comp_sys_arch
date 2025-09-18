import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assembler {

    // A map to store the symbol table (labels and their addresses)
    private static final Map<String, Integer> symbolTable = new HashMap<>();
    // A map to store opcodes and their binary representation
    private static final Map<String, String> opcodeTable = new HashMap<>();

    public static void main(String[] args) {
        // --- Argument Handling ---
        if (args.length < 1) {
            System.err.println("Usage: java -jar Assembler.jar <source_file.txt>");
            System.err.println("Example: java -jar Assembler.jar test_program.txt");
            return;
        }
        String sourceFileName = args[0];
        // Automatically determine output file names based on the source file name
        String listingFileName = sourceFileName.replace(".txt", "_listing.txt");
        String loadFileName = sourceFileName.replace(".txt", "_load.txt");

        // Populate the opcode table with values from the ISA document
        initializeOpcodeTable();

        try {
            // --- PASS 1: Build the Symbol Table ---
            System.out.println("--- Starting Pass 1: Building Symbol Table ---");
            performPass1(sourceFileName);
            System.out.println("Symbol Table constructed successfully:");
            symbolTable.forEach((label, address) ->
                    System.out.printf("  Label: %-10s Address: %d (0x%X)\n", label, address, address));
            System.out.println("--- Pass 1 Complete ---\n");


            // --- PASS 2: Generate Code and Output Files ---
            System.out.println("--- Starting Pass 2: Generating Machine Code ---");
            performPass2(sourceFileName, listingFileName, loadFileName);
            System.out.println("--- Pass 2 Complete ---");

            System.out.println("\nAssembly successful!");
            System.out.println("=> Listing File generated: " + listingFileName);
            System.out.println("=> Load File generated: " + loadFileName);

        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Initializes the opcode table with instruction mnemonics and their
     * corresponding 6-bit binary opcode.
     */
    private static void initializeOpcodeTable() {
        opcodeTable.put("HLT", "000000");
        opcodeTable.put("LDR", "000001");
        opcodeTable.put("STR", "000010");
        opcodeTable.put("LDA", "000011");
        opcodeTable.put("AMR", "000100");
        opcodeTable.put("SMR", "000101");
        opcodeTable.put("AIR", "000110");
        opcodeTable.put("SIR", "000111");
        opcodeTable.put("JZ", "001010");
        opcodeTable.put("JNE", "001011");
        opcodeTable.put("JCC", "001100");
        opcodeTable.put("JMA", "001101");
        opcodeTable.put("JSR", "001110");
        opcodeTable.put("RFS", "001111");
        opcodeTable.put("SOB", "010000");
        opcodeTable.put("JGE", "010001");
        opcodeTable.put("LDX", "100001");
        opcodeTable.put("STX", "100010");
        // Add other opcodes as needed for future parts
    }

    /**
     * Reads the source file to build a symbol table containing all labels and their memory addresses.
     *
     * @param fileName The path to the assembly source file.
     * @throws IOException If the file cannot be read.
     */
    private static void performPass1(String fileName) throws IOException {
        File sourceFile = new File(fileName);
        Scanner scanner = new Scanner(sourceFile);
        int locationCounter = 0;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            // Ignore comments and empty lines
            if (line.isEmpty() || line.startsWith(";")) {
                continue;
            }

            // Remove comments from the line
            if (line.contains(";")) {
                line = line.substring(0, line.indexOf(';')).trim();
            }

            // Check for a label (e.g., "End:")
            Pattern labelPattern = Pattern.compile("^(\\w+):");
            Matcher labelMatcher = labelPattern.matcher(line);
            if (labelMatcher.find()) {
                String label = labelMatcher.group(1);
                symbolTable.put(label, locationCounter);
                // Remove the label from the line to process the instruction
                line = line.substring(label.length() + 1).trim();
            }

            // Process directives and instructions
            if (!line.isEmpty()) {
                String[] parts = line.split("\\s+", 2);
                String operation = parts[0].toUpperCase();

                if (operation.equals("LOC")) {
                    locationCounter = Integer.parseInt(parts[1]);
                } else {
                    // For any other instruction or 'Data' directive, increment the location counter
                    locationCounter++;
                }
            }
        }
        scanner.close();
    }


    /**
     * Reads the source file a second time, translates instructions to machine code,
     * and writes the listing and load files.
     *
     * @param sourceFileName  The path to the assembly source file.
     * @param listingFileName The name for the output listing file.
     * @param loadFileName    The name for the output load file.
     * @throws IOException If files cannot be read or written.
     */
    private static void performPass2(String sourceFileName, String listingFileName, String loadFileName) throws IOException {
        File sourceFile = new File(sourceFileName);
        Scanner scanner = new Scanner(sourceFile);
        FileWriter listingWriter = new FileWriter(listingFileName);
        FileWriter loadWriter = new FileWriter(loadFileName);

        int locationCounter = 0;

        while (scanner.hasNextLine()) {
            String originalLine = scanner.nextLine();
            String line = originalLine.trim();

            // Handle comments and empty lines for the listing file
            if (line.isEmpty() || line.startsWith(";")) {
                listingWriter.write("\t\t\t" + originalLine + "\n");
                continue;
            }

            String comment = "";
            if (line.contains(";")) {
                comment = line.substring(line.indexOf(';'));
                line = line.substring(0, line.indexOf(';')).trim();
            }

            // Strip label from the line for processing, but keep it for the listing
            if (line.matches("^(\\w+):.*")) {
                line = line.substring(line.indexOf(':') + 1).trim();
            }

            // Process LOC directive
            String[] parts = line.split("\\s+", 2);
            String operation = parts[0].toUpperCase();

            if (operation.equals("LOC")) {
                locationCounter = Integer.parseInt(parts[1]);
                listingWriter.write("\t\t\t" + originalLine + "\n");
                continue; // LOC does not generate code
            }

            // --- Instruction and Data Processing ---
            String machineCodeBinary = ""; // This will hold the 16-bit binary string

            if (operation.equals("DATA")) {
                String valueStr = parts[1].trim();
                int value;
                if (symbolTable.containsKey(valueStr)) {
                    // The data is a label's address
                    value = symbolTable.get(valueStr);
                } else {
                    // The data is a number
                    value = Integer.parseInt(valueStr);
                }
                machineCodeBinary = String.format("%16s", Integer.toBinaryString(value)).replace(' ', '0');

            } else if (operation.equals("HLT")) {
                machineCodeBinary = String.format("%16s", "0").replace(' ', '0');
            } else if (opcodeTable.containsKey(operation)) {
                // It's a standard instruction (LDR, STR, etc.)
                String[] operands = parts[1].split(",");
                String opcode = opcodeTable.get(operation);

                // Default values
                String r = "00";
                String ix = "00";
                String i = "0";
                String address = "00000";

                // Handle different operand structures based on opcode
                if (operation.equals("LDX") || operation.equals("STX")) {
                    // Format: x, address[,I] -> operands[0], operands[1]
                    if (operands.length >= 2) {
                        ix = String.format("%2s", Integer.toBinaryString(Integer.parseInt(operands[0].trim()))).replace(' ', '0');
                        address = String.format("%5s", Integer.toBinaryString(Integer.parseInt(operands[1].trim()))).replace(' ', '0');
                    }
                    if (operands.length == 3 && "1".equals(operands[2].trim())) {
                        i = "1";
                    }
                } else {
                    // Default format: r, ix, address[,I] -> operands[0], operands[1], operands[2]
                    if (operands.length >= 3) {
                        r = String.format("%2s", Integer.toBinaryString(Integer.parseInt(operands[0].trim()))).replace(' ', '0');
                        ix = String.format("%2s", Integer.toBinaryString(Integer.parseInt(operands[1].trim()))).replace(' ', '0');
                        address = String.format("%5s", Integer.toBinaryString(Integer.parseInt(operands[2].trim()))).replace(' ', '0');
                    }
                    if (operands.length == 4 && "1".equals(operands[3].trim())) {
                        i = "1";
                    }
                }

                machineCodeBinary = opcode + r + ix + i + address;
            }


            // --- Format and Write Output ---
            if (!machineCodeBinary.isEmpty()) {
                // Ensure binary string is 16 bits
                if(machineCodeBinary.length() > 16) machineCodeBinary = machineCodeBinary.substring(0, 16);
                if(machineCodeBinary.length() < 16) machineCodeBinary = String.format("%16s", machineCodeBinary).replace(' ', '0');


                int decimalValue = Integer.parseInt(machineCodeBinary, 2);
                String octalAddress = String.format("%06o", locationCounter);
                String octalContent = String.format("%06o", decimalValue);

                // Write to listing file
                String listingLine = String.format("%s\t%s\t%s\n", octalAddress, octalContent, originalLine);
                listingWriter.write(listingLine);

                // Write to load file
                String loadLine = String.format("%s\t%s\n", octalAddress, octalContent);
                loadWriter.write(loadLine);

                System.out.printf("Processed Address %s: %s -> %s (Binary: %s)\n", octalAddress, originalLine.trim(), octalContent, machineCodeBinary);

                locationCounter++;
            } else {
                listingWriter.write("\t\t\t" + originalLine + "\n");
            }
        }

        scanner.close();
        listingWriter.close();
        loadWriter.close();
    }
}

