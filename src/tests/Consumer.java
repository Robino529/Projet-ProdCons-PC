package tests;

import interfaces.IProdConsBuffer;
import interfaces.Message;

public class Consumer extends Thread {
	protected IProdConsBuffer buffer;
	protected final int consTime;

	public Consumer(int id, IProdConsBuffer buffer, int consTime) {
		super("Consumer-" + id);
		this.buffer = buffer;
		this.consTime = consTime;
	}

	@Override
	public void run() {
		// variable locale utilisée à chaque tour de boucle
		Message msg; // inutile en l'état car le buffer affiche le message au moment où il le donne mais c'est un cas particulier du projet

		// boucle infinie
		while (true) {
			// on récupère le message suivant du buffer
			try {
				msg = buffer.get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			// affiche le message consommé et le nom du consommateur
//			System.out.println(this.getName() + " consume :\n" + msg);

			// on s'endort un temps donné
			try {
				sleep(consTime);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
