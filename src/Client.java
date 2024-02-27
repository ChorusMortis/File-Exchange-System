public class Client {
    public static void main(String[] args) {
        ClientView clientView = new ClientView();
        ClientModel clientModel = new ClientModel(clientView);
        ClientController clientController = new ClientController(clientModel, clientView);
    }
}
