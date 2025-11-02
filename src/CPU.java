import javax.swing.*;
import java.util.LinkedList;

public class CPU {
    // --- CPU Registers ---
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

    // --- Cache ---
    private static final int CACHE_SIZE = 16;
    private final CacheLine[] cache = new CacheLine[CACHE_SIZE];
    private final LinkedList<Integer> fifoQueue = new LinkedList<>();

    // --- I/O ---
    private final JTextArea consoleOutputArea; // For OUT instruction

    /**
     * CacheLine inner class
     */
    private static class CacheLine {
        int tag = -1; // Memory address
        int data = 0; // Data at that address
        boolean valid = false;
    }

    // --- Constructor ---
    public CPU(JTextArea consoleOutputArea) {
        this.consoleOutputArea = consoleOutputArea;
        for (int i = 0; i < CACHE_SIZE; i++) {
            cache[i] = new CacheLine();
        }
        reset();
    }

    /**
     * Resets all CPU registers, memory, and cache to their initial state.
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
        // Invalidate all cache lines
        for (int i = 0; i < CACHE_SIZE; i++) {
            cache[i].valid = false;
            cache[i].tag = -1;
        }
        fifoQueue.clear();
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

    // --- Condition Code ---
    /**
     * Sets the Condition Code (CC) based on the value.
     * CC(0) = OVERFLOW, CC(1) = UNDERFLOW, CC(2) = DIVZERO, CC(3) = EQUAL/NOT
     */
    private void setCC(int value) {
        CC = 0;
        if (value == 0) {
            CC = (CC | 0b1000); // Set EQUAL bit
        }
        // Note: OVERFLOW and UNDERFLOW would be set by arithmetic operations
        // We'll primarily use the EQUAL bit for JZ/JNE.
    }

    private int getCCBit(int bit) {
        return (CC >> (3 - bit)) & 1;
    }

    // --- Memory Operations (Now with Cache) ---

    /**
     * Reads a value from memory, checking the cache first.
     */
    public int readMemory(int address) {
        if (address < 0 || address >= memory.length) {
            MFR = 1; // Illegal Memory Address
            return 0;
        }
        MAR = address;

        // Check cache
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (cache[i].valid && cache[i].tag == address) {
                // Cache Hit
                MBR = cache[i].data;
                return MBR;
            }
        }

