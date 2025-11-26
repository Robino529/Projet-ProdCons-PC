package prodcons.v4;

import prodcons.v2.Consumer;
import tests.Producer;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

/*
 * Cette classe est un exemple, il faudra faire plusieurs classes de test
 */
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

			// création des threads
			for (int i = 0; i < producers.length; i++) {
				producers[i] = new Producer(i, buffer, options.minMsgToProduce, options.maxMsgToProduce, options.productionTime);
			}
			for (int i = 0; i < consumers.length; i++) {
				consumers[i] = new Consumer(i, buffer, options.consumptionTime);
			}

			// lancement des threads
			Random starter = new Random();
			Thread[] order = new Thread[options.nProd+options.nCons]; // sert à afficher l'ordre de lancement

			int alreadyStartedProd = 0;
			int alreadyStartedCons = 0;
			for (int i = 0; i < options.nProd + options.nCons; i++) {
				// clause permettant de choisir aléatoirement entre lancer un Producteur ou un Consommateur, tant que c'est possible
				if ((starter.nextBoolean() && options.nProd > alreadyStartedProd) || options.nCons == alreadyStartedCons) {
					producers[alreadyStartedProd].start();
					order[i] = producers[alreadyStartedProd];
					alreadyStartedProd++;
				} else {
					consumers[alreadyStartedCons].start();
					order[i] = consumers[alreadyStartedCons];
					alreadyStartedCons++;
				}
			}

			// attente de la terminaison des producteurs
			for (Producer producer : producers) {
				try {
					producer.join();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			// Extinction du buffer
			// => Permet de s'assurer que personne d'autre ne va produire des messages (en réalité inutile dans le cas
			//    de cet objectif car un seul programme discute avec le buffer).
			// => Cela permet également aux consumers de s'arrêter puisqu'ils n'avaient pas de condition d'arrêt au préalable.
			buffer.shutdown();

			// attente de la terminaison des consommateurs
			for (Consumer consumer : consumers) {
				try {
					consumer.join();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			// affichage de l'ordre de démarrage des threads
			System.out.println("######################################\n\t\tOrdre de démarrage des threads");
			for (Thread thread : order) {
				System.out.println("\t"+thread.getName());
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Méthode d'exemple de récupération des données d'un fichier options.xml
	 *
	 * @throws IOException => si problème dans lecture du fichier
	 *                     Ne renvoie rien car rempli directement les champs de l'objet TestProdCons
	 */
	private void retrieveOptions() throws IOException {
		Properties properties = new Properties();
		properties.loadFromXML(
				TestProdCons.class.getClassLoader().getResourceAsStream("tests/ex-options.xml"));

		nProd = Integer.parseInt(properties.getProperty("nProd"));
		nCons = Integer.parseInt(properties.getProperty("nCons"));
		minMsgToProduce = Integer.parseInt(properties.getProperty("minProd"));
		maxMsgToProduce = Integer.parseInt(properties.getProperty("maxProd"));
		bufferSize = Integer.parseInt(properties.getProperty("bufSz"));
		productionTime = Integer.parseInt(properties.getProperty("prodTime"));
		consumptionTime = Integer.parseInt(properties.getProperty("consTime"));
	}
}
