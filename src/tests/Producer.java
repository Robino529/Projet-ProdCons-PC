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
		int count = 1;
		long startTime = System.currentTimeMillis();

		while (nMsgToProd > 0) {
			try {
				buffer.put(new Message("Message n°"+count));
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

//			System.out.println("Message n°" + count + " produced by " + Producer.this.getName());

			count++;
			nMsgToProd--;
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
