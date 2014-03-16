
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKey;

public abstract class Client {

    /**
     * Subclasses have access to Socket and input/output streams *
     */
    protected Socket sock;
    protected ObjectInputStream input;
    protected ObjectOutputStream output;

    protected SecretKey secretKey;
    protected static PublicKey gsPubKey;

    static {
        try {
            gsPubKey = Utils.getPubKey("public_key.der");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("Error: " + e.getMessage() + "\n\n" + e.toString());
            e.printStackTrace(System.err);
        }
    }

    public boolean connect(final String server, final int port) {
        System.out.println("\nAttempting to connect to: " + server + ":" + port);
        try {
            @SuppressWarnings("resource")
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(server, port), 10000); // 10 second timeout
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            return diffieHellman();
        } catch (SocketTimeoutException | UnknownHostException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        return false;
    }

    public boolean isConnected() {
        return sock != null && sock.isConnected();
    }

    public void disconnect() {
        if (isConnected())
            try {
                Envelope message = new Envelope("DISCONNECT");
                output.writeObject(message);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
    }

    private boolean diffieHellman() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        int nonceLength = 16;
        byte[] nonce;

        // Nonce
        SecureRandom sr = new SecureRandom();
        nonce = new byte[nonceLength];
        sr.nextBytes(nonce);

        // Client DH key pair
        KeyPair kp = Utils.genDHKeyPair();
        PrivateKey clientPrivKey = kp.getPrivate();
        PublicKey clientPubKey = kp.getPublic();

        try {
            Envelope message, response;

            message = new Envelope("DH");
            message.addObject(nonce);
            message.addObject(clientPubKey.getEncoded());
            System.out.println("\nDH message sent to Group Server: " + message.toString());
            output.reset();
            output.writeObject(message);

            // Get the response from the server
            response = (Envelope) input.readObject();
            System.out.println("Message received from Group Server: " + response.toString());

            // Successful response
            if (response.getMessage().equals("OK")) {
                Signature sig = Signature.getInstance("SHA1withRSA");
                sig.initVerify(gsPubKey);
                sig.update(nonce);

                return true;
            }
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return false;
    }
}
