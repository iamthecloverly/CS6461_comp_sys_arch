# CSCI 6461 Assembler

This project is a **two-pass assembler** for the custom **C6461 Instruction Set Architecture (ISA)**, developed for the CSCI 6461 Computer Architecture course. The assembler is written entirely in **Java**.

It takes a text file containing C6461 assembly language, processes labels, directives, and instructions, and outputs two files:
- A **human-readable listing file**
- A **machine-readable load file** intended for a C6461 simulator

---

## ✨ Features

### Two-Pass Assembly Process
- **Pass 1:** Scans the source code to build a symbol table, resolving all label addresses before translation.
- **Pass 2:** Translates assembly mnemonics and operands into **16-bit machine code**.

### Full ISA Support
- Handles all specified instruction formats:
    - Load/Store
    - Arithmetic
    - Transfer
    - Shift/Rotate operations

### Symbolic Addressing
- Supports labels and forward references in both **Data directives** and **jump instructions**.

### Correct Output Formatting
- Generates:
    - `[source]_listing.txt` → For debugging
    - `[source]_load.txt` → For the simulator (all values in **octal**)

### Robust Error Handling
- Provides clear error messages for unsupported instructions or incorrect syntax.

---

## 📦 Prerequisites

Before building or running the assembler, ensure you have the following installed:

- **Java Development Kit (JDK):** Version 8 or later
- **(Optional) Git:** For version control

---

## ⚙️ How to Build the JAR File

You can build the executable `.jar` file using either an IDE like IntelliJ or directly from the command line.

### Option 1: Using IntelliJ IDEA
1. **Open the Project:** Launch IntelliJ IDEA and open the project folder.
2. **Configure Artifact:**
    - Navigate to `File → Project Structure…` (`⌘;` or `Ctrl+Alt+Shift+S`).
    - Select `Artifacts → + → JAR → From modules with dependencies...`.
    - Set **Assembler** as the Main Class.
    - Click **OK**.
3. **Build Artifact:**
    - Go to `Build → Build Artifacts…`.
    - Select your artifact and choose **Build**.
    - The JAR will be generated in the `out/artifacts/` directory.

### Option 2: Using the Command Line
1. **Navigate to Source Directory:**
   ```bash
   cd path/to/your/project/src
