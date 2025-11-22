package prodcons.v4;

import interfaces.IProdConsBuffer;
import interfaces.Message;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implémentation de la Solution Directe à partir de :
 *
 * | Opération           | Pre-action   | Garde                             | Post-action                                                          |
 * |---------------------|--------------|-----------------------------------|----------------------------------------------------------------------|
 * | void put(Message m) |              | nbMsgInBuffer < buffer.length     | putBuffer(m); nbMsgInBuffer++; nbMsgDuringBufferLife++; notifyAll(); |
 * | Message get()       |              | nbMsgInBuffer > 0                 | return buffer[indice]; incrIndice(); nbMsgInBuffer--; notifyAll();   |
 *
 */

public class ProdConsBuffer implements IProdConsBuffer {
	private final static int SIZE_BUFFER = 5;

	private Message[] buffer;
	private int nbMsgInBuffer = 0;
	private int nbMsgDuringBufferLife = 0;
	private int indice = 0; // on utilise un Ring donc le premier indice n'est pas toujours 0
	private boolean shutdown = false; // lié à l'objectif 2

	// quitte à utiliser un Lock autant utiliser la version équitable qui garantit l'absence de famine
	private Lock prodLock = new ReentrantLock(true);
	private Condition prodCondition = prodLock.newCondition();
	private Lock consLock = new ReentrantLock(true);
	private Condition consCondition = consLock.newCondition();

	public ProdConsBuffer() {
		this.buffer = new Message[SIZE_BUFFER];
	}

	public ProdConsBuffer(int buffer_size) {
		this.buffer = new Message[buffer_size];
	}

	@Override
	public void put(Message m) throws InterruptedException {
		prodLock.lock();

		if (shutdown) {
			prodLock.unlock(); // si on sort pour exception, on libère le verrou
			throw new InterruptedException("Buffer is shutting down.");
		}

		// la méthode Signal ne réveillera qu'un thread attendant de produire cependant un autre
		// thread souhaitant produire et qui n'est pas bloqué peut prendre la place du thread réveillé
		while (nmsg() >= buffer.length) {
			prodCondition.await(); // on attend que la condition soit libérée par la méthode get()
		}

		putBuffer(m);
		nbMsgInBuffer++;
		nbMsgDuringBufferLife++;
		consCondition.signal(); // on prévient la condition des consommateurs, qu'un message a été ajouté
		prodLock.unlock(); // on libère le verrou ici, l'E/S juste après va provoquer la commutation mais on aura terminé
		System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tProduce : " + m);
	}

	@Override
	public Message get() throws InterruptedException {
		consLock.lock();

		if (shutdown) {
			consLock.unlock();
			throw new InterruptedException("Buffer is shutting down.");
		}

		while (nmsg() == 0) {
			consCondition.await();
		}

		Message m = buffer[indice];
		incrIndice();
		nbMsgInBuffer--;
		prodCondition.signal();
		consLock.unlock();
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
	 * Exécuter uniquement via des méthodes synchronized, permet d'incrémenter l'indice en respectant le Ring
	 */
	private void incrIndice() {
		indice = (indice+1) % buffer.length; // le modulo permet de respecter le ring
	}

	private void putBuffer(Message m) {
		buffer[(indice+nbMsgInBuffer) % buffer.length] = m;
	}

	public void shutdown() {
		shutdown = true;
	}

	public boolean isShutdown() {
		return shutdown;
	}
}
