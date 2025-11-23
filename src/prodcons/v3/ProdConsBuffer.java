package prodcons.v3;

import interfaces.IProdConsBuffer;
import interfaces.Message;
import java.util.concurrent.Semaphore;

public class ProdConsBuffer implements IProdConsBuffer {
    private Message[] buffer;
    private int in = 0, out = 0, count = 0, total = 0, size;
    
    // Sémaphores
    private Semaphore notFull;
    private Semaphore notEmpty; 
    private Semaphore mutex;    // Exclusion mutuelle pour toucher aux index

    public ProdConsBuffer(int bufSz) {
        this.size = bufSz;
        this.buffer = new Message[size];
        this.notFull = new Semaphore(size, true); // FIFO fairness true
        this.notEmpty = new Semaphore(0, true);
        this.mutex = new Semaphore(1, true);
    }

    @Override
    public void put(Message m) throws InterruptedException {
        notFull.acquire();
        mutex.acquire();
        try {
            buffer[in] = m;
            in = (in + 1) % size;
            count++;
            total++;
            System.out.println("Put " + m);
        } finally {
            mutex.release();
        }
        notEmpty.release();
    }

    @Override
    public Message get() throws InterruptedException {
        notEmpty.acquire(); // Décrémente messages dispos (bloque si 0)
        mutex.acquire();
        Message m = null;
        try {
            m = buffer[out];
            out = (out + 1) % size;
            count--;
            System.out.println("Get " + m);
        } finally {
            mutex.release();
        }
        notFull.release(); // Incrémente places libres
        return m;
    }

    @Override
    public int nmsg() { return count; }
    @Override
    public int totmsg() { return total; }
    @Override
    public Message[] get(int k) throws InterruptedException{
		return null;
	}
 
}