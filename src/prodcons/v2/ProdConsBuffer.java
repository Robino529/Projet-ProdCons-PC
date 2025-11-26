package prodcons.v2;

import interfaces.IProdConsBuffer;
import interfaces.Message;

/**
 * Implémentation de la Solution Directe à partir de :
 *
 * | Opération           | Pre-action   | Garde                             | Post-action                                                          |
 * |---------------------|--------------|-----------------------------------|----------------------------------------------------------------------|
 * | void put(Message m) |              | nbMsgInBuffer < buffer.length     | putBuffer(m); nbMsgInBuffer++; nbMsgDuringBufferLife++; notifyAll(); |
 * | Message get()       |              | nbMsgInBuffer > 0                 | return buffer[indice]; incrIndice(); nbMsgInBuffer--; notifyAll();   |
 *
 * Supplément shutdown et terminaison
 */

public class ProdConsBuffer implements IProdConsBuffer {
	private final static int SIZE_BUFFER = 5;

	private Message[] buffer;
	private int nbMsgInBuffer = 0;
	private int nbMsgDuringBufferLife = 0;
	private int indice = 0; // on utilise un Ring donc le premier indice n'est pas toujours 0
	private boolean shutdown = false;

	public ProdConsBuffer() {
		this.buffer = new Message[SIZE_BUFFER];
	}

	public ProdConsBuffer(int buffer_size) {
		this.buffer = new Message[buffer_size];
	}

	@Override
	public synchronized void put(Message m) throws InterruptedException {
		// test de l'état du buffer
		if (shutdown) {
			throw new InterruptedException("Buffer is shutting down.");
		}

		// boucle d'attente classique tirée du table des gardes donné plus haut
		while (nmsg() >= buffer.length) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}

		// insertion du message dans le buffer
		putBuffer(m);
		// incrémentation des différents indicateurs
		nbMsgInBuffer++;
		nbMsgDuringBufferLife++;
		// réveil de tout le monde (un seul pourrait suffire si on pouvait prévenir de façon isolée Cons et Prod)
		notifyAll();
		// affichage console pour pouvoir décomposer l'exécution
		System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tProduce : " + m);
	}

	@Override
	public synchronized Message get() throws InterruptedException {
		// test de l'état du buffer
		if (shutdown && nmsg() == 0) {
			throw new InterruptedException("Buffer is shutting down.");
		}

		// boucle d'attente classique tirée du table des gardes donné plus haut
		while (nmsg() == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}

		// récupération du message dans le buffer
		Message m = buffer[indice];
		// mise à jour des différents indicateurs
		incrIndice();
		nbMsgInBuffer--;
		// réveil de tout le monde (un seul pourrait suffire si on pouvait prévenir de façon isolée Cons et Prod)
		notifyAll();
		// affichage console pour pouvoir décomposer l'exécution
		System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tConsume : " + m);
		return m;
	}

	@Override
	public int nmsg() {
		return nbMsgInBuffer;
	}

	@Override
	public int totmsg() {
		return nbMsgDuringBufferLife;
	}

	/**
	 * À exécuter uniquement via des méthodes synchronized, permet d'incrémenter l'indice en respectant le Ring
	 */
	private void incrIndice() {
		indice = (indice+1) % buffer.length; // le modulo permet de respecter le ring
	}

	/**
	 * Permet d'insérer le message donné dans le buffer, en respectant les contraintes du Ring
	 * @param m Message à insérer dans le buffer
	 */
	private void putBuffer(Message m) {
		buffer[(indice+nbMsgInBuffer) % buffer.length] = m;
	}

	/**
	 * Éteint le buffer, il ne pourra plus recevoir de nouveaux messages
	 */
	public void shutdown() {
		shutdown = true;
	}

	/**
	 * @return État du buffer : True = buffer éteint
	 *                        | False = buffer ouvert
	 */
	public boolean isShutdown() {
		return shutdown;
	}
	public Message[] get(int k) throws InterruptedException{
		return null;
	}
	public void put(Message m, int n) throws InterruptedException{}
}