        // Cache Miss
        MBR = memory[address]; // Fetch from main memory
        addToCache(address, MBR);
        return MBR;
    }

    /**
     * Writes a value to memory (Write-Through policy).
     */
    public void writeMemory(int address, int value) {
        if (address < 0 || address >= memory.length) {
            MFR = 1; // Illegal Memory Address
            return;
        }
        MAR = address;
        MBR = value & 0xFFFF; // Ensure value is 16-bit
        memory[address] = MBR; // Write-Through to main memory

        // Update cache if the address is cached
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (cache[i].valid && cache[i].tag == address) {
                cache[i].data = MBR; // Update cache data
                return;
            }
        }

        // If not in cache, add it (Write-Allocate)
        addToCache(address, MBR);
    }

    /**
     * Adds a memory block to the cache using FIFO replacement.
     */
    private void addToCache(int address, int data) {
        // Find an invalid line first
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (!cache[i].valid) {
                cache[i].valid = true;
                cache[i].tag = address;
                cache[i].data = data;
                fifoQueue.add(i); // Add to end of FIFO queue
                return;
            }
        }

        // If cache is full, use FIFO
        int replaceIndex = fifoQueue.poll(); // Get and remove head of queue
        cache[replaceIndex].tag = address;
        cache[replaceIndex].data = data;
        fifoQueue.add(replaceIndex); // Add to end of queue
    }

    /**
     * Public accessor to get cache contents for the GUI.
     */
    public String getCacheContents() {
        StringBuilder sb = new StringBuilder();
        sb.append("Index | Valid | Tag (Oct) | Data (Oct)\n");
        sb.append("--------------------------------------\n");
        for (int i = 0; i < CACHE_SIZE; i++) {
            sb.append(String.format("  %02d  |   %d   |  %04o   |  %06o\n",
                    i,
                    cache[i].valid ? 1 : 0,
                    cache[i].tag,
                    cache[i].data));
        }
        return sb.toString();
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

    // Helper for 16-bit signed arithmetic
    private int signExtend(int value) {
        if ((value & 0x8000) != 0) {
            return value | 0xFFFF0000;
        }
        return value;
    }


    /**
     * Fetches, decodes, and executes a single instruction pointed to by the PC.
     *
     * @return true if execution should continue, false if HLT or a fault occurs.
     */
    public boolean executeInstruction() {
        // Fetch
        if (PC < 0 || PC >= memory.length) {
            MFR = 1; // Illegal Memory Address
            return false;
        }
        IR = readMemory(PC);
        PC++;

        // Decode
        int opcode = (IR >> 10) & 0b111111;
        int r = (IR >> 8) & 0b11;
        int ix = (IR >> 6) & 0b11;
        int i = (IR >> 5) & 0b1;
        int address = IR & 0b11111;

        // Register-to-Register Decode
        int rx = (IR >> 8) & 0b11;
        int ry = (IR >> 6) & 0b11;

        // Shift/Rotate Decode
        int al = (IR >> 7) & 0b1; // Arithmetic/Logical
        int lr = (IR >> 6) & 0b1; // Left/Right
        int count = IR & 0b1111;

        // I/O Decode
        int devid = IR & 0b11111;

        // Immediate Decode
        int immed = IR & 0b11111;
        if ((opcode == 6 || opcode == 7) && (immed & 0b10000) != 0) {
            immed = (immed - 32); // Sign-extend 5-bit immediate
        }


        // Execute
        int ea;
        int val, val2, result;

        switch (opcode) {
            case 0: // HLT
                return false; // Signal to stop execution

            case 1: // LDR
                ea = calculateEffectiveAddress(address, ix, i);
                setGPR(r, readMemory(ea));
                break;

            case 2: // STR
                ea = calculateEffectiveAddress(address, ix, i);
                writeMemory(ea, getGPR(r));
                break;

            case 3: // LDA
                ea = calculateEffectiveAddress(address, ix, i);
                setGPR(r, ea);
                break;

            case 4: // AMR (Add Memory to Register)
                ea = calculateEffectiveAddress(address, ix, i);
                val = signExtend(getGPR(r));
                val2 = signExtend(readMemory(ea));
                result = val + val2;
                setGPR(r, result);
                // TODO: Set OVERFLOW CC
                break;

            case 5: // SMR (Subtract Memory from Register)
                ea = calculateEffectiveAddress(address, ix, i);
                val = signExtend(getGPR(r));
                val2 = signExtend(readMemory(ea));
                result = val - val2;
                setGPR(r, result);
                // TODO: Set UNDERFLOW CC
                break;

            case 6: // AIR (Add Immediate to Register)
                val = signExtend(getGPR(r));
                result = val + immed;
                setGPR(r, result);
                // TODO: Set OVERFLOW CC
                break;

            case 7: // SIR (Subtract Immediate from Register)
                val = signExtend(getGPR(r));
                result = val - immed;
                setGPR(r, result);
                // TODO: Set UNDERFLOW CC
                break;

            case 10: // JZ (Jump if Zero)
                ea = calculateEffectiveAddress(address, ix, i);
                if (getGPR(r) == 0) {
                    PC = ea;
                }
                break;

            case 11: // JNE (Jump if Not Equal)
                ea = calculateEffectiveAddress(address, ix, i);
                if (getGPR(r) != 0) {
                    PC = ea;
                }
                break;

            case 12: // JCC (Jump if Condition Code)
                ea = calculateEffectiveAddress(address, ix, i);
                if (getCCBit(r) == 1) { // r = 0,1,2,3 -> CC bit 0,1,2,3
                    PC = ea;
                }
                break;

            case 13: // JMA (Unconditional Jump to Address)
                ea = calculateEffectiveAddress(address, ix, i);
                PC = ea;
                break;

            case 14: // JSR (Jump and Save Return)
                ea = calculateEffectiveAddress(address, ix, i);
                setGPR(3, PC); // Save PC in R3
                PC = ea;
                break;

            case 15: // RFS (Return from Subroutine)
                PC = getGPR(3); // Restore PC from R3
                break;

            case 16: // SOB (Subtract One and Branch)
                val = signExtend(getGPR(r)) - 1;
                setGPR(r, val);
                if (signExtend(getGPR(r)) > 0) {
                    ea = calculateEffectiveAddress(address, ix, i);
                    PC = ea;
                }
                break;

            case 17: // JGE (Jump if Greater Than or Equal)
                ea = calculateEffectiveAddress(address, ix, i);
                if (signExtend(getGPR(r)) >= 0) {
                    PC = ea;
                }
                break;

            case 20: // MLT (Multiply Register by Register)
                val = signExtend(getGPR(rx));
                val2 = signExtend(getGPR(ry));
                result = val * val2;
                // High bits in rx, low bits in rx+1
                setGPR(rx, (result >> 16) & 0xFFFF);
                setGPR(rx + 1, result & 0xFFFF);
                break;

            case 21: // DVD (Divide Register by Register)
                val = signExtend(getGPR(rx));
                val2 = signExtend(getGPR(ry));
                if (val2 == 0) {
                    CC = (CC | 0b0100); // Set DIVZERO bit
                    MFR = 2; // Illegal TRAP code
                    return false;
                }
                setGPR(rx, (val / val2) & 0xFFFF); // Quotient
                setGPR(rx + 1, (val % val2) & 0xFFFF); // Remainder
                break;

            case 22: // TRR (Test Registers)
                val = getGPR(rx);
                val2 = getGPR(ry);
                if (val == val2) {
                    setCC(0);
                } else {
                    setCC(1);
                }
                break;

            case 23: // AND (Logical AND)
                val = getGPR(rx);
                val2 = getGPR(ry);
                setGPR(rx, (val & val2) & 0xFFFF);
                break;

            case 24: // ORR (Logical OR)
                val = getGPR(rx);
                val2 = getGPR(ry);
                setGPR(rx, (val | val2) & 0xFFFF);
                break;

            case 25: // NOT (Logical NOT)
                val = getGPR(rx);
                setGPR(rx, (~val) & 0xFFFF);
                break;

            case 30: // TRAP
                MFR = 3; // TRAP instruction
                PC = readMemory(0); // Jump to address in memory[0]
                break;

            case 31: // SRC (Shift Register)
                val = getGPR(r);
                if (lr == 1) { // Left shift
                    result = val << count;
                } else { // Right shift
                    if (al == 1) { // Arithmetic
                        result = signExtend(val) >> count;
                    } else { // Logical
                        result = val >>> count;
                    }
                }
                setGPR(r, result & 0xFFFF);
                break;

            case 32: // RRC (Rotate Register)
                val = getGPR(r);
                if (lr == 1) { // Left rotate
                    result = (val << count) | (val >>> (16 - count));
                } else { // Right rotate
                    result = (val >>> count) | (val << (16 - count));
                }
                setGPR(r, result & 0xFFFF);
                break;

            case 41: // LDX (Opcode 51 Octal)
                ea = calculateEffectiveAddress(address, 0, i);
                setIXR(ix, readMemory(ea));
                break;

            case 42: // STX (Opcode 52 Octal)
                ea = calculateEffectiveAddress(address, 0, i);
                writeMemory(ea, getIXR(ix));
                break;

            case 61: // IN (Input)
                // We simplify this: DEVID 0 = Keyboard, reads a *signed integer*
                if (devid == 0) {
                    String input = JOptionPane.showInputDialog(null, "Enter an integer value for IN instruction:");
                    try {
                        result = Integer.parseInt(input);
                        setGPR(r, result);
                    } catch (NumberFormatException e) {
                        consoleOutputArea.append("Invalid input. R" + r + " not set.\n");
                    }
                }
                break;

            case 62: // OUT (Output)
                // DEVID 1 = Console Printer, prints a *signed integer*
                if (devid == 1) {
                    val = signExtend(getGPR(r));
                    consoleOutputArea.append(val + "\n");
                }
                break;

            default:
                MFR = 4; // Illegal Operation Code
                return false; // Halt on fault
        }

        return true; // Signal to continue execution
    }
}