public class ClientController {
    private ClientModel clientModel;
    private ClientView clientView;

    public ClientController(ClientModel clientModel, ClientView clientView) {
        this.clientModel = clientModel;
        this.clientView = clientView;

        appendCommandPrompt();

        clientView.addChatboxActionListener(e -> {
            String input = clientView.getChatboxText();
            clientView.setChatboxText("");
            doCommand(input);
            clientView.appendChatLogsText(clientModel.getFunctionOutput());
            clientView.appendChatLogsText(""); // print newline
            appendCommandPrompt();
        });
    }

    private void doCommand(String input) {
        InputParser ip = InputParser.parseInput(input);

        // abort if command doesn't exist or has invalid parameters
        if (ip.getErrorMessage() != null) {
            clientView.appendChatLogsText(ip.getErrorMessage());
            return;
        }

        String command = ip.getCommand();

        if (command.equals("?")) {
            // show help menu and command documentation
            clientModel.doHelpCmd();
            return;
        }

        if (command.equals("leave")) {
            // disconnect user from server
            clientModel.doLeaveCmd();
            return;
        }

        if (command.equals("join")) {
            // connect user to server
            String host = ip.getParams(0);
            int port = Integer.parseInt(ip.getParams(1));
            clientModel.doJoinCmd(host, port);
            return;
        }

        if (command.equals("register")) {
            // allow user to register an identifying alias or handle in server
            String handle = ip.getParams(0);
            clientModel.doRegisterCmd(handle);
            return;
        }

        if (command.equals("dir")) {
            // return list of file names in server directory
            clientModel.doDirCmd();
            return;
        }

        if (command.equals("store")) {
            // fetch file from client directory and store it in server directory
            String filename = ip.getParams(0);
            clientModel.doStoreCmd(filename);
            return;
        }

        if (command.equals("get")) {
            // fetch file from server and download to client directory
            String filename = ip.getParams(0);
            clientModel.doGetCmd(filename);
            return;
        }

        if (command.equals("msg")) {
            // unicast or send message to one user in server
            String alias = ip.getParams(0);
            String message = ip.getParams(1);
            clientModel.doMsgCmd(alias, message);
            return;
        }

        if (command.equals("bc")) {
            // broadcast or send message to all users in server
            String message = ip.getParams(0);
            clientModel.doBcCmd(message);
            return;
        }
    }

    private void appendCommandPrompt() {
        String s = "Unconnected to server. Do /? to show all the available commands.";

        if (clientModel.isJoined()) {
            s = "Connected to server. Do /? to show all the available commands.";
        }

        if (clientModel.isRegistered()) {
            s = "Connected to server and registered. Do /? to show all the available commands.";
        }

        clientView.appendChatLogsText(s);
    }
}
