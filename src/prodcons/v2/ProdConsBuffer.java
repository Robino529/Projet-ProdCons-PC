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
		if (shutdown) {
			throw new InterruptedException("Buffer is shutting down.");
		}

		while (nmsg() >= buffer.length) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}

		putBuffer(m);
		nbMsgInBuffer++;
		nbMsgDuringBufferLife++;
		notifyAll();
		System.out.println("Thread : " + Thread.currentThread().getName() + "\n\tProduce : " + m);
	}

	@Override
	public synchronized Message get() throws InterruptedException {
		if (shutdown) {
			throw new InterruptedException("Buffer is shutting down.");
		}

		while (nmsg() == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}

		Message m = buffer[indice];
		incrIndice();
		nbMsgInBuffer--;
		notifyAll();
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
