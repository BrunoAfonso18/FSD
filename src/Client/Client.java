package Client;

import java.util.*;
import java.net.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.io.*;

public class Client {
	
	static final int DEFAULT_PORT=2000;
	static final String DEFAULT_HOST="127.0.0.1";
    static final int interval= 10000;

    static private void displayMenu() {
        System.out.println("\nMenu:\n[1] Send STOCK_REQUEST\n[2] Send STOCK_UPDATE\n[3] Send DISCONNECT");
        System.out.print("Choose an option: ");
    }

    private static PublicKey requestServerPublicKey(BufferedReader reader, BufferedWriter writer) throws IOException {
        // Send a "GET_PUBKEY" message to the server
        writer.write("GET_PUBKEY");
        writer.newLine();
        writer.flush();
        
        String stringServerKey = reader.readLine();

        try {
            return KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(stringServerKey)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	public static void main(String[] args) {

        Scanner opt = new Scanner(System.in);
		
        String servidor=DEFAULT_HOST;
		int port=DEFAULT_PORT;
		
       
            try (Socket socket = new Socket(servidor, port)) {
                
                System.out.println("Connected to the server!");

                
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                PublicKey serverKey = requestServerPublicKey(reader, writer);
                System.out.println(serverKey);

                Thread stockRequestThread = new Thread(() -> {
                    while (!socket.isClosed()) {
                        try {
                            writer.write("STOCK_REQUEST");
                            writer.newLine();
                            writer.flush();
    
                            String response;
                            System.out.println("\n\nStock");
                            while ((response = reader.readLine()) != null) {
                                if (response.equals("STOCK_RESPONSE")) {
                                    break;
                                }
                                System.out.println("> " + response);
                            }
                            displayMenu();
                            

    
                            Thread.sleep(interval);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                
                stockRequestThread.start();

                while(true){
                    try {
                        int option = opt.nextInt();

                switch (option) {
                    case 1:
                        writer.write("STOCK_REQUEST");
                        writer.newLine();
                        writer.flush();    

                        String response;
                        while ((response = reader.readLine()) != null) {
                            if (response.equals("STOCK_RESPONSE")) {
                                break;
                            }
                            System.out.println("> " + response);
                        }

                        break;
                
                    case 2:
                        
                        System.out.print("Enter product code: ");
                        String productCode = opt.next();
                        System.out.print("Enter quantity: ");
                        int quantity = opt.nextInt();
                        String stockUpdate = "STOCK_UPDATE" + " " + productCode + " " + quantity;
                        
                        writer.write(stockUpdate);
                        writer.newLine();
                        writer.flush();
                        
                      
                        response = reader.readLine();
                        System.out.println("Received from server: " + response);

                        break;

                    case 3:

                        writer.write("DISCONNECT");
                        writer.newLine();
                        writer.flush();
                        socket.close();
                        System.out.println("Connection Terminated"); 
                        return; 

                    default:

                        System.out.println("Opcao invalida\n");
                        displayMenu();
                }
                        
                    } catch (NumberFormatException e) {
                        System.out.println("Wrong character type");
                    }
                    
                
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
	
	}
}
