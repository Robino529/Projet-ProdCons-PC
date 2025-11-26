package prodcons.v3;

import prodcons.v2.Consumer;
import tests.Producer;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class TestProdCons {
    private int nProd;
    private int nCons;
    private int minMsgToProduce;
    private int maxMsgToProduce;
    private int bufferSize;
    private int productionTime;
    private int consumptionTime;

    public static void main(String[] args) {
        try {
            TestProdCons options = new TestProdCons();
            options.retrieveOptions();

            ProdConsBuffer buffer = new ProdConsBuffer(options.bufferSize);

            Producer[] producers = new Producer[options.nProd];
            Consumer[] consumers = new Consumer[options.nCons];

            for (int i = 0; i < producers.length; i++) {
                producers[i] = new Producer(i, buffer, options.minMsgToProduce, options.maxMsgToProduce, options.productionTime);
            }
            for (int i = 0; i < consumers.length; i++) {
                consumers[i] = new Consumer(i, buffer, options.consumptionTime);
            }

            Random starter = new Random();
            int alreadyStartedProd = 0;
            int alreadyStartedCons = 0;

            for (int i = 0; i < options.nProd + options.nCons; i++) {
                boolean startProd = starter.nextBoolean();
                if ((startProd && alreadyStartedProd < options.nProd) || alreadyStartedCons == options.nCons) {
                    producers[alreadyStartedProd].start();
                    alreadyStartedProd++;
                } else {
                    consumers[alreadyStartedCons].start();
                    alreadyStartedCons++;
                }
            }

            for (Producer producer : producers) {
                try {
                    producer.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Arrêt du buffer une fois les producteurs terminés
            buffer.shutdown();

            for (Consumer consumer : consumers) {
                try {
                    consumer.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            System.out.println("Fin du test Objectif 3.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void retrieveOptions() throws IOException {
        Properties properties = new Properties();
        properties.loadFromXML(TestProdCons.class.getClassLoader().getResourceAsStream("tests/ex-options.xml"));

        nProd = Integer.parseInt(properties.getProperty("nProd"));
        nCons = Integer.parseInt(properties.getProperty("nCons"));
        minMsgToProduce = Integer.parseInt(properties.getProperty("minProd"));
        maxMsgToProduce = Integer.parseInt(properties.getProperty("maxProd"));
        bufferSize = Integer.parseInt(properties.getProperty("bufSz"));
        productionTime = Integer.parseInt(properties.getProperty("prodTime"));
        consumptionTime = Integer.parseInt(properties.getProperty("consTime"));
    }
}