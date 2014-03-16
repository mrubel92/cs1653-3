
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyAgreement;
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
        gsPubKey = Utils.getPubKey("public_key.der");
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
        KeyPair dhKP = Utils.genDHKeyPair();
        PrivateKey clientDHPrivKey = dhKP.getPrivate();
        PublicKey clientDHPubKey = dhKP.getPublic();

        try {
            Envelope message, response;

            // Send server nonce to sign and client's DH public key
            message = new Envelope("DH");
            message.addObject(nonce);
            message.addObject(clientDHPubKey.getEncoded());
            System.out.println("\nDH message sent to Group Server: " + message.toString());
            output.reset();
            output.writeObject(message);

            // Get the response from the server
            response = (Envelope) input.readObject();
            System.out.println("Message received from Group Server: " + response.toString());

            // Successful response
            if (response.getMessage().equals("OK")) {
                byte[] signedNonce = (byte[]) response.getObjContents().get(0);
                byte[] servDHPubKeyBytes = (byte[]) response.getObjContents().get(1);
                
                // Verify nonce first
                Signature sig = Signature.getInstance("SHA1withRSA", "BC");
                   
                sig.initVerify(gsPubKey);
                sig.update(nonce);
                if(sig.verify(signedNonce))
                    System.out.println("Nonce is verified");
                else {
                    System.out.println("Nonce not verified");
                    return false;
                }
                
                // Now get server's DH public key and create the shared AES key
                X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(servDHPubKeyBytes);
                KeyFactory kf = KeyFactory.getInstance("DH", "BC");
                PublicKey servDHPublicKey = kf.generatePublic(x509KeySpec);
                
                KeyAgreement ka = KeyAgreement.getInstance("DH", "BC");
                ka.init(clientDHPrivKey);
                ka.doPhase(servDHPublicKey, true);

                // Generate the secret key
                secretKey = ka.generateSecret("AES");
                System.out.println("SECRET KEY: " + Base64.encode(secretKey.getEncoded()));

                return true;
            }
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidKeySpecException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return false;
    }
}
