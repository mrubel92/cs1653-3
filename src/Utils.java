
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.*;
import java.io.UnsupportedEncodingException;

public class Utils {

    static BigInteger prime;
    static BigInteger generator;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // https://svn.apache.org/repos/asf/directory/sandbox/erodriguez/kerberos-pkinit/src/main/java/org/apache/directory/server/kerberos/pkinit/DhGroup.java
        StringBuilder sb = new StringBuilder();
        sb.append("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1");
        sb.append("29024E088A67CC74020BBEA63B139B22514A08798E3404DD");
        sb.append("EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245");
        sb.append("E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED");
        sb.append("EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D");
        sb.append("C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F");
        sb.append("83655D23DCA3AD961C62F356208552BB9ED529077096966D");
        sb.append("670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B");
        sb.append("E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9");
        sb.append("DE2BCBF6955817183995497CEA956AE515D2261898FA0510");
        sb.append("15728E5A8AACAA68FFFFFFFFFFFFFFFF");

        prime = new BigInteger(sb.toString(), 16);
        generator = BigInteger.valueOf(2);
    }

    public static KeyPair genDHKeyPair() {
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("DH", "BC");
            DHParameterSpec dhSpec = new DHParameterSpec(prime, generator);
            kpg.initialize(dhSpec);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
     * Load Group Server's private key that was generated offline
     * http://codeartisan.blogspot.com/2009/05/public-key-cryptography-in-java.html
     *
     * @param filename
     * @return
     */
    public static PrivateKey getPrivKey(String filename) {
        try {
            File f = new File(filename);
            FileInputStream fis = new FileInputStream(f);
            byte[] keyBytes;
            try (DataInputStream dis = new DataInputStream(fis)) {
                keyBytes = new byte[(int) f.length()];
                dis.readFully(keyBytes);
            }

            PKCS8EncodedKeySpec spec
                    = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
            return kf.generatePrivate(spec);
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
     * Load Group Server's public key that was generated offline
     * http://codeartisan.blogspot.com/2009/05/public-key-cryptography-in-java.html
     *
     * @param filename
     * @return
     */
    public static PublicKey getPubKey(String filename) {
        FileInputStream fis = null;
        try {
            File f = new File(filename);
            fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int) f.length()];
            dis.readFully(keyBytes);
            dis.close();
            X509EncodedKeySpec spec
                    = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
            return kf.generatePublic(spec);
        } catch (FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
        return null;
    }

    public static byte[] serializeEnv(Object message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }

    public static byte[] encryptEnv(Envelope message, SecretKey secretKey, IvParameterSpec ivSpec) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            Envelope hmacEnv = appendHMac(message, secretKey, ivSpec);
            byte[] serializedEnv = serializeEnv(message);
            return cipher.doFinal(serializedEnv);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }

    public static Envelope appendHMac(Envelope envelope, SecretKey secretKey, IvParameterSpec ivSpec)
    {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        //return envelope with last object in it being the HMac value
        byte[] serializedEnv = serializeEnv(envelope);
        byte[] keyArray = secretKey.getEncoded();
        if(keyArray.length > 64)
        {
            //key is too long, we must hash it down to 64 bytes
            try {
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                MessageDigest digest = MessageDigest.getInstance("SHA-1", "BC");
                digest.reset();
                keyArray = digest.digest(keyArray);
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                System.err.println("Error: " + e.getMessage() + "\n\n" + e.toString());
                e.printStackTrace(System.err);
            }
        }
        else if(keyArray.length < 64)
        {
            byte[] keyTemp = keyArray.clone();
            keyArray = new byte[64];
            for(int i = 0; i < keyTemp.length; i++)
            {
                keyArray[i] = keyTemp[i];
            }
            for(int i = keyTemp.length; i < 64; i++)
            {
                keyArray[i] = 0x00;
            }
        }
        byte opad = 0x5a;
        byte ipad = 0x36;
        byte[] opadkey = new byte[64];
        byte[] ipadkey = new byte[64];
        for(int i = 0; i < opadkey.length; i++)
        {
            int temp = (int) keyArray[i] ^ (int) opad;
            opadkey[i] = (byte) temp;
        }
        for(int i = 0; i < ipadkey.length; i++)
        {
            int temp = keyArray[i] ^ ipad;
            ipadkey[i] = (byte) temp;
        }
        byte[] toHash = new byte[serializedEnv.length+128];
        byte[] hashed = null;
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            MessageDigest digest = MessageDigest.getInstance("SHA-1", "BC");
            digest.reset();
            hashed = digest.digest(toHash);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage() + "\n\n" + e.toString());
            e.printStackTrace(System.err);
        }
        envelope.addHmac(hashed);
        return envelope;
    }

    public static Envelope verifyHMac(Envelope envelope, SecretKey secretKey, IvParameterSpec ivSpec)
    {
        //This method should return the Envelope without the hmac if it is correct and null if it is incorrect
        byte[] envHmac = envelope.rmHmac();

        byte[] serializedEnv = serializeEnv(envelope);
        byte[] keyArray = secretKey.getEncoded();
        if(keyArray.length > 64)
        {
            //key is too long, we must hash it down to 64 bytes
            try {
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                MessageDigest digest = MessageDigest.getInstance("SHA-1", "BC");
                digest.reset();
                keyArray = digest.digest(keyArray);
            } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                System.err.println("Error: " + e.getMessage() + "\n\n" + e.toString());
                e.printStackTrace(System.err);
            }
        }
        else if(keyArray.length < 64)
        {
            byte[] keyTemp = keyArray.clone();
            keyArray = new byte[64];
            for(int i = 0; i < keyTemp.length; i++)
            {
                keyArray[i] = keyTemp[i];
            }
            for(int i = keyTemp.length; i < 64; i++)
            {
                keyArray[i] = 0x00;
            }
        }
        byte opad = 0x5a;
        byte ipad = 0x36;
        byte[] opadkey = new byte[64];
        byte[] ipadkey = new byte[64];
        for(int i = 0; i < opadkey.length; i++)
        {
            int temp = (int) keyArray[i] ^ (int) opad;
            opadkey[i] = (byte) temp;
        }
        for(int i = 0; i < ipadkey.length; i++)
        {
            int temp = keyArray[i] ^ ipad;
            ipadkey[i] = (byte) temp;
        }
        byte[] toHash = new byte[serializedEnv.length+128];
        byte[] hashed = null;
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            MessageDigest digest = MessageDigest.getInstance("SHA-1", "BC");
            digest.reset();
            hashed = digest.digest(toHash);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage() + "\n\n" + e.toString());
            e.printStackTrace(System.err);
        }

        //check to see if the HMAC on the message equals the one caluclated here
        if(Arrays.equals(hashed,envHmac))
        {
            return envelope;
        }
        else
        {
            System.out.println("HMACs did not match!!!!!!");
            return null;
        }


    }

    public static Envelope decryptEnv(byte[] bytes, SecretKey secretKey, IvParameterSpec ivSpec) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decrypted = cipher.doFinal(bytes);
            Envelope hmacMessage = (Envelope) deserializeEnv(decrypted);
            return verifyHMac(hmacMessage, secretKey, ivSpec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }

    public static Object deserializeEnv(byte[] bytes) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }
}
