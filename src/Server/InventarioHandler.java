package Server;

import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class InventarioHandler implements Runnable {
    private Socket clientSocket;
    private Inventario inventory;
    private Server server;

    public InventarioHandler(Socket clientSocket, Inventario inventory, Server server) {
        this.clientSocket = clientSocket;
        this.inventory = new Inventario();
        this.server = server;
    }


    @Override
    public void run() {
        try {
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));


            String request;
            while ((request = reader.readLine()) != null) {

                inventory.carregar();

                if (request.equals("DISCONNECT")) {
                    System.out.println("Client at " + clientSocket.getInetAddress() + " disconnected.");
                    break;

                } else if (request.equals("STOCK_REQUEST")) {
                    inventory.carregar();
                    String stockList = inventory.StockResponse();

                    writer.write(stockList);
                    writer.write("STOCK_RESPONSE");
                    writer.newLine();
                    writer.flush();

                } else if (request.startsWith("STOCK_UPDATE")) {
                    String[] parts = request.split(" ");
                    if (parts.length == 3) {
                        String productCode = parts[1];
                        int quantity = Integer.parseInt(parts[2]);
                        String response = inventory.StockUpdate(productCode, quantity);
                        inventory.guardar();

                        writer.write(response);
                        writer.newLine();
                        writer.flush();

                    } else {
                        writer.write("STOCK_ERROR");
                        writer.newLine();
                        writer.flush();
                    }

                } else if (request.equals("GET_PUBKEY")) {
                    // Send the server's public key to the client
                    PublicKey pubKey = server.get_pubKey();
                    String pubKeyBase64 = Base64.getEncoder().encodeToString(pubKey.getEncoded());

                    writer.write(pubKeyBase64);
                    writer.newLine();
                    writer.flush();
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String signResponse(String message) {
        try {

            byte[] signature = server.createSignature(message);

            String base64Signature = Base64.getEncoder().encodeToString(signature);
    
            return message + "|" + base64Signature;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
