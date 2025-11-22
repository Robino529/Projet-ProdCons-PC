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
		Message msg;
		while (true) {
			try {
				msg = buffer.get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
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
