import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientThread extends Thread {
    private DataInputStream reader;
    private ClientView clientView;
    private boolean keepGoing = true;

    public ClientThread(Socket s, ClientView clientView) {
        this.clientView = clientView;
        try {
            this.reader = new DataInputStream(s.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (keepGoing) {
            try {
                String message;
                while (keepGoing) {
                    message = reader.readUTF();
                    if (message != null) {
                        clientView.appendChatLogsText(message);
                    }
                }
            } catch (Exception e) {
                quit();
            }
        }
    }

    public void quit() {
        keepGoing = false;
    }
}
