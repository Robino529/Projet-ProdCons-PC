package prodcons.v6;

import interfaces.IProdConsBuffer;
import interfaces.Message;

public class ProdConsBuffer implements IProdConsBuffer {
    private final static int SIZE_BUFFER = 5;

    private static class RendezVous {
        final Message m;
        final int n;       // Nombre total d'exemplaires à distribuer
        int consumed = 0;  // Nombre d'exemplaires déjà pris

        RendezVous(Message m, int n) {
            this.m = m;
            this.n = n;
        }

        boolean isCompleted() {
            return consumed >= n;
        }
    }

    // Le buffer stocke des RendezVous, pas juste des messages
    private RendezVous[] buffer;
    private int nbMsgInBuffer = 0; // Nombre de RendezVous actifs (pas le nombre d'exemplaires)
    private int indice = 0;
    private int nbMsgDuringBufferLife = 0;
    private boolean shutdown = false;

    public ProdConsBuffer() {
        this.buffer = new RendezVous[SIZE_BUFFER];
    }

    public ProdConsBuffer(int buffer_size) {
        this.buffer = new RendezVous[buffer_size];
    }

    @Override
    public synchronized void put(Message m, int n) throws InterruptedException {

        while (nbMsgInBuffer >= buffer.length) {
             if (shutdown) throw new InterruptedException("Shutdown");
             wait();
        }
        RendezVous rdv = new RendezVous(m, n);
        putBuffer(rdv);
        nbMsgInBuffer++;
        nbMsgDuringBufferLife++;
        
        notifyAll();
        System.out.println("Thread Prod " + Thread.currentThread().getId() + " : Déposé " + m + " (x" + n + "). En attente des consommateurs...");

        while (!rdv.isCompleted()) {
            if (shutdown) throw new InterruptedException("Shutdown during synchronous put");
            wait();
        }
        
        System.out.println("Thread Prod " + Thread.currentThread().getId() + " : Libéré (tous exemplaires consommés).");
    }

    @Override
    public synchronized Message get() throws InterruptedException {
        while (nbMsgInBuffer == 0) {
            if (shutdown) throw new InterruptedException("Shutdown");
            wait();
        }
        RendezVous currentRdv = buffer[indice];
        currentRdv.consumed++;
        Message m = currentRdv.m;
        
        System.out.println("Thread Cons " + Thread.currentThread().getId() + " : A pris exemplaire " + currentRdv.consumed + "/" + currentRdv.n);
        if (!currentRdv.isCompleted()) {
            notifyAll();
            while (!currentRdv.isCompleted()) {
                if (shutdown) throw new InterruptedException("shutdown waiting at barrier");
                wait();
            }
        } else {
            // Cas où c'est le dernier consommateur
            System.out.println("thread Cons " + Thread.currentThread().getId() + " : dernier consommateur");
            incrIndice();
            nbMsgInBuffer--;
            
            // notifyAll pour va libérer le producteur bloqué dans put()
            // et les autres Consommateurs bloqués dans le while() ci-dessus
            notifyAll();
        }

        return m;
    }

    @Override
    public synchronized void put(Message m) throws InterruptedException {
        put(m, 1);
    }

    @Override
    public synchronized int nmsg() { return nbMsgInBuffer; }

    @Override
    public synchronized int totmsg() { return nbMsgDuringBufferLife; }
    
    public synchronized void shutdown() {
        shutdown = true;
        notifyAll();
    }

    
    private void incrIndice() {
        indice = (indice + 1) % buffer.length;
    }

    private void putBuffer(RendezVous rdv) {
        buffer[(indice + nbMsgInBuffer) % buffer.length] = rdv;
    }
    
    @Override
    public Message[] get(int k) throws InterruptedException {
        throw new UnsupportedOperationException("Obj 6 non compatible avec Obj 5");
    }
}