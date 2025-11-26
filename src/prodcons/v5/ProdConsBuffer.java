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

    // Sert de verrou pour dire "Occupé à faire un get(k), pas touche !"
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

        // Bloque si le buffer est plein
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
        notifyAll(); // On réveille les consommateurs
        System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tProduce : " + m);
    }

    @Override
    public synchronized Message get() throws InterruptedException {
        // On attend si vide OU si un transfert get(k) est en cours pour ne pas "voler" un message
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
        
        notifyAll(); // On libère de la place pour les producteurs
        System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tConsume : " + m);
        return m;
    }

    @Override
    public synchronized Message[] get(int k) throws InterruptedException {
        // Si quelqu'un fait déjà un gros transfert, on attend notre tour
        while (transferInProgress) {
            wait();
        }
        
        // Si c'est déjà fermé, pas la peine de commencer
        if (shutdown && nmsg() == 0) {
            return null; 
        }

        // On verrouille l'accès pour être les seuls à consommer
        transferInProgress = true;
        Message[] messages = new Message[k];

        try {
            for (int i = 0; i < k; i++) {
                // Attente qu'un message arrive
                while (nmsg() == 0) {
                    if (shutdown) {
                        // Si ça coupe au milieu, on renvoie ce qu'on a déjà pour ne pas perdre de données
                        return messages; 
                    }
                    try {
                        // Relâche le lock ici : permet aux producteurs de remplir le buffer pendant qu'on boucle
                        wait();
                    } catch (InterruptedException e) {
                         if (shutdown) return messages; 
                    }
                }

                messages[i] = buffer[indice];
                incrIndice();
                nbMsgInBuffer--;
                
                System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tMulti-Consume [" + (i+1) + "/" + k + "] : " + messages[i]);

                // Important : on notifie à chaque pas pour que les producteurs remettent des messages
                notifyAll();
            }
        } finally {
            // Quoi qu'il arrive (succès ou crash), on libère le passage pour les autres
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

    /**
     * Place un message à la suite des autres.
     * Logique : L'index d'écriture se trouve à "Tête de Lecture" + "Nombre d'éléments".
     * Le modulo (%) gère le retour au début du tableau si on dépasse la taille.
     */
    private void putBuffer(Message m) {
        buffer[(indice+nbMsgInBuffer) % buffer.length] = m;
    }

    public synchronized void shutdown() {
        shutdown = true;
        notifyAll(); // Réveil général pour traiter l'arrêt
    }

    public boolean isShutdown() {
        return shutdown;
    }
    
    // Stub pour l'interface (utilisé plus tard en obj 6)
    public void put(Message m, int n) throws InterruptedException {}
}