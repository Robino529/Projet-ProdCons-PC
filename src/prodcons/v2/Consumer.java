package prodcons.v2;

import interfaces.IProdConsBuffer;
import interfaces.Message;

public class Consumer extends tests.Consumer {
	public Consumer(int id, IProdConsBuffer buffer, int consTime) {
		super(id, buffer, consTime);
	}

	@Override
	public void run() {
		// variable locale utilisée à chaque tour de boucle
		Message msg; // inutile en l'état car le buffer affiche le message au moment où il le donne mais c'est un cas particulier du projet

		// boucle infinie qui s'arrête lorsque le buffer est éteint et qu'il ne reste plus aucun message à lire
		while (true) {
			// on récupère le message suivant du buffer
			try {
				msg = buffer.get();
			}
			// condition d'arrêt standard du consommateur, c'est une exception de ce type que renvoie le buffer
			// lorsqu'il est éteint et qu'il ne reste aucun message à lire
			// NOTE : Il peut être préférable de créer une exception spécifique d'arrêt pour éviter de mélanger un
			//        arrêt classique d'une autre Interruption de thread.
			catch (InterruptedException e) {
				break;
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
