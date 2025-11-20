package prodcons;

public class Message {
    private String content;
    private int id;

    public Message(String content, int id) {
        this.content = content;
        this.id = id;
    }

    @Override
    public String toString() {
        return "Msg#" + id + ":" + content;
    }
}