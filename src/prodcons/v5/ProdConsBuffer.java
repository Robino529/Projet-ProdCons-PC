package prodcons.v5;

import prodcons.v5.IProdConsBuffer;
import interfaces.Message;

public class ProdConsBuffer implements IProdConsBuffer {
    private Message[] buffer;
    private int in = 0, out = 0, count = 0, total = 0, size;
    
    private boolean transferInProgress = false; 

    public ProdConsBuffer(int bufSz) {
        this.size = bufSz;
        this.buffer = new Message[size];
    }

    @Override
    public synchronized void put(Message m) throws InterruptedException {
        while (count == size) {
            wait();
        }
        
        buffer[in] = m;
        in = (in + 1) % size;
        count++;
        total++;
        
        notifyAll(); 
    }

    @Override
    public synchronized Message get() throws InterruptedException {
        while (count == 0 || transferInProgress) {
            wait();
        }
        
        Message m = buffer[out];
        out = (out + 1) % size;
        count--;
        
        notifyAll();
        return m;
    }

    @Override
    public synchronized Message[] get(int k) throws InterruptedException {
        while (transferInProgress) { 
            wait();
        }

        // 2. Verrouillage de la consommation
        transferInProgress = true; 
        Message[] messages = new Message[k];

        try {

            for (int i = 0; i < k; i++) {
                while (count == 0) {
                    wait();
                }

                messages[i] = buffer[out];
                out = (out + 1) % size;
                count--;
                notifyAll();
            }
        } finally {
            transferInProgress = false;
            notifyAll();
        }

        return messages;
    }

    @Override
    public synchronized int nmsg() {
        return count;
    }

    @Override
    public synchronized int totmsg() {
        return total;
    }
}