import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class ServerThread extends Thread {
    private Socket fileEndpoint;
    private Socket messageEndpoint;
    private DataInputStream reader;
    private DataOutputStream writer;
    private DataOutputStream messageWriter;
    private FileOutputStream fileWriter;
    private FileInputStream fileReader;

    private String registeredHandle;
    private ArrayList<File> serverFiles;
    private Map<String, Socket> aliasToMessageEndpoint;

    public ServerThread(Socket fileEndpoint, Socket messageEndpoint, ArrayList<File> serverFiles,
            Map<String, Socket> aliasToMessageEndpoint) {
        this.fileEndpoint = fileEndpoint;
        this.messageEndpoint = messageEndpoint;
        this.serverFiles = serverFiles;
        this.aliasToMessageEndpoint = aliasToMessageEndpoint;
    }

    @Override
    public void run() {
        try {
            String msg;
            reader = new DataInputStream(fileEndpoint.getInputStream());
            writer = new DataOutputStream(fileEndpoint.getOutputStream());

            // continue listening for commands until user disconnects
            while (!(msg = reader.readUTF()).equals("DISCONNECT")) {

                if (msg.equals("REGISTER")) {
                    doRegisterCmd();
                    continue;
                }

                if (msg.equals("DIR")) {
                    doDirCmd();
                    continue;
                }

                if (msg.equals("STORE")) {
                    doStoreCmd();
                    continue;
                }

                if (msg.equals("GET")) {
                    doGetCmd();
                    continue;
                }

                if (msg.equals("MESSAGE")) {
                    doMsgCmd();
                    continue;
                }

                if (msg.equals("BROADCAST")) {
                    doBcCmd();
                    continue;
                }
            }

            doDisconnectCmd();
            reader.close();
            writer.close();
            fileEndpoint.close();
            messageEndpoint.close();
        } catch (SocketException e) {
            doDisconnectCmd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDisconnectCmd() {
        System.out.println("Server: Client at " + fileEndpoint.getRemoteSocketAddress() + " has disconnected");

        // don't broadcast anything if user is not registered
        if (registeredHandle == null) {
            return;
        }

        synchronized (aliasToMessageEndpoint) {
            aliasToMessageEndpoint.remove(registeredHandle);

            aliasToMessageEndpoint.forEach((alias, endpoint) -> {
                // skip clients not registered
                if (endpoint == null) {
                    return;
                }

                try {
                    messageWriter = new DataOutputStream(endpoint.getOutputStream());
                    messageWriter.writeUTF(String.format("%s disconnected from the server.", registeredHandle));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void doRegisterCmd() {
        try {
            String handle = reader.readUTF();

            synchronized (aliasToMessageEndpoint) {
                if (aliasToMessageEndpoint.containsKey(handle)) {
                    writer.writeUTF("HANDLE_EXISTS");
                    return;
                }

                registeredHandle = handle;
                aliasToMessageEndpoint.put(handle, messageEndpoint);
                writer.writeUTF("REGISTRATION_DONE");

                aliasToMessageEndpoint.forEach((alias, endpoint) -> {
                    // skip clients not registered
                    if (endpoint == null) {
                        return;
                    }

                    try {
                        messageWriter = new DataOutputStream(endpoint.getOutputStream());
                        messageWriter.writeUTF(String.format("%s joined the server.", handle));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDirCmd() {
        updateFileList();

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Server Directory");

            synchronized (serverFiles) {
                if (serverFiles.size() == 0) {
                    sb.append("\nNo files found");
                    writer.writeUTF(sb.toString());
                    return;
                }

                for (File file : serverFiles) {
                    sb.append("\n");
                    sb.append(file.getName());
                }
            }

            writer.writeUTF(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doStoreCmd() {
        try {
            String filename = reader.readUTF();
            long fileLength = reader.readLong();
            fileWriter = new FileOutputStream(System.getProperty("user.dir") + "\\server_files\\" + filename);

            int bytes = 0;
            long totalBytes = 0;
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = reader.read(buffer, 0, (int) Math.min(fileLength - totalBytes, buffer.length))) != -1 &&
                    totalBytes < fileLength) {
                fileWriter.write(buffer, 0, bytes);
                totalBytes += bytes;
            }

            Date dateTime = new Date();
            synchronized (aliasToMessageEndpoint) {
                aliasToMessageEndpoint.forEach((alias, endpoint) -> {
                    // skip clients not registered
                    if (endpoint == null) {
                        return;
                    }

                    try {
                        messageWriter = new DataOutputStream(endpoint.getOutputStream());
                        messageWriter
                                .writeUTF(String.format("%s<%s>: Uploaded %s", registeredHandle, dateTime.toString(),
                                        filename));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            fileWriter.close();

            updateFileList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doGetCmd() {
        try {
            String filename = reader.readUTF();

            File file = new File(System.getProperty("user.dir") + "\\server_files\\" + filename);
            fileReader = new FileInputStream(System.getProperty("user.dir") + "\\server_files\\" + filename);
            writer.writeUTF("FILE_EXISTS");

            long fileLength = file.length();
            writer.writeLong(fileLength);

            int bytes = 0;
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileReader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytes);
            }

            fileReader.close();
        } catch (FileNotFoundException e) {
            try {
                writer.writeUTF("FILE_NOT_IN_SERVER");
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doMsgCmd() {
        synchronized (aliasToMessageEndpoint) {
            try {
                String receiverHandle = reader.readUTF();
                String senderHandle = reader.readUTF();
                String message = reader.readUTF();

                messageWriter = new DataOutputStream(messageEndpoint.getOutputStream());

                if (senderHandle.equals(receiverHandle)) {
                    messageWriter.writeUTF(
                            "Error: Unicast messaging failed. You cannot message yourself. That'd be pretty weird.");
                    return;
                }

                if (!aliasToMessageEndpoint.containsKey(receiverHandle)) {
                    messageWriter.writeUTF(
                            "Error: Unicast messaging failed. Specified alias or handle does not exist/is not registered in the server.");
                    return;
                }

                // send message to recipient
                messageWriter = new DataOutputStream(aliasToMessageEndpoint.get(receiverHandle).getOutputStream());
                messageWriter.writeUTF(String.format("Message from %s: %s", senderHandle, message));

                // let sender know they've sent the message successfully
                messageWriter = new DataOutputStream(messageEndpoint.getOutputStream());
                messageWriter.writeUTF("Message sent successfully!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void doBcCmd() {
        synchronized (aliasToMessageEndpoint) {
            try {
                String senderHandle = reader.readUTF();
                String message = reader.readUTF();

                aliasToMessageEndpoint.forEach((alias, endpoint) -> {
                    // skip clients not registered
                    if (endpoint == null) {
                        return;
                    }

                    try {
                        messageWriter = new DataOutputStream(endpoint.getOutputStream());
                        messageWriter.writeUTF(String.format("Broadcast from %s: %s", senderHandle, message));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateFileList() {
        synchronized (serverFiles) {
            serverFiles.clear();

            try {
                // create ./server_files directory if it doesn't exist
                Files.createDirectories(Paths.get("./server_files"));

                // get files in server directory
                File[] files = new File("./server_files").listFiles();

                // abort if ./server_files cannot be accessed
                if (files == null) {
                    return;
                }

                for (File file : files) {
                    if (file.isFile()) {
                        serverFiles.add(file);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
