package prodcons.v2;

import interfaces.IProdConsBuffer;
import interfaces.Message;

public class Consumer extends tests.Consumer {
	public Consumer(int id, IProdConsBuffer buffer, int consTime) {
		super(id, buffer, consTime);
	}

	@Override
	public void run() {
		Message msg;
		while (true) {
			try {
				msg = buffer.get();
				if (msg == null) { break; }
			} catch (InterruptedException e) {
				break;
			}

//			System.out.println(this.getName() + " consume :\n" + msg);

			try {
				sleep(consTime);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
