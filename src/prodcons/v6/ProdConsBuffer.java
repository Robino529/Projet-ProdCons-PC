package prodcons.v6;

import interfaces.IProdConsBuffer;
import interfaces.Message;

public class ProdConsBuffer implements IProdConsBuffer {
    private final static int SIZE_BUFFER = 5;

    // Classe interne pour l'Obj 6.
    // Au lieu de stocker juste le Message, on stocke cet objet qui contient
    // le message ET le nombre d'exemplaires restants à distribuer.
    private static class RendezVous {
        final Message m;
        final int n;       // Combien d'exemplaires le producteur veut distribuer
        int consumed = 0;  // Combien on en a déjà distribué

        RendezVous(Message m, int n) {
            this.m = m;
            this.n = n;
        }

        // indique si tout le monde a eu son exemplaire
        boolean isCompleted() {
            return consumed >= n;
        }
    }

    private RendezVous[] buffer;
    private int nbMsgInBuffer = 0;
    private int indice = 0; // Tête de lecture pour le buffer circulaire
    private int nbMsgDuringBufferLife = 0;
    private boolean shutdown = false;

    public ProdConsBuffer() {
        this.buffer = new RendezVous[SIZE_BUFFER];
    }

    public ProdConsBuffer(int buffer_size) {
        this.buffer = new RendezVous[buffer_size];
    }

    // Le put synchrone demandé pour l'objectif 6
    @Override
    public synchronized void put(Message m, int n) throws InterruptedException {
        // Classique : si plus de place dans le tableau, on attend
        while (nbMsgInBuffer >= buffer.length) {
             if (shutdown) throw new InterruptedException("Shutdown");
             wait();
        }
        
        // On crée le RDV et on le met dans le buffer
        RendezVous rdv = new RendezVous(m, n);
        putBuffer(rdv);
        nbMsgInBuffer++;
        nbMsgDuringBufferLife++;
        
        notifyAll(); // On prévient les consommateurs qu'il y a du boulot
        System.out.println("Thread Prod " + Thread.currentThread().getId() + " : Déposé " + m + " (x" + n + "). En attente des consommateurs...");

        // Barrière coté Producteur :
        // On ne peux pas sortir de la méthode tant que le RDV n'est pas fini (consumed == n)
        while (!rdv.isCompleted()) {
            if (shutdown) throw new InterruptedException("Shutdown during synchronous put");
            wait(); // On lâche le verrou pour laisser les consommateurs prendre les messages
        }
        
        System.out.println("Thread Prod " + Thread.currentThread().getId() + " : Libéré (tous exemplaires consommés).");
    }

    // Le get synchrone (barrière consommateur)
    @Override
    public synchronized Message get() throws InterruptedException {
        // On attend qu'il y ait un RDV dispo
        while (nbMsgInBuffer == 0) {
            if (shutdown) throw new InterruptedException("Shutdown");
            wait();
        }
        
        // Attention : Je récupère l'objet mais je ne l'enlève pas du tableau tout de suite !
        // Les autres consommateurs doivent pouvoir accéder au MEME objet pour la synchro.
        RendezVous currentRdv = buffer[indice];
        
        // Je prends mon exemplaire
        currentRdv.consumed++;
        Message m = currentRdv.m;
        
        System.out.println("Thread Cons " + Thread.currentThread().getId() + " : A pris exemplaire " + currentRdv.consumed + "/" + currentRdv.n);
        
        // Logique de la barrière consommateur
        if (!currentRdv.isCompleted()) {
            // Cas 1 : Je ne suis pas le dernier.
            notifyAll(); // Je réveille les autres pour qu'ils viennent compléter le quota
            
            // Je suis bloqué ici tant que les autres ne sont pas arrivés
            while (!currentRdv.isCompleted()) {
                if (shutdown) {
                    // Correction du bug de fermeture :
                    // Si le buffer ferme alors que j'ai déjà mon message, je le garde
                    // Sinon ça fait une perte de données dans les tests.
                    return m; 
                }
                try {
                    wait();
                } catch (InterruptedException e) {
                    if (shutdown) return m; // Idem ici
                }
            }
        } else {
            // Cas 2 : Je suis le dernier !
            System.out.println("thread Cons " + Thread.currentThread().getId() + " : dernier consommateur");
            
            // on passe au message suivant dans le buffer
            incrIndice();
            nbMsgInBuffer--;
            
            // ce notifyAll libère tout le monde :
            // - Le Producteur qui attendait la fin
            // - Les autres Consommateurs bloqués dans le while au-dessus
            notifyAll();
        }

        return m;
    }

    // Juste pour respecter l'interface, on redirige vers put(m, 1)
    @Override
    public synchronized void put(Message m) throws InterruptedException {
        put(m, 1);
    }

    @Override
    public synchronized int nmsg() { return nbMsgInBuffer; }

    @Override
    public synchronized int totmsg() { return nbMsgDuringBufferLife; }
    
    // Gestion de l'arrêt propre
    public synchronized void shutdown() {
        shutdown = true;
        notifyAll();
    }
    
    // Gestion index circulaire
    private void incrIndice() {
        indice = (indice + 1) % buffer.length;
    }

    private void putBuffer(RendezVous rdv) {
        buffer[(indice + nbMsgInBuffer) % buffer.length] = rdv;
    }
    
    @Override
    public Message[] get(int k) throws InterruptedException {
        throw new UnsupportedOperationException("Pas demandé pour l'obj 6");
    }
}