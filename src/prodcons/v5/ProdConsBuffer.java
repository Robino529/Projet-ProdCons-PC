package prodcons.v5;

import interfaces.IProdConsBuffer;
import interfaces.Message;

public class ProdConsBuffer implements IProdConsBuffer {
    private final static int SIZE_BUFFER = 5;

    private Message[] buffer;
    private int nbMsgInBuffer = 0;
    private int nbMsgDuringBufferLife = 0;
    private int indice = 0; 
    private boolean shutdown = false;

    private boolean transferInProgress = false; 

    public ProdConsBuffer() {
        this.buffer = new Message[SIZE_BUFFER];
    }

    public ProdConsBuffer(int buffer_size) {
        this.buffer = new Message[buffer_size];
    }

    @Override
    public synchronized void put(Message m) throws InterruptedException {
        if (shutdown) {
            throw new InterruptedException("Buffer is shutting down.");
        }

        while (nmsg() >= buffer.length) {
            try {
                wait();
            } catch (InterruptedException e) {
                if (shutdown) throw new InterruptedException("Buffer is shutting down.");
            }
        }

        putBuffer(m);
        nbMsgInBuffer++;
        nbMsgDuringBufferLife++;
        notifyAll();
        System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tProduce : " + m);
    }

    @Override
    public synchronized Message get() throws InterruptedException {
        while ((nmsg() == 0 && !shutdown) || transferInProgress) {
            try {
                wait();
            } catch (InterruptedException e) {
                if (shutdown) break; 
            }
        }

        if (shutdown && nmsg() == 0) {
            throw new InterruptedException("Buffer is shutting down.");
        }

        Message m = buffer[indice];
        incrIndice();
        nbMsgInBuffer--;
        
        notifyAll();
        System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tConsume : " + m);
        return m;
    }

@Override
    public synchronized Message[] get(int k) throws InterruptedException {
        // 1. Attente initiale
        while (transferInProgress) {
            wait();
        }
        
        // Si fermé au départ, on arrête tout de suite
        if (shutdown && nmsg() == 0) {
            return null; // Ou throw exception, mais null est plus simple à gérer
        }

        transferInProgress = true;
        Message[] messages = new Message[k];

        try {
            for (int i = 0; i < k; i++) {
                // Attente de message
                while (nmsg() == 0) {
                    if (shutdown) {
                        // MODIFICATION ICI :
                        // Si on s'arrête au milieu du paquet, on arrête la boucle
                        // mais on ne lance PAS d'exception pour ne pas perdre 
                        // les messages déjà stockés dans 'messages'
                        return messages; 
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {
                         if (shutdown) return messages; // Idem ici
                    }
                }

                messages[i] = buffer[indice];
                incrIndice();
                nbMsgInBuffer--;
                
                System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tMulti-Consume [" + (i+1) + "/" + k + "] : " + messages[i]);

                notifyAll();
            }
        } finally {
            transferInProgress = false;
            notifyAll();
        }

        return messages;
    }

    @Override
    public int nmsg() {
        return nbMsgInBuffer;
    }

    @Override
    public int totmsg() {
        return nbMsgDuringBufferLife;
    }

    private void incrIndice() {
        indice = (indice+1) % buffer.length;
    }

    private void putBuffer(Message m) {
        buffer[(indice+nbMsgInBuffer) % buffer.length] = m;
    }

    public synchronized void shutdown() {
        shutdown = true;
        notifyAll(); // Important : réveiller tout le monde pour qu'ils voient le flag shutdown
    }

    public boolean isShutdown() {
        return shutdown;
    }
}