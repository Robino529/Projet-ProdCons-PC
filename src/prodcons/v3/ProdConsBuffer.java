package prodcons.v3;

import interfaces.IProdConsBuffer;
import interfaces.Message;
import java.util.concurrent.Semaphore;

/**
 * Implémentation de la Solution basée sur les Sémaphores (Objectif 3) à partir de :
 *
 * | Opération           | Pre-action (Ressource)             | Garde (Mutex)              | Post-action (Libération)                                             |
 * |---------------------|------------------------------------|----------------------------|----------------------------------------------------------------------|
 * | void put(Message m) | notFull.acquire()                  | mutex.acquire()            | putBuffer(m); mutex.release(); notEmpty.release();                   |
 * | Message get()       | notEmpty.acquire()                 | mutex.acquire()            | m = getBuffer(); mutex.release(); notFull.release(); return m;       |
 *
 */
public class ProdConsBuffer implements IProdConsBuffer {
    private Message[] buffer;
    private int in = 0, out = 0, count = 0, total = 0, size;
    
    // Ajout pour la terminaison (comme v2)
    private boolean stop = false; 
    
    // Sémaphores
    private Semaphore notFull;
    private Semaphore notEmpty; 
    private Semaphore mutex;

    public ProdConsBuffer(int bufSz) {
        this.size = bufSz;
        this.buffer = new Message[size];
        this.notFull = new Semaphore(size, true); 
        this.notEmpty = new Semaphore(0, true);
        this.mutex = new Semaphore(1, true);
    }

    @Override
    public void put(Message m) throws InterruptedException {

        notFull.acquire(); // Attente place libre
        mutex.acquire();   // Section critique
        try {
            buffer[in] = m;
            in = (in + 1) % size;
            count++;
            total++;
            System.out.println("Put " + m);
        } finally {
            mutex.release();
        }
        notEmpty.release(); // Signale un nouveau message
    }

    @Override
    public Message get() throws InterruptedException {
        notEmpty.acquire(); // Attente message ou signal d'arrêt
        
        mutex.acquire(); // Section critique
        Message m = null;
        try {
            // Si le flag est levé et qu'il n'y a plus de messages
            if (count == 0 && stop) {
                // On redonne le jeton pour que les autres consommateurs 
                // bloqués puissent aussi se réveiller et voir le stop
                notEmpty.release(); 
                return null; 
            }

            m = buffer[out];
            out = (out + 1) % size;
            count--;
            System.out.println("Get " + m);
        } finally {
            mutex.release();
        }
        notFull.release(); // Libère une place pour les producteurs
        return m;
    }


    public void shutdown() {
        try {
            mutex.acquire();
            stop = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release();
        }
        // On libère plein de jetons pour débloquer 
        // tous les consommateurs coincés dans notEmpty.acquire()
        notEmpty.release(1000); 
    }

    @Override
    public int nmsg() { 
        try {
            mutex.acquire();
            try { return count; } finally { mutex.release(); }
        } catch (InterruptedException e) { return 0; }
    }

    @Override
    public int totmsg() { 
        try {
            mutex.acquire();
            try { return total; } finally { mutex.release(); }
        } catch (InterruptedException e) { return 0; }
    }

    @Override
    public Message[] get(int k) throws InterruptedException { return null; }
    public void put(Message m, int n) throws InterruptedException {}
}