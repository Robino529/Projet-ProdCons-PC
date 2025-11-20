package prodcons.v1;

import tests.Consumer;
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
			int alreadyStartedProd = 0;
			int alreadyStartedCons = 0;
			for (int i = 0; i < options.nProd + options.nCons; i++) {
				if ((starter.nextBoolean() && options.nProd < alreadyStartedProd) || options.nCons == alreadyStartedCons) {
					producers[alreadyStartedProd].start();
					alreadyStartedProd++;
				} else {
					consumers[alreadyStartedCons].start();
					alreadyStartedCons++;
				}
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
