package tests;

import interfaces.Message;
import prodcons.v1.ProdConsBuffer;

public class Consumer extends Thread {
	private ProdConsBuffer buffer;
	private final int consTime;

	public Consumer(int id, ProdConsBuffer buffer, int consTime) {
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
