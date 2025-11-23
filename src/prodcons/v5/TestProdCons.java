package prodcons.v5;

import tests.Producer;
import interfaces.Message;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TestProdCons {
    // Compteurs atomiques pour vérifier mathématiquement la validité
    public static AtomicInteger totalProduced = new AtomicInteger(0);
    public static AtomicInteger totalConsumed = new AtomicInteger(0);

    private int nProd;
    private int nCons;
    private int bufSz;
    private int prodTime;
    private int consTime;
    private int minProd;
    private int maxProd;

    public static void main(String[] args) {
        try {
            new TestProdCons().runTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runTest() throws IOException, InterruptedException {
        retrieveOptions();
        
        // On instancie votre buffer v5
        ProdConsBuffer buffer = new ProdConsBuffer(bufSz);

        Producer[] producers = new Producer[nProd];
        BatchConsumer[] consumers = new BatchConsumer[nCons];

        for (int i = 0; i < nProd; i++) {
            producers[i] = new Producer(i, buffer, minProd, maxProd, prodTime);
        }
        Random rand = new Random();
        for (int i = 0; i < nCons; i++) {
            int k = rand.nextInt(bufSz + 3) + 2; 
            consumers[i] = new BatchConsumer(i, buffer, consTime, k);
        }

        System.out.println("--- Démarrage du Test V5 ---");
        System.out.println("Buffer Taille: " + bufSz);
        System.out.println("Consommateurs demanderont des paquets de taille variable (jusqu'à " + (bufSz+4) + ")");

        for (Producer p : producers) p.start();
        for (BatchConsumer c : consumers) c.start();

        for (Producer p : producers) {
            p.join();
        }
        System.out.println(">>> Tous les producteurs ont terminé.");

        buffer.shutdown();
        System.out.println(">>> Signal Shutdown envoyé.");

        for (BatchConsumer c : consumers) {
            c.join();
        }
        System.out.println(">>> Tous les consommateurs ont terminé.");

        System.out.println("\n--- Bilan ---");
        System.out.println("Messages restants dans le buffer : " + buffer.nmsg());
        System.out.println("Messages produits (selon buffer.totmsg) : " + buffer.totmsg());
        System.out.println("Messages consommés (compteur test)      : " + totalConsumed.get());

        if (buffer.nmsg() == 0 && buffer.totmsg() == totalConsumed.get()) {
            System.out.println("succes : Buffer vide et comptes cohérents.");
        } else {
            System.out.println("echec : Incohérence détectée.");
        }
    }

    private void retrieveOptions() throws IOException {
        Properties properties = new Properties();
        properties.loadFromXML(TestProdCons.class.getClassLoader().getResourceAsStream("tests/ex-options.xml"));
        nProd = Integer.parseInt(properties.getProperty("nProd"));
        nCons = Integer.parseInt(properties.getProperty("nCons")); 
        bufSz = Integer.parseInt(properties.getProperty("bufSz"));
        prodTime = Integer.parseInt(properties.getProperty("prodTime"));
        consTime = Integer.parseInt(properties.getProperty("consTime"));
        minProd = Integer.parseInt(properties.getProperty("minProd"));
        maxProd = Integer.parseInt(properties.getProperty("maxProd"));
    }


    static class BatchConsumer extends Thread {
        private final ProdConsBuffer buffer;
        private final int id;
        private final int waitTime;
        private final int batchSize;

        public BatchConsumer(int id, ProdConsBuffer buffer, int waitTime, int k) {
            super("BatchCons-" + id); // Appel au constructeur de Thread
            this.id = id;
            this.buffer = buffer;
            this.waitTime = waitTime;
            this.batchSize = k;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Message[] messages = buffer.get(batchSize);
                    
                    if (messages == null) break;

                    int nbRecus = 0;
                    for (Message m : messages) {
                        if (m != null) nbRecus++;
                    }

                    if (nbRecus == 0) break;

                    synchronized (TestProdCons.class) {
                        System.out.println("Consommateur " + id + " a récupéré un lot de " + nbRecus + " messages (demandé: " + batchSize + ").");
                        TestProdCons.totalConsumed.addAndGet(nbRecus);
                    }
                    
                    if (nbRecus < batchSize) break; 
                    
                    Thread.sleep(waitTime);
                }
            } catch (InterruptedException e) {
                System.out.println("Consommateur " + id + " arrêté (Interrupted).");
            }
        }
    }
}