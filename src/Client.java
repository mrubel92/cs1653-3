
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
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
import javax.crypto.spec.IvParameterSpec;
import javax.swing.JOptionPane;

public abstract class Client {

    /**
     * Subclasses have access to Socket and input/output streams *
     */
    protected Socket sock;
    protected ObjectInputStream input;
    protected ObjectOutputStream output;

    private final String GROUP = "GROUP";
    private final String FILE = "FILE";

    protected SecretKey gsSecretKey;
    protected SecretKey fsSecretKey;
    protected static PublicKey gsPubKey;
    protected static PublicKey fsPubKey;
    protected IvParameterSpec ivSpec;

    protected FileServerList fileServersList;

    static {
        gsPubKey = Utils.getPubKey("gs_public_key.der");
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public boolean connect(final String server, final int port, String serverName) {
        System.out.println("\nAttempting to connect to: " + server + ":" + port);
        try {
            @SuppressWarnings("resource")
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(server, port), 10000); // 10 second timeout
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            if (serverName.equals("FILE")) {
                openFileServerList();
                if (fileServersList.checkIP(server))
                    fsPubKey = fileServersList.getPublicKey(server);
                else if (!askForPublicKey(server))
                    return false;
            }

            return diffieHellman(serverName);
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
                output.reset();
                output.writeObject(message);

                input.close();
                output.close();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
    }

    private boolean askForPublicKey(String server) {
        try {
            Envelope message, response;

            message = new Envelope("FINGERPRINT");
            System.out.println("\nFINGERPRINT message sent to Group Server: " + message.toString());
            Envelope tempMessage = new Envelope("NOT ENCRYPTED");
            tempMessage.addObject(message);
            output.reset();
            output.writeObject(tempMessage);

            response = (Envelope) input.readObject();
            if (response.getMessage().equals("FINGERPRINT"))
                fsPubKey = (PublicKey) response.getObjContents().get(0);

            MessageDigest sha1 = MessageDigest.getInstance("SHA1", "BC");
            byte[] digest = sha1.digest(fsPubKey.getEncoded());
            BigInteger bi = new BigInteger(1, digest);
            String fingerprint = bi.toString(16);
            while (fingerprint.length() < 32) {
                fingerprint = "0" + fingerprint;
            }

            if (fileServersList.checkFingerprint(fingerprint)) {
                int reply = JOptionPane.showConfirmDialog(null, "WARNING!\nTHE FINGERPRINT RECEIVED HAS ALREADY BEEN REGISTERED WITH ANOTHER FILE SERVER!\nDO YOU STILL WANT TO CONNECT?", "Do you accept this fingerprint?", JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.NO_OPTION)
                    return false;
            } else {
                int reply = JOptionPane.showConfirmDialog(null, fingerprint, "Do you accept this fingerprint?", JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.NO_OPTION)
                    return false;
            }
            
            fileServersList.addFileServer(server, fingerprint, fsPubKey);

            // Save FileServerList file
            ObjectOutputStream outStreamFiles = new ObjectOutputStream(new FileOutputStream("FileServerList.bin"));
            outStreamFiles.writeObject(fileServersList);

            System.out.println("Message received from Group Server: " + response.toString());
            return true;
        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return true;
    }

    private void openFileServerList() {
        String fileFile = "FileServerList.bin";
        ObjectInputStream fileStream;

        try {
            FileInputStream fis = new FileInputStream(fileFile);
            fileStream = new ObjectInputStream(fis);
            fileServersList = (FileServerList) fileStream.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("FileServerList Does Not Exist. Creating FileServerList...");
            fileServersList = new FileServerList();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading from FileServerList file");
            System.exit(-1);
        }
    }

    // http://exampledepot.8waytrips.com/egs/javax.crypto/KeyAgree.html
    private boolean diffieHellman(String serverName) {
        int nonceLength = 16;
        byte[] nonce;

        // Nonce
        SecureRandom sr = new SecureRandom();
        nonce = new byte[nonceLength];
        sr.nextBytes(nonce);
        ivSpec = new IvParameterSpec(nonce);

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
            Envelope tempMessage = new Envelope("NOT ENCRYPTED");
            tempMessage.addObject(message);
            output.reset();
            output.writeObject(tempMessage);

            // Get the response from the server
            response = (Envelope) input.readObject();
            System.out.println("Message received from Group Server: " + response.toString());

            // Successful response
            if (response.getMessage().equals("DH")) {
                byte[] signedNonce = (byte[]) response.getObjContents().get(0);
                byte[] servDHPubKeyBytes = (byte[]) response.getObjContents().get(1);

                // Verify nonce first
                Signature sig = Signature.getInstance("SHA1withRSA", "BC");

                if (serverName.equals(GROUP))
                    sig.initVerify(gsPubKey);
                if (serverName.equals(FILE))
                    sig.initVerify(fsPubKey);

                sig.update(nonce);
                if (sig.verify(signedNonce))
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
                if (serverName.equals(GROUP))
                    gsSecretKey = ka.generateSecret("AES");
                if (serverName.equals(FILE))
                    fsSecretKey = ka.generateSecret("AES");

                return true;
            }
        } catch (IllegalStateException | NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (NoSuchProviderException | SignatureException | InvalidKeySpecException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return false;
    }

}
