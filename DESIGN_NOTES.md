# CSCI 6461 Assembler - Design Notes
1. Overall Architecture
   The assembler follows a classic two-pass design. This approach is chosen to correctly handle forward references, where a label is used as an operand before it is defined in the source code.

Pass 1: The primary goal is to build a Symbol Table. This pass reads the source file line-by-line to find all labels and record their corresponding memory addresses. It does not generate any machine code. A location counter is maintained to track addresses.

Pass 2: This pass performs the actual translation. It reads the source file again, and for each line, it generates the corresponding 16-bit machine code. It uses the Symbol Table created in Pass 1 to resolve label addresses. It then formats this machine code into octal and writes to the required output files.

2. Key Data Structures
   Symbol Table: A java.util.HashMap<String, Integer> is used to store the symbol table.

Key: The String label name (e.g., "LOOP", "END").

Value: The Integer memory address (location counter value) corresponding to that label.

Reasoning: A HashMap provides efficient O(1) average time complexity for lookups, which is ideal for resolving label addresses during Pass 2.

Opcode Table: A java.util.HashMap<String, String> is used to map instruction mnemonics to their binary opcodes.

Key: The String instruction mnemonic (e.g., "LDR", "STR").

Value: The 6-bit binary String representation of the opcode (e.g., "000001").

Reasoning: This provides a clean and quick way to look up the binary value for each instruction during translation.

3. File Handling
   Input: The assembler takes a single command-line argument: the path to the assembly source file (.txt).

Output: It generates two files automatically based on the input file's name:

[source]_listing.txt: The human-readable listing file.

[source]_load.txt: The machine-readable load file for the simulator.

Implementation: Standard Java I/O classes (java.io.File, java.util.Scanner, java.io.FileWriter) are used for all file operations.

4. Code Parsing and Translation
   Each line is processed by first removing comments (anything after a ;).

Labels are identified by checking for a trailing colon (:).

Instructions and their operands are parsed using String.split() with whitespace and commas as delimiters.

Binary strings for each part of an instruction (opcode, registers, address, etc.) are constructed and then concatenated to form the final 16-bit machine code.

Java's String.format() and Integer.toBinaryString() / Integer.toOctalString() are used heavily for padding and number base conversions to ensure correct formatting.