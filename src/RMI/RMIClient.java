package RMI;

import Server.SecureDirectNotification;
import Server.StockServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Scanner;

public class RMIClient extends UnicastRemoteObject implements SecureDirectNotification{
    private StockServer server;
    private boolean isConnected;
    private String clientID;
    private KeyPair keyPair;
    PublicKey pubKey = null;

    public RMIClient() throws RemoteException{
        super();
        this.isConnected = false;
    }

    public void connectToServer(String host, int port) {
        try {

            Registry registry = LocateRegistry.getRegistry(host, port);
            server = (StockServer) registry.lookup("StockServer");

            clientID = "Client_" + System.currentTimeMillis();
            server.subscribe(clientID, this);

            isConnected = true;

            System.out.println("Connected to the server. Client ID: " + clientID);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (isConnected) {
            try {
                server.unsubscribe(clientID, this);
                System.out.println("Disconnected from the server.");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        if (!isConnected) {
            System.out.println("RMI client is not connected to the server.");
            return;
        }

        try {
            
            try {
                pubKey = server.get_pubKey();
            } catch (Exception e) {
                System.out.println("Key not Found ");
            }

            try (Scanner opt = new Scanner(System.in)) {
                displayMenu();

                while (true) {
                    try {
                        int option = opt.nextInt();

                        switch (option) {
                            case 1:
                                String stock = server.stockRequest();
                                System.out.println("\nSTOCK_RESPONSE:\n" + stock);
                                
                                displayMenu();
                                break;

                            case 2:
                                System.out.print("Enter product code: ");
                                String productCode = opt.next();
                                System.out.print("Enter quantity: ");
                                int quantity = opt.nextInt();

                                String response = server.stockUpdate(productCode, quantity);
                                System.out.println("Message: " + response);
                                
                                displayMenu();
                                break;

                            case 3:
                                disconnect();
                                return;

                            default:
                                System.out.println("Invalid option. Please choose again.");
                                displayMenu();
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please choose again.");
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void StockUpdated(String message) throws RemoteException {
        if(message.startsWith("STOCK_UPDATE")) {
            System.out.println("\nNotification " + message);
        }
    }

    @Override
    public String stock_updated_signed(String message, String signature) throws java.rmi.RemoteException {
        try {
            message = message.trim();
            Signature sig = Signature.getInstance("SHA256withDSA");
            sig.initVerify(pubKey);
            sig.update(message.getBytes());
            if (sig.verify(Base64.getDecoder().decode(signature))) {
                System.out.println("\nSignature correct\n");
                System.out.println(message);
            } else {
                System.out.println("Signature failed");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        return message;
    }
    
    private void displayMenu() {
        System.out.println("\nMenu:\n[1] Send STOCK_REQUEST\n[2] Send STOCK_UPDATE\n[3] Send DISCONNECT");
        System.out.print("Choose an option: ");
    }

    public static void main(String[] args) throws RemoteException {
        String host = "127.0.0.1"; // Specify the desired host
        int port = 1999; // Specify the desired port
        RMIClient client = new RMIClient();
        client.connectToServer(host, port);
        client.run();
    }
}




