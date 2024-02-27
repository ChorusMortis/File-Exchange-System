import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientModel {
    private ClientView clientView;

    private boolean joined = false;
    private boolean registered = false;

    private String functionOutput;
    private String registeredHandle;

    private Socket fileEndpoint;
    private Socket messageEndpoint;
    private ClientThread ct;
    private DataInputStream reader;
    private DataOutputStream writer;
    private FileOutputStream fileWriter;
    private FileInputStream fileReader;

    public ClientModel(ClientView clientView) {
        this.clientView = clientView;
    }

    public void doHelpCmd() {
        String s = """
                Available commands:
                /?                                    Show this help text.
                /join <server_ip_address> <port>      Connect to the server application.
                /leave                                Disconnect from the server application.
                /register <handle>                    Register a unique handle or alias.
                /dir                                  Request directory list from the server.
                /store <filename>                     Send file to the server.
                /get <filename>                       Fetch a file from the server.
                /msg <handle> <message>               Message a fellow user in the server.
                /bc <message>                         Message all users in the server.
                """;

        functionOutput = s;
    }

    public void doLeaveCmd() {
        if (!joined) {
            functionOutput = "Error: Disconnection failed. Please connect to the server first.";
            return;
        }

        try {
            writer.writeUTF("DISCONNECT");
            functionOutput = "Client: Connection closed: Thank you!";
        } catch (SocketException e) {
            functionOutput = "Error: Server connection has terminated.";
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            terminateServerConnection();
        }
    }

    public void doJoinCmd(String host, int port) {
        if (joined) {
            functionOutput = "Error: You are already connected to the server.";
            return;
        }

        StringBuilder sb = new StringBuilder();
        try {
            sb.append(String.format("Client: Connecting to server %s:%d\n", host, port));
            fileEndpoint = new Socket();
            messageEndpoint = new Socket();
            // time out after 10s
            fileEndpoint.connect(new InetSocketAddress(host, port), 10000);
            messageEndpoint.connect(new InetSocketAddress(host, 5555), 10000);
            joined = true;

            reader = new DataInputStream(fileEndpoint.getInputStream());
            writer = new DataOutputStream(fileEndpoint.getOutputStream());
            ct = new ClientThread(messageEndpoint, clientView);
            ct.start();

            sb.append(String.format("Client: Connected to server %s:%d\n", host, port));
            sb.append("Connection to the File Exchange Server is successful!");
        } catch (Exception e) {
            sb.append("Error: Connection to the server has failed! Please check IP and port number.");
        } finally {
            functionOutput = sb.toString();
        }
    }

    public void doRegisterCmd(String handle) {
        if (!joined) {
            functionOutput = "Error: Registration failed. Please connect to the server first.";
            return;
        }

        if (registered) {
            functionOutput = "Error: Registration failed. You already have a registered alias.";
            return;
        }

        try {
            writer.writeUTF("REGISTER");
            writer.writeUTF(handle);

            String response = reader.readUTF();
            if (response.equals("HANDLE_EXISTS")) {
                functionOutput = "Error: Registration failed. Handle or alias already exists.";
                return;
            }

            registered = true;
            registeredHandle = handle;
            Files.createDirectories(Paths.get("./client_files/" + handle));
        } catch (SocketException e) {
            functionOutput = "Error: Server connection has terminated.";
            terminateServerConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doDirCmd() {
        if (!joined) {
            functionOutput = "Error: Requesting directory file list failed. Please connect to the server first.";
            return;
        }

        if (!registered) {
            functionOutput = "Error: Requesting directory file list failed. Register an alias first.";
            return;
        }

        try {
            writer.writeUTF("DIR");
            functionOutput = reader.readUTF();
        } catch (SocketException e) {
            functionOutput = "Error: Server connection has terminated.";
            terminateServerConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doStoreCmd(String filename) {
        if (!joined) {
            functionOutput = "Error: Sending file to server failed. Please connect to the server first.";
            return;
        }

        if (!registered) {
            functionOutput = "Error: Sending file to server failed. Register an alias first.";
            return;
        }

        try {
            File file = new File(
                    System.getProperty("user.dir") + "\\client_files\\" + registeredHandle + "\\" + filename);
            fileReader = new FileInputStream(
                    System.getProperty("user.dir") + "\\client_files\\" + registeredHandle + "\\" + filename);

            writer.writeUTF("STORE");
            writer.writeUTF(filename);

            long fileLength = file.length();
            writer.writeLong(fileLength);

            int bytes = 0;
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileReader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytes);
            }

            fileReader.close();
        } catch (FileNotFoundException e) {
            functionOutput = "Error: File not found.";
        } catch (SocketException e) {
            functionOutput = "Error: Server connection has terminated.";
            terminateServerConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doGetCmd(String filename) {
        if (!joined) {
            functionOutput = "Error: Getting file from server failed. Please connect to the server first.";
            return;
        }

        if (!registered) {
            functionOutput = "Error: Getting file from server failed. Register an alias first.";
            return;
        }

        try {
            // send the filename to the server
            writer.writeUTF("GET");
            writer.writeUTF(filename);

            // check if file exists
            String fileExistsResponse = reader.readUTF();
            if (fileExistsResponse.equals("FILE_NOT_IN_SERVER")) {
                functionOutput = "Error: File not found in the server.";
                return;
            }

            long fileLength = reader.readLong();
            fileWriter = new FileOutputStream(
                    System.getProperty("user.dir") + "\\client_files\\" + registeredHandle + "\\" + filename);

            int bytes = 0;
            long totalBytes = 0;
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = reader.read(buffer, 0, (int) Math.min(fileLength - totalBytes, buffer.length))) != -1 &&
                    totalBytes < fileLength) {
                fileWriter.write(buffer, 0, bytes);
                totalBytes += bytes;
            }

            functionOutput = "File received from Server: " + filename;
            fileWriter.close();
        } catch (SocketException e) {
            functionOutput = "Error: Server connection has terminated.";
            terminateServerConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doMsgCmd(String alias, String message) {
        if (!joined) {
            functionOutput = "Error: Unicast messaging failed. Please connect to the server first.";
            return;
        }

        if (!registered) {
            functionOutput = "Error: Unicast messaging failed. Register an alias first.";
            return;
        }

        try {
            writer.writeUTF("MESSAGE");
            writer.writeUTF(alias);
            writer.writeUTF(registeredHandle);
            writer.writeUTF(message);
        } catch (SocketException e) {
            functionOutput = "Error: Server connection has terminated.";
            terminateServerConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doBcCmd(String message) {
        if (!joined) {
            functionOutput = "Error: Broadcast messaging failed. Please connect to the server first.";
            return;
        }

        if (!registered) {
            functionOutput = "Error: Broadcast messaging failed. Register an alias first.";
            return;
        }

        try {
            writer.writeUTF("BROADCAST");
            writer.writeUTF(registeredHandle);
            writer.writeUTF(message);
        } catch (SocketException e) {
            functionOutput = "Error: Server connection has terminated.";
            terminateServerConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void terminateServerConnection() {
        try {
            reader.close();
            writer.close();
            fileEndpoint.close();
            if (ct != null) {
                ct.quit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            joined = false;
            registered = false;
            registeredHandle = null;
            fileEndpoint = null;
            ct = null;
            reader = null;
            writer = null;
        }
    }

    public String getFunctionOutput() {
        String s = functionOutput;
        functionOutput = null;
        return s;
    }

    public boolean isJoined() {
        return joined;
    }

    public boolean isRegistered() {
        return registered;
    }
}
