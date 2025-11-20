package prodcons;

public interface IProdConsBuffer {
    // Objectif 1 & 3
    void put(Message m) throws InterruptedException;
    Message get() throws InterruptedException;
    int nmsg();
    int totmsg();

    // Objectif 5
    default Message[] get(int k) throws InterruptedException { return null; }

    // Objectif 6
    default void put(Message m, int n) throws InterruptedException { }
}