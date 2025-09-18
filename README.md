# CS6461 - Part 0: C6461 Assembler

This project is a **two-pass assembler** for the custom **C6461 Instruction Set Architecture (ISA)**, developed for the **CSCI 6461 Computer Architecture** course at George Washington University.

The assembler is written entirely in **Java** and designed to be compiled and executed using standard development tools such as **IntelliJ IDEA**.

It takes in a text file containing C6461 assembly language, processes labels, directives, and instructions, and outputs two files:

- A **human-readable listing file**
- A **machine-readable load file** (for use in the future C6461 simulator project)

---

## ✨ Features
- Implements a **two-pass assembly process**:
    - **Pass 1:** Scans and resolves labels, builds the symbol table, and handles directives.
    - **Pass 2:** Translates mnemonics into machine code and outputs final binaries.
- Supports:
    - **Directives** (e.g., LOC, Data)
    - **Labels** (e.g., `LOOP:`)
    - **Instructions** (e.g., `LDR 1,0,10`)
    - **Comments** (lines starting with `;`)
- Produces both **debugging-friendly output** and **simulator-ready binaries**.
- Formats memory addresses and machine code in **octal**, consistent with the ISA specification.
- Modular structure for future extensibility.

---

## 📦 Prerequisites
Before building or running the assembler, ensure you have the following installed on your system:

- **Java Development Kit (JDK):** Version 8 or later
- **IntelliJ IDEA:** Community or Ultimate edition
- (Optional) **Git:** For version control

---

## ⚙️ How to Compile and Create the JAR File
This project uses IntelliJ IDEA’s **artifact packaging tool** for JAR generation.

1. **Open the Project**
    - Launch IntelliJ IDEA
    - Open the project folder (e.g., `CS6461_Assembler`)

2. **Configure the JAR Artifact**
    - Navigate to **File → Project Structure…** (shortcut: `⌘;` or `Ctrl+Alt+Shift+S`)
    - Select **Artifacts** from the left-hand menu
    - Click the **`+`** icon → **JAR → From modules with dependencies...**
    - In the dialog, set **Assembler** as the **Main Class**
    - Select **extract to the target JAR**
    - Click **OK** to save

3. **Build the JAR File**
    - Go to **Build → Build Artifacts…**
    - Select your artifact (e.g., `CS6461_Assembler:jar`)
    - Choose **Build**
    - IntelliJ will generate the JAR at:
      ```
      out/artifacts/YourProjectName_jar/YourProjectName.jar
      ```

---

## ▶️ How to Run the Assembler
The assembler must be run from a **command-line terminal**.

1. **Navigate to Artifacts Directory**
   ```bash
   cd ~/IdeaProjects/CS6461_Assembler/out/artifacts/CS6461_Assembler_jar/
