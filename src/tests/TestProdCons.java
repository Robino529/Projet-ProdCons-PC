package tests;

import java.io.IOException;
import java.util.Properties;

/*
* Cette classe est un exemple, il faudra faire plusieurs classes de test
*/
public class TestProdCons {
	private int nProd;
	private int nCons;

	public static void main(String[] args) {
		try {
			new TestProdCons().retrieveOptions();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Méthode d'exemple de récupération des données d'un fichier options.xml
	 * @throws IOException => si problème dans lecture du fichier
	 * Ne renvoie rien car rempli directement les champs de l'objet TestProdCons
	 */
	private void retrieveOptions() throws IOException {
		Properties properties = new Properties();
		properties.loadFromXML(
				TestProdCons.class.getClassLoader().getResourceAsStream("tests/ex-options.xml"));
		nProd = Integer.parseInt(properties.getProperty("nProd"));
		nCons = Integer.parseInt(properties.getProperty("nCons"));
	}
}
