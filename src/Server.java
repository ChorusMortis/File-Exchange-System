import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static ArrayList<File> serverFiles = new ArrayList<>();
    private static Map<String, Socket> aliasToMessageEndpoint = new HashMap<>();

    public static void main(String[] args) {
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            // used for file server transfers
            ServerSocket fileSocket = new ServerSocket(port);

            // used for messages
            ServerSocket messageSocket = new ServerSocket(5555);

            System.out.println("Server: Listening on port " + port);
            while (true) {
                Socket fileEndpoint = fileSocket.accept();
                Socket messageEndpoint = messageSocket.accept();
                System.out.println("Server: Client at " + fileEndpoint.getRemoteSocketAddress() + " has connected");
                ServerThread st = new ServerThread(fileEndpoint, messageEndpoint, serverFiles, aliasToMessageEndpoint);
                st.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
