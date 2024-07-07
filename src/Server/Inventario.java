package Server;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Inventario {
    private HashMap<String, Integer> products = new HashMap<>();
    private static final String FILE = "Server/inventario.txt";
    
    
    public Inventario() {
        carregar();
    }

    public synchronized String StockResponse() {
        carregar();
        StringBuilder stockList = new StringBuilder();
        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            stockList.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return stockList.toString();
    }

    public synchronized String StockUpdate(String productCode, int quantity) {
       
        String uppercaseProductCode = productCode.toUpperCase();
    
        if (products.containsKey(uppercaseProductCode)) {
            int currentStock = products.get(uppercaseProductCode);
            int newStock = currentStock + quantity;
    
            if (newStock >= 0) {
                
                products.put(uppercaseProductCode, newStock);
                guardar();
                

                return "STOCK_UPDATED"+ "\n" + uppercaseProductCode + " " + newStock;


            } else {
                return "STOCK_ERROR - Invalid stock quantity";
            }
        } else {
            return "STOCK_ERROR - Invalid product code";
        }
    }    

// guardar o inventario para o ficheiro
        public void guardar() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE))) {
            for (Map.Entry<String, Integer> entry : products.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

// carregar o inventario para o ficheiro
        public void carregar() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    String productCode = parts[0];
                    int quantity = Integer.parseInt(parts[1]);
                    products.put(productCode, quantity);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // se o ficheiro nao existir criar novo inventario
            products = new HashMap<>();
            products.put("A", 500);
            products.put("B", 200);
            products.put("C", 300);
            products.put("D", 150);
        }
    }
}