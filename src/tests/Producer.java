package tests;

import interfaces.IProdConsBuffer;

import java.util.Random;

public class Producer extends Thread {
	private int nMsgToProd;
	private IProdConsBuffer buffer;
	private final int prodTime;

	public Producer(int id, IProdConsBuffer buffer, int minMsg, int maxMsg, int prodTime) {
		super("Producer-" + id);
		this.nMsgToProd = new Random().nextInt(minMsg, maxMsg+1);
		this.buffer = buffer;
		this.prodTime = prodTime;
	}

	// Implémentation d'une classe Message utilisable dans notre cas
	private class Message implements interfaces.Message {
		String msg;

		public Message(String msg) {
			this.msg = msg + "\n\tProduced by " + Producer.this.getName();
		}

		@Override
		public String toString() {
			return msg;
		}
	}

	@Override
	public void run() {
		// indice du message à créer (permet de les retrouver)
		int count = 1;
		// temps au lancement qui va permettre de dormir uniquement le temps nécessaire pour assurer une production en prodTime millisecondes
		long startTime = System.currentTimeMillis();

		// boucle d'exécution qui va produire exactement nMsgToProd messages
		while (nMsgToProd > 0) {
			// on insère un message dans le buffer
			try {
				buffer.put(new Message("Message n°"+count));
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

            // affiche l'indice du message produit et le nom du producteur
//			System.out.println("Message n°" + count + " produced by " + Producer.this.getName());

			// on incrémente l'indice pour le prochain message
			count++;
			// on décrémente le nombre de messages restants à produire
			nMsgToProd--;
			// on attend le temps qu'il faut pour atteindre prodTime millisecondes de temps de production
			try {
				long timeToWait = prodTime-startTime;
				if (timeToWait > 0) {
					sleep(timeToWait);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
