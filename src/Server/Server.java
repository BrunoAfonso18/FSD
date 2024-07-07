package Server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.net.*;
import java.io.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements StockServer {
    private Inventario inventory;
    private Map<String, SecureDirectNotification> RMISubscribers;
    private ServerSocket serverSocket;
    static final int DEFAULT_PORT_RMI = 1999;
    static final int DEFAULT_PORT_SOCKET = 2000;
    public PublicKey pubKey;
    private PrivateKey privKey;
    private KeyPair pair;

    public Server() throws RemoteException {
        super();
        inventory = new Inventario();
        RMISubscribers = new HashMap<>();
    }

    public void startRMIServer() {
        try {
            LocateRegistry.createRegistry(DEFAULT_PORT_RMI);
            Registry registry = LocateRegistry.getRegistry(DEFAULT_PORT_RMI);
            registry.rebind("StockServer", this);

            System.out.println("RMI server is running.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startSocketServer() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT_SOCKET);
            System.out.println("Socket server is running. Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new InventarioHandler(clientSocket, inventory, this)).start();
                System.out.println("Server connected to " + clientSocket.getInetAddress() + " (" + clientSocket.getPort() + ")");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String stockRequest() throws RemoteException {
        String message = inventory.StockResponse();
        byte[] signature;
        try {
            signature = createSignature(message);
            return message + "|" + Base64.getEncoder().encodeToString(signature);
        } catch (InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
        
    }

    @Override
    public String stockUpdate(String productCode, int quantity) throws RemoteException {
        String notification = inventory.StockUpdate(productCode, quantity);
        
        try {
            byte[] signature = createSignature(notification);
            notifySubscribers(notification, Base64.getEncoder().encodeToString(signature));
            return notification + "|" + Base64.getEncoder().encodeToString(signature);
        } catch (InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void subscribe(String clientID, SecureDirectNotification client) throws RemoteException {
        RMISubscribers.put(clientID, client);
        printClientIP(clientID);
    }

    @Override
    public void unsubscribe(String clientID, SecureDirectNotification client) throws RemoteException {
        RMISubscribers.remove(clientID, client);
        System.out.println("Client " + clientID + " unsubscribed.");
    }

    @Override
    public void notifySubscribers(String message, String signature) throws RemoteException {
        for (SecureDirectNotification subscriber : RMISubscribers.values()) {
            subscriber.stock_updated_signed(message, signature);
        }
    }

    public void generateKeys(){
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DSA");
            keyPairGen.initialize(1024);

            this.pair = keyPairGen.generateKeyPair();
            this.pubKey = pair.getPublic();
            this.privKey = pair.getPrivate();

            System.out.println("Public key: " + Base64.getEncoder().encodeToString(pubKey.getEncoded()));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] createSignature(String message) throws InvalidKeyException, SignatureException{
        try {
            Signature sign = Signature.getInstance("SHA256withDSA");
            sign.initSign(privKey);
            byte[] bytes = message.getBytes();
            sign.update(bytes);
            byte[] signature = sign.sign();
            return signature;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void printClientIP(String clientId) {
        try {
            String clientIP = RemoteServer.getClientHost();
            System.out.println("RMI client " + clientId + " connected from: " + clientIP);
        } catch (ServerNotActiveException e) {
            e.printStackTrace();
        }
    }

    public PublicKey get_pubKey() throws RemoteException{
        return this.pubKey;
    }

    public static void main(String[] args) {
        Server server = null;
        try {
            server = new Server();
            server.generateKeys();
            server.startRMIServer();
            server.startSocketServer();
            System.out.println("Server is running.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}