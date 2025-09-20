# CSCI 6461 Assembler - Design Notes

This document outlines the design and architecture of the two-pass assembler for the CSCI 6461 computer architecture.

---

## 1. Overall Architecture

The assembler employs a **classic two-pass design**. This approach is essential for handling forward references, where a symbolic label (e.g., `LOOP_START`) is used as an operand before it is defined in the source code.

### Pass 1: Symbol Table Construction
- Primary goal: Build a complete symbol table.
- Process:
    - Read the source file line-by-line.
    - Identify all labels (e.g., `End:`).
    - Record their corresponding memory addresses.
- Implementation details:
    - Maintain a location counter to track the address of each instruction and data directive.
    - **No machine code** is generated during this pass.

### Pass 2: Code Generation
- Primary goal: Perform the actual translation.
- Process:
    - Read the source file again.
    - Generate the final 16-bit machine code for each line.
    - Use the symbol table created in Pass 1 to resolve all symbolic addresses.
- Output:
    - Final machine code is formatted into **octal**.
    - Written to the required output files.

---

## 2. Key Data Structures

### Symbol Table
- **Type:** `java.util.HashMap<String, Integer>`
- **Key:** Label name (e.g., `"LOOP"`, `"END"`)
- **Value:** Memory address corresponding to the label
- **Reasoning:** Provides efficient **O(1)** average lookup time, crucial for resolving label addresses quickly during Pass 2.

### Opcode Table
- **Type:** `java.util.HashMap<String, Integer>`
- **Key:** Instruction mnemonic (e.g., `"LDR"`, `"STR"`)
- **Value:** Integer representation of the opcode (e.g., `1`, `2`)
- **Reasoning:** Storing opcodes as integers allows clean, centralized mapping and easy construction of machine code using **bitwise operations**.

---

## 3. File Handling

- **Input:**
    - Command-line utility accepting a single argument: the path to an assembly source file (e.g., `program1.txt`).

- **Output:**  
  Automatically generates two files based on the input filename:
    - `[source]_listing.txt`: Human-readable listing file showing the address, generated machine code, and original source line.
    - `[source]_load.txt`: Machine-readable load file containing `[address] [value]` pairs in octal format for the simulator.

- **Implementation:**  
  Uses standard Java I/O classes (`java.io.File`, `java.util.Scanner`, `java.io.FileWriter`) for file operations.

---

## 4. Code Parsing and Translation

### Line Parsing
- Strip comments (any text following `;`).
- Identify labels (trailing `:`) in **Pass 1**.
- Parse instructions and operands using `String.split()` with whitespace and commas as delimiters.

### Machine Code Generation
- Construct the final 16-bit machine code word using **bitwise operations**.
- Shift (`<<`) opcode, registers, and other fields into their correct positions.
- Combine using bitwise OR (`|`).
- This method is efficient and standard for assembling machine instructions.

**Example:**

Instruction:
```text
LDR 1,0,20   ; Opcode=1, R=1, IX=0, I=0, Addr=20

machineCode = (opcode << 10) | (r << 8) | (ix << 6) | (i << 5) | address;

