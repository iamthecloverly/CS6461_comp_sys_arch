public class CPU {
    // --- CPU Registers ---
    // Public for easy access from the GUI, but could be made private with
    // getters/setters
    public int PC; // Program Counter, 12 bits
    public int IR; // Instruction Register, 16 bits
    public int MAR; // Memory Address Register, 12 bits
    public int MBR; // Memory Buffer Register, 16 bits
    public int MFR; // Machine Fault Register, 4 bits
    public int CC; // Condition Code, 4 bits

    private final int[] gpr = new int[4]; // General Purpose Registers R0-R3, 16 bits
    private final int[] ixr = new int[4]; // Index Registers X1-X3 (index 0 is unused), 16 bits

    // --- Main Memory ---
    public final int[] memory = new int[2048]; // 2048 words of 16-bit memory

    // --- Constructor ---
    public CPU() {
        reset();
    }

    /**
     * Resets all CPU registers and memory to their initial state (zero).
     */
    public void reset() {
        PC = 0;
        IR = 0;
        MAR = 0;
        MBR = 0;
        MFR = 0;
        CC = 0;
        for (int i = 0; i < 4; i++) {
            gpr[i] = 0;
            ixr[i] = 0;
        }
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    // --- Register Accessors ---
    public int getGPR(int index) {
        return gpr[index];
    }

    public void setGPR(int index, int value) {
        gpr[index] = value & 0xFFFF; // Ensure value is 16-bit
    }

    public int getIXR(int index) {
        return ixr[index];
    }

    public void setIXR(int index, int value) {
        if (index > 0 && index < 4) {
            ixr[index] = value & 0xFFFF; // Ensure value is 16-bit
        }
    }

    // --- Memory Operations ---
    public int readMemory(int address) {
        if (address < 0 || address >= memory.length) {
            MFR = 1; // Illegal Memory Address to Reserved Locations/Out of Bounds
            return 0;
        }
        MAR = address;
        MBR = memory[address];
        return MBR;
    }

    public void writeMemory(int address, int value) {
        if (address < 0 || address >= memory.length) {
            MFR = 1; // Illegal Memory Address
            return;
        }
        MAR = address;
        MBR = value & 0xFFFF; // Ensure value is 16-bit
        memory[address] = MBR;
    }

    /**
     * Calculates the Effective Address (EA) based on the instruction fields.
     */
    public int calculateEffectiveAddress(int addressField, int ixrIndex, int iFlag) {
        int effectiveAddress = addressField;

        if (ixrIndex > 0 && ixrIndex < 4) {
            effectiveAddress += ixr[ixrIndex];
        }

        if (iFlag == 1) { // Indirect addressing
            // The calculated address is a pointer to the final address
            effectiveAddress = readMemory(effectiveAddress);
        }
        return effectiveAddress;
    }

    /**
     * Fetches, decodes, and executes a single instruction pointed to by the PC.
     * * @return true if execution should continue, false if HLT or a fault occurs.
     */
    public boolean executeInstruction() {
        // Fetch
        IR = readMemory(PC);
        PC++;

        // Decode
        int opcode = (IR >> 10) & 0b111111;
        int r = (IR >> 8) & 0b11;
        int ix = (IR >> 6) & 0b11;
        int i = (IR >> 5) & 0b1;
        int address = IR & 0b11111;

        // Execute
        switch (opcode) {
            case 0: // HLT
                return false; // Signal to stop execution

            case 1: // LDR
                int ea_ldr = calculateEffectiveAddress(address, ix, i);
                setGPR(r, readMemory(ea_ldr));
                break;

            case 2: // STR
                int ea_str = calculateEffectiveAddress(address, ix, i);
                writeMemory(ea_str, getGPR(r));
                break;

            case 3: // LDA
                int ea_lda = calculateEffectiveAddress(address, ix, i);
                setGPR(r, ea_lda);
                break;

            case 41: // LDX (Opcode 51 Octal)
                // For LDX, the EA calculation does not use an index register.
                // The 'ix' field specifies WHICH index register to load INTO.
                int ea_ldx = calculateEffectiveAddress(address, 0, i);
                setIXR(ix, readMemory(ea_ldx));
                break;

            default:
                MFR = 4; // Illegal Operation Code
                return false; // Halt on fault
        }

        return true; // Signal to continue execution
    }
}
