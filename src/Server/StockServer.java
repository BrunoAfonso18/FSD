package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface StockServer extends Remote {
    String stockRequest() throws RemoteException;
    String stockUpdate(String productCode, int quantity) throws RemoteException;
    void subscribe(String clientID, SecureDirectNotification client) throws RemoteException;
    void unsubscribe(String clientID, SecureDirectNotification client) throws RemoteException;
    PublicKey get_pubKey() throws RemoteException;
    void notifySubscribers(String message, String signature) throws RemoteException;
}
