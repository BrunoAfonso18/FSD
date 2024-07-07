package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SecureDirectNotification extends Remote{

    void StockUpdated(String message) throws RemoteException;
    String stock_updated_signed(String message, String signature) throws RemoteException;
}