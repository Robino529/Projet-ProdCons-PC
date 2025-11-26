package prodcons.v4;

import interfaces.IProdConsBuffer;
import interfaces.Message;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implémentation utilisant des Locks et des Conditions de Java,
 * basée sur l'implémentation de la Solution Directe à partir de :
 *
 * | Opération           | Pre-action   | Garde                             | Post-action                                                          |
 * |---------------------|--------------|-----------------------------------|----------------------------------------------------------------------|
 * | void put(Message m) |              | nbMsgInBuffer < buffer.length     | putBuffer(m); nbMsgInBuffer++; nbMsgDuringBufferLife++; notifyAll(); |
 * | Message get()       |              | nbMsgInBuffer > 0                 | return buffer[indice]; incrIndice(); nbMsgInBuffer--; notifyAll();   |
 *
 * L'avantage de cette version est que l'on sépare le verrou des Consommateurs du verrou des Producteurs
 */

public class ProdConsBuffer implements IProdConsBuffer {
	private final static int SIZE_BUFFER = 5;

	private Message[] buffer;
	private int nbMsgInBuffer = 0;
	private int nbMsgDuringBufferLife = 0;
	private int indice = 0; // on utilise un Ring donc le premier indice n'est pas toujours 0
	private boolean shutdown = false; // lié à l'objectif 2

	// quitte à utiliser un Lock autant utiliser la version équitable
	// => Attention, dans notre cas c'est toujours possible qu'il y ait famine
	private final Lock prodLock = new ReentrantLock(true);
	private final Condition prodCondition = prodLock.newCondition();
	private final Lock consLock = new ReentrantLock(true);
	private final Condition consCondition = consLock.newCondition();

	public ProdConsBuffer() {
		this.buffer = new Message[SIZE_BUFFER];
	}

	public ProdConsBuffer(int buffer_size) {
		this.buffer = new Message[buffer_size];
	}

	@Override
	public void put(Message m) throws InterruptedException {
		prodLock.lock(); // en remplacement du synchronized (moniteur)

		// test de l'état du buffer
		if (shutdown) {
			prodLock.unlock(); // si on sort pour exception, on libère le verrou
			throw new InterruptedException("Buffer is shutting down.");
		}

		// boucle d'attente
		// => La méthode signal() ne réveillera qu'un thread attendant de produire cependant un autre
		// thread souhaitant produire et qui n'est pas bloqué peut prendre la place du thread réveillé.
		// => Peut provoquer de la famine
		while (nmsg() >= buffer.length) {
			prodCondition.await(); // on attend que la condition soit libérée par la méthode get()
		}

		// si le nombre de messages dans le buffer est à zéro avant l'insertion du message en traitement
		boolean signalRequired = nmsg() == 0;

		// on insère le message dans le buffer
		putBuffer(m);
		// on met à jour les indicateurs
		nbMsgInBuffer++;
		nbMsgDuringBufferLife++;

		// on affiche le message d'information indiquant le producteur, le message produit et le nombre de messages contenus dans le buffer
		System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tProduce : " + m +"\n\tBuffer contains : "+nmsg());

		// si on était dans un état de blocage des Consommateurs avant l'insertion dans le buffer
		if (signalRequired) {
			consLock.lock();
			consCondition.signal(); // on prévient la condition des consommateurs, qu'un message a été ajouté
			consLock.unlock();
		}

		// on libère le verrou
		prodLock.unlock();
	}

	@Override
	public Message get() throws InterruptedException {
		consLock.lock(); // on verrouille, le verrou des consommateurs

		// test de l'état du buffer
		if (shutdown && nmsg() == 0) {
			consLock.unlock(); // si on sort pour cause d'extinction, il faut libérer le verrou
			throw new InterruptedException("Buffer is shutting down.");
		}

		// boucle d'attente
		while (nmsg() == 0) {
			consCondition.await();
		}

		// on regarde si le buffer est plein avant que l'on récupère notre message
		boolean signalRequired = nmsg() >= buffer.length;

		// on récupère le prochain message
		Message m = buffer[indice];
		// on met à jour les indicateurs
		incrIndice();
		nbMsgInBuffer--;

		// affichage du Consommateur, du message et du nombre de messages dans le buffer
		System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tConsume : " + m + "\n\tBuffer contains : "+nmsg());

		// si le buffer était plein avant récupération du message (blocage des producteurs), on réveille un producteur
		if (signalRequired) {
			prodLock.lock();
			prodCondition.signal(); // on prévient la condition des producteurs, qu'un message a été consommé
			prodLock.unlock();
		}

		// libération du verrou
		consLock.unlock();

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
		// on verrouille tout pour attendre la fin de l'exécution des différents threads en cours
		consLock.lock();
		prodLock.lock();
		// on change l'état
		shutdown = true;
		// on réveille tous les threads en attente
		// => pour éventuellement les arrêter car extinction
		consCondition.signalAll();
		prodCondition.signalAll();
		// on relâche les verrou
		consLock.unlock();
		prodLock.unlock();
	}

	/**
	 * @return État du buffer : True = buffer éteint
	 *                        | False = buffer ouvert
	 */
	public boolean isShutdown() {
		return shutdown;
	}
}
