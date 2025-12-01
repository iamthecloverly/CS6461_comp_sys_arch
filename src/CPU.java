public class CPU {
    // --- Registers ---
    public int PC, IR, MAR, MBR, MFR, CC;
    private final int[] gpr = new int[4];
    private final int[] ixr = new int[4];

    public final int[] memory = new int[2048];
    public final Cache cache;
    public SimulatorGUI gui;

    public CPU() {
        this.cache = new Cache(this);
        reset();
    }

    public void setGUI(SimulatorGUI gui) { this.gui = gui; }

    public void reset() {
        PC = 0; IR = 0; MAR = 0; MBR = 0; MFR = 0; CC = 0;
        for (int i=0; i<4; i++) { gpr[i] = 0; ixr[i] = 0; }
        for (int i=0; i<memory.length; i++) memory[i] = 0;
        cache.reset();
    }

    // Accessors
    public int getGPR(int i) { return gpr[i]; }
    public void setGPR(int i, int v) { gpr[i] = v & 0xFFFF; }
    public int getIXR(int i) { return ixr[i]; }
    public void setIXR(int i, int v) { if (i>0 && i<4) ixr[i] = v & 0xFFFF; }

    // Memory / Cache / Helper methods
    public int readMemory(int addr) { return cache.read(addr); }
    public void writeMemory(int addr, int val) { cache.write(addr, val); }
    public int fetchFromMemory(int addr) {
        if (addr < 0 || addr >= memory.length) { triggerFault(1); return 0; } // Illegal Address Fault
        MAR = addr; MBR = memory[addr]; return MBR;
    }
    public void writeToMemory(int addr, int val) {
        if (addr < 0 || addr >= memory.length) { triggerFault(1); return; } // Illegal Address Fault
        MAR = addr; MBR = val & 0xFFFF; memory[addr] = MBR;
    }
    public int calculateEffectiveAddress(int addr, int ix, int i) {
        int ea = addr;
        if (ix > 0 && ix < 4) ea += ixr[ix];
        if (i == 1) ea = readMemory(ea);
        return ea;
    }
    private void setCC(int bit, boolean val) { if (val) CC |= (1<<bit); else CC &= ~(1<<bit); }
    private boolean getCC(int bit) { return (CC & (1<<bit)) != 0; }

    // --- FAULT & TRAP LOGIC ---
    private void triggerFault(int faultCode) {
        MFR = faultCode;
        // Trap to location 1 for faults (simple implementation)
        // Store PC to location 2 (optional, but good practice)
        memory[2] = PC;
        PC = 1;
    }

    public boolean executeInstruction() {
        if (PC < 0 || PC >= memory.length) { triggerFault(1); return false; }
        IR = readMemory(PC);
        PC++;

        int opcode = (IR >> 10) & 0b111111;
        int r = (IR >> 8) & 0b11;
        int ix = (IR >> 6) & 0b11;
        int i = (IR >> 5) & 0b1;
        int address = IR & 0b11111;

        int rx = r; int ry = ix;
        int al = (IR >> 7) & 0b1; int lr = (IR >> 6) & 0b1; int count = IR & 0b1111;
        int devid = address;

        int ea, r_val, ea_val;
        long result;

        switch (opcode) {
            case 0: return false; // HLT
            case 1: ea = calculateEffectiveAddress(address, ix, i); setGPR(r, readMemory(ea)); break; // LDR
            case 2: ea = calculateEffectiveAddress(address, ix, i); writeMemory(ea, getGPR(r)); break; // STR
            case 3: ea = calculateEffectiveAddress(address, ix, i); setGPR(r, ea); break; // LDA
            case 41: ea = calculateEffectiveAddress(address, 0, i); setIXR(ix, readMemory(ea)); break; // LDX
            case 42: ea = calculateEffectiveAddress(address, 0, i); writeMemory(ea, getIXR(ix)); break; // STX

            // Arithmetic
            case 4: // AMR
                r_val = getGPR(r); ea = calculateEffectiveAddress(address, ix, i); ea_val = readMemory(ea);
                result = (long)(short)r_val + (long)(short)ea_val;
                setGPR(r, (int)result); setCC(0, result > 32767 || result < -32768); break;
            case 5: // SMR
                r_val = getGPR(r); ea = calculateEffectiveAddress(address, ix, i); ea_val = readMemory(ea);
                result = (long)(short)r_val - (long)(short)ea_val;
                setGPR(r, (int)result); setCC(1, result < -32768); break;
            case 6: // AIR
                r_val = getGPR(r); ea_val = (address > 15) ? (address | 0xFFE0) : address;
                result = (long)(short)r_val + (long)ea_val;
                setGPR(r, (int)result); setCC(0, result > 32767 || result < -32768); break;
            case 7: // SIR
                r_val = getGPR(r); ea_val = (address > 15) ? (address | 0xFFE0) : address;
                result = (long)(short)r_val - (long)ea_val;
                setGPR(r, (int)result); setCC(1, result < -32768); break;

            // Logical
            case 20: // MLT
                if(rx!=0 && rx!=2) break;
                result = (long)(short)getGPR(rx) * (long)(short)getGPR(ry);
                setGPR(rx, (int)(result >> 16)); setGPR(rx+1, (int)(result & 0xFFFF));
                setCC(0, result > Integer.MAX_VALUE); break;
            case 21: // DVD
                if(rx!=0 && rx!=2) break;
                if(getGPR(ry) == 0) { setCC(2, true); break; }
                setCC(2, false);
                setGPR(rx, (short)getGPR(rx) / (short)getGPR(ry));
                setGPR(rx+1, (short)getGPR(rx) % (short)getGPR(ry)); break;
            case 22: setCC(3, getGPR(rx) == getGPR(ry)); break; // TRR
            case 23: setGPR(rx, getGPR(rx) & getGPR(ry)); break; // AND
            case 24: setGPR(rx, getGPR(rx) | getGPR(ry)); break; // ORR
            case 25: setGPR(rx, ~getGPR(rx)); break; // NOT

            // Shift
            case 31: // SRC
                r_val = getGPR(r);
                if (al==0) r_val = (lr==1) ? r_val << count : r_val >> count;
                else r_val = (lr==1) ? r_val << count : (r_val & 0xFFFF) >>> count;
                setGPR(r, r_val); break;
            case 32: // RRC
                r_val = getGPR(r);
                if (lr==1) r_val = (r_val << count) | ((r_val & 0xFFFF) >>> (16 - count));
                else r_val = ((r_val & 0xFFFF) >>> count) | (r_val << (16 - count));
                setGPR(r, r_val); break;

            // Jump
            case 10: if (getGPR(r) == 0) PC = calculateEffectiveAddress(address, ix, i); break; // JZ
            case 11: if (getGPR(r) != 0) PC = calculateEffectiveAddress(address, ix, i); break; // JNE
            case 12: if (getCC(r)) PC = calculateEffectiveAddress(address, ix, i); break; // JCC
            case 13: PC = calculateEffectiveAddress(address, ix, i); break; // JMA
            case 14: setGPR(3, PC); PC = calculateEffectiveAddress(address, ix, i); break; // JSR
            case 15: setGPR(0, address); PC = getGPR(3); break; // RFS
            case 16: r_val = (short)getGPR(r) - 1; setGPR(r, r_val); if (r_val > 0) PC = calculateEffectiveAddress(address, ix, i); break; // SOB
            case 17: if ((short)getGPR(r) >= 0) PC = calculateEffectiveAddress(address, ix, i); break; // JGE

            // --- PART 3 NEW INSTRUCTIONS ---
            case 61: // IN
                if (gui != null) {
                    // Device 2 is the File Reader for Program 2
                    int input = gui.readFromDevice(devid);
                    if (gui.isWaitingForInput()) { PC--; return true; } // Pause
                    setGPR(r, input);
                }
                break;
            case 62: // OUT
                if (gui != null) gui.writeToDevice(devid, getGPR(r));
                break;
            case 63: // CHK (Check Device Status)
                // 0: Keyboard, 1: Printer, 2: File Reader
                // Returns 1 (Ready) or 0 (Busy). For sim, mostly 1.
                int status = 1;
                // Could perform more complex checks here if needed
                setGPR(r, status);
                break;
            case 30: // TRAP
                // Trap code is in the address field (first 4 bits)
                int trapCode = address & 0b1111;
                // Save PC to memory[2], jump to memory[0] + trapCode
                // Simplified: just jump to trap vector 0
                memory[2] = PC;
                PC = 0; // Vector 0 is the trap handler
                break;

            default: triggerFault(4); return false; // Illegal Opcode
        }
        return true;
    }
}