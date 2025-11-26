package prodcons.v6;

import interfaces.Message;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TestProdCons {
    // J'utilise des AtomicInteger pour les compteurs globaux car plusieurs threads 
    // écrivent dedans en même temps (sinon risque de race condition et résultats faux).
    public static AtomicInteger totalCopiesProduced = new AtomicInteger(0);
    public static AtomicInteger totalConsumed = new AtomicInteger(0);

    private int nProd;
    private int nCons;
    private int bufSz;
    private int prodTime;
    private int consTime;
    private int minCopies = 2;
    private int maxCopies = 5;

    public static void main(String[] args) {
        try {
            new TestProdCons().runTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runTest() throws IOException, InterruptedException {
        retrieveOptions();
        
        System.out.println("--- Démarrage du Test V6 (Rendez-Vous Synchrone) ---");
        
        ProdConsBuffer buffer = new ProdConsBuffer(bufSz);
        MultiProducer[] producers = new MultiProducer[nProd];
        SynchroConsumer[] consumers = new SynchroConsumer[nCons];

        // Init des threads
        for (int i = 0; i < nProd; i++) {
            producers[i] = new MultiProducer(i, buffer, prodTime, minCopies, maxCopies);
        }
        for (int i = 0; i < nCons; i++) {
            consumers[i] = new SynchroConsumer(i, buffer, consTime);
        }

        // Start
        for (MultiProducer p : producers) p.start();
        for (SynchroConsumer c : consumers) c.start();

        // On attend que les producteurs aient fini d'envoyer
        for (MultiProducer p : producers) {
            p.join();
        }
        System.out.println("\n>>> Producteurs terminés.");

        // On coupe le sifflet au buffer pour libérer les consommateurs restants
        System.out.println(">>> Envoi du SHUTDOWN...");
        buffer.shutdown();

        // On attend que tout le monde rentre
        for (SynchroConsumer c : consumers) {
            c.join();
        }
        System.out.println(">>> Consommateurs terminés.");

        // Vérif finale
        int produced = totalCopiesProduced.get();
        int consumed = totalConsumed.get();
        int remainingRendezVous = buffer.nmsg(); 

        System.out.println("\n--- BILAN OBJECTIF 6 ---");
        System.out.println("Total Produit : " + produced);
        System.out.println("Total Consommé : " + consumed);

        if (produced == consumed && remainingRendezVous == 0) {
            System.out.println("\n SUCCES : Tout correspond !");
        } else {
            System.out.println("\n ECHEC : Il y a une différence...");
            System.out.println("Différence : " + (produced - consumed));
        }
    }

    private void retrieveOptions() throws IOException {
        Properties properties = new Properties();
        properties.loadFromXML(TestProdCons.class.getClassLoader().getResourceAsStream("tests/ex-options.xml"));
        nProd = Integer.parseInt(properties.getProperty("nProd"));        
        nCons = Math.max(Integer.parseInt(properties.getProperty("nCons")), nProd * maxCopies + 5);
        bufSz = Integer.parseInt(properties.getProperty("bufSz"));
        prodTime = Integer.parseInt(properties.getProperty("prodTime"));
        consTime = Integer.parseInt(properties.getProperty("consTime"));
    }

    // Producteur spécial pour l'objectif qui choisit un nombre de copies au hasard
    static class MultiProducer extends Thread {
        private final ProdConsBuffer buffer;
        private final int prodTime;
        private final int minCopies, maxCopies;
        private final Random rand = new Random();
        private int nbMessagesToSend = 5; 

        public MultiProducer(int id, ProdConsBuffer buffer, int prodTime, int min, int max) {
            super("MultiProd-" + id);
            this.buffer = buffer;
            this.prodTime = prodTime;
            this.minCopies = min;
            this.maxCopies = max;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < nbMessagesToSend; i++) {
                    int n = rand.nextInt(maxCopies - minCopies + 1) + minCopies;
                    Message m = new StringMessage(getName() + "-M" + i);

                    try {
                        buffer.put(m, n);
                        // On incrémente seulement si le put a réussi (donc que le RDV est fini)
                        TestProdCons.totalCopiesProduced.addAndGet(n);
                    } catch (InterruptedException e) {
                        System.out.println(getName() + " interrompu.");
                        break; 
                    }
                    Thread.sleep(prodTime);
                }
            } catch (InterruptedException e) {}
        }
    }

    // Consommateur simple qui compte ce qu'il reçoit
    static class SynchroConsumer extends Thread {
        private final ProdConsBuffer buffer;
        private final int consTime;

        public SynchroConsumer(int id, ProdConsBuffer buffer, int consTime) {
            super("SyncCons-" + id);
            this.buffer = buffer;
            this.consTime = consTime;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Message m = buffer.get();                    
                    if (m == null) break; // Fin
                    
                    TestProdCons.totalConsumed.incrementAndGet();
                    Thread.sleep(consTime);
                }
            } catch (InterruptedException e) {}
        }
    }

    static class StringMessage implements Message {
        private final String content;
        public StringMessage(String s) { this.content = s; }
        @Override
        public String toString() { return content; }
    }
}