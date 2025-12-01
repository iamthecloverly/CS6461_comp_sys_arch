import java.util.LinkedList;
import java.util.Queue;

public class Cache {

    private static class CacheLine {
        int tag = -1; // Memory address
        int data = 0; // Data at that address
        boolean valid = false;
    }

    private final CacheLine[] cacheLines;
    private final Queue<Integer> fifoQueue;
    private final int CACHE_SIZE = 16;
    private final CPU cpu;

    public Cache(CPU cpu) {
        this.cpu = cpu;
        this.cacheLines = new CacheLine[CACHE_SIZE];
        for (int i = 0; i < CACHE_SIZE; i++) {
            this.cacheLines[i] = new CacheLine();
        }
        this.fifoQueue = new LinkedList<>();
    }

    public void reset() {
        for (int i = 0; i < CACHE_SIZE; i++) {
            cacheLines[i].valid = false;
            cacheLines[i].tag = -1;
            cacheLines[i].data = 0;
        }
        fifoQueue.clear();
    }

    public int read(int address) {
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (cacheLines[i].valid && cacheLines[i].tag == address) {
                return cacheLines[i].data;
            }
        }
        int data = cpu.fetchFromMemory(address);
        addToCache(address, data);
        return data;
    }

    public void write(int address, int value) {
        cpu.writeToMemory(address, value);
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (cacheLines[i].valid && cacheLines[i].tag == address) {
                cacheLines[i].data = value;
                return;
            }
        }
        addToCache(address, value);
    }

    private void addToCache(int address, int data) {
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (!cacheLines[i].valid) {
                cacheLines[i].valid = true;
                cacheLines[i].tag = address;
                cacheLines[i].data = data;
                fifoQueue.add(i);
                return;
            }
        }
        int indexToEvict = fifoQueue.poll();
        cacheLines[indexToEvict].tag = address;
        cacheLines[indexToEvict].data = data;
        fifoQueue.add(indexToEvict);
    }

    // This is the method required by SimulatorGUI
    public String getCacheStateForGUI() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CACHE_SIZE; i++) {
            if (cacheLines[i].valid) {
                sb.append(String.format("L%d: [M: %04o, V: %06o]\n", i, cacheLines[i].tag, cacheLines[i].data));
            } else {
                sb.append(String.format("L%d: [Invalid]\n", i));
            }
        }
        return sb.toString();
    }
}