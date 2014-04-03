/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class FileThread extends Thread {

    private final Socket socket;
    private final FileServer my_fs;

    protected SecretKey secretKey;
    private IvParameterSpec ivSpec;
    private int messageCounter;

    public FileThread(Socket _socket, FileServer _fs) {
        socket = _socket;
        my_fs = _fs;
        messageCounter = 0;
    }

    @Override
    public void run() {
        boolean verifiedToken = false;
        boolean proceed = true;
        try {
            // Announces connection and opens object streams
            System.out.println("\n*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + " ***");
            final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            messageCounter = 0;

            do {
                Envelope tempMessage = (Envelope) input.readObject();
                Envelope message = null;
                if (tempMessage.getMessage().equals("ENCRYPTED"))
                {
                    message = Utils.decryptEnv((byte[]) tempMessage.getObjContents().get(0), secretKey, ivSpec);
                    int numberIndex = message.getObjContents().size() - 1; //gives us the index of the number appended to the message
                    int seqNumber =(Integer) message.getObjContents().get(numberIndex);
                    messageCounter++;
                    if(seqNumber != messageCounter)
                    {
                        //cease communication
                        System.out.println("Possible Replay or Reorder Attack");
                        System.exit(0);
                    }

                }
                else
                    message = (Envelope) tempMessage.getObjContents().get(0);

                System.out.println("\nMessage received from client: " + message.toString());
                Envelope response = null;
                Envelope tempResponse;
                List<String> usersFiles;
                UserToken yourToken;
                byte[] signedToken;
                String remotePath;
                Token t;
                ShareFile sf;

                String msg = message.getMessage();
                if (!msg.equals("VERIFY") && !verifiedToken && !msg.equals("DH") && !msg.equals("FINGERPRINT"))
                    message = new Envelope("DISCONNECT");

                // Handler to list files that this user is allowed to see
                switch (message.getMessage()) {
                    case "VERIFY":
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL-BADCONTENTS");
                        else if (message.getObjContents().get(0) == null)
                            response = new Envelope("FAIL-BADTOKEN");
                        else if (message.getObjContents().get(1) == null)
                            response = new Envelope("FAIL-BADSIGNATURE");
                        else {
                            yourToken = (UserToken) message.getObjContents().get(0);
                            signedToken = (byte[]) message.getObjContents().get(1);
                            if (verifyToken(signedToken, yourToken)) {
                                response = new Envelope("VERIFIED");
                                verifiedToken = true;
                            } else
                                response = new Envelope("NOTVERIFIED");
                        }
                        tempResponse = new Envelope("ENCRYPTED");
                        messageCounter++;
                        response.addObject(messageCounter);
                        tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));

                        output.reset();
                        output.writeObject(tempResponse);
                        System.out.println("VERIFY response sent to client: " + response.toString());
                        break;
                    case "FINGERPRINT":
                        response = new Envelope("FINGERPRINT");
                        response.addObject(FileServer.fsPubKey);

                        output.reset();
                        output.writeObject(response);
                        System.out.println("FINGERPRINT response sent to client: " + response.toString());
                        break;
                    case "DH":
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL");
                        else {
                            response = new Envelope("FAIL");
                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null) {
                                    byte[] nonce = (byte[]) message.getObjContents().get(0);
                                    byte[] clientPubKeyBytes = (byte[]) message.getObjContents().get(1);
                                    if (nonce != null && clientPubKeyBytes != null) {
                                        ivSpec = new IvParameterSpec(nonce);
                                        response = diffieHellman(nonce, clientPubKeyBytes);
                                    }
                                }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("DH response sent to client: " + response.toString());
                        break;
                    case "LFILES":
                        if (message.getObjContents().size() < 1)
                            response = new Envelope("FAIL-BADCONTENTS");
                        else if (message.getObjContents().get(0) == null)
                            response = new Envelope("FAIL-BADTOKEN");

                        else {
                            usersFiles = new ArrayList<>();
                            yourToken = (UserToken) message.getObjContents().get(0);

                            // Get list of all files
                            ArrayList<ShareFile> allFiles = my_fs.fileList.getFiles();
                            // Look through all files to see which are in the appropriate group
                            for (ShareFile currFile : allFiles) {
                                // Determine if usertoken is associated with the group that the file is in
                                if (yourToken.getGroups().contains(currFile.getGroup()))
                                    usersFiles.add(currFile.getPath().substring(1));
                            }
                            response = new Envelope("OK");
                            response.addObject(usersFiles);
                        }

                        tempResponse = new Envelope("ENCRYPTED");
                        messageCounter++;
                        response.addObject(messageCounter);
                        tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                        output.reset();
                        output.writeObject(tempResponse);
                        System.out.println("LFILES response sent to client: " + response.toString());
                        break;
                    case "LGFILES":
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL-BADCONTENTS");
                        else {
                            if (message.getObjContents().get(0) == null)
                                response = new Envelope("FAIL-BADTOKEN");
                            if (message.getObjContents().get(1) == null)
                                response = new Envelope("FAIL-BADGROUP");
                            else {
                                usersFiles = new ArrayList<>();
                                yourToken = (UserToken) message.getObjContents().get(0);
                                String group = (String) message.getObjContents().get(1);

                                if (yourToken.getGroups().contains(group)) {
                                    // Get list of all files
                                    ArrayList<ShareFile> allFiles = my_fs.fileList.getFiles();
                                    // Look through all files to see which are in the appropriate group
                                    for (ShareFile currFile : allFiles) {
                                        // Determine if usertoken is associated with the group that the file is in
                                        if (currFile.getGroup().equals(group))
                                            usersFiles.add(currFile.getPath().substring(1));
                                    }
                                    response = new Envelope("OK");
                                    response.addObject(usersFiles);
                                } else {
                                    response = new Envelope("FAIL");
                                    response.addObject(null);
                                }
                            }
                        }

                        tempResponse = new Envelope("ENCRYPTED");
                        messageCounter++;
                        response.addObject(messageCounter);
                        tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                        output.reset();
                        output.writeObject(tempResponse);
                        System.out.println("LGFILES response sent to client: " + response.toString());
                        break;
                    case "UPLOADF":
                        if (message.getObjContents().size() < 3)
                            response = new Envelope("FAIL-BADCONTENTS");
                        else {
                            if (message.getObjContents().get(0) == null)
                                response = new Envelope("FAIL-BADPATH");
                            if (message.getObjContents().get(1) == null)
                                response = new Envelope("FAIL-BADGROUP");
                            if (message.getObjContents().get(2) == null)
                                response = new Envelope("FAIL-BADTOKEN");
                            else {
                                remotePath = (String) message.getObjContents().get(0);
                                String group = (String) message.getObjContents().get(1);
                                yourToken = (UserToken) message.getObjContents().get(2); // Extract token

                                if (my_fs.fileList.checkFile(remotePath)) {
                                    System.out.printf("Error: file already exists at %s\n", remotePath);
                                    response = new Envelope("FAIL-FILEEXISTS"); // Success
                                } else if (!yourToken.getGroups().contains(group)) {
                                    System.out.printf("Error: user missing valid token for group %s\n", group);
                                    response = new Envelope("FAIL-UNAUTHORIZED"); // Success
                                } else {
                                    File file = new File("shared_files/" + remotePath.replace('/', '_'));
                                    file.createNewFile();
                                    try (FileOutputStream fos = new FileOutputStream(file)) {
                                        System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

                                        response = new Envelope("READY"); // Success
                                        tempResponse = new Envelope("ENCRYPTED");
                                        messageCounter++;
                                        response.addObject(messageCounter);
                                        tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                                        output.reset();
                                        output.writeObject(tempResponse);

                                        tempMessage = (Envelope) input.readObject();
                                        if (tempMessage.getMessage().equals("ENCRYPTED"))
                                        {
                                            message = Utils.decryptEnv((byte[]) tempMessage.getObjContents().get(0), secretKey, ivSpec);
                                            int numberIndex = message.getObjContents().size() - 1; //gives us the index of the number appended to the message
                                            int seqNumber =(Integer) message.getObjContents().get(numberIndex);
                                            messageCounter++;
                                            if(seqNumber != messageCounter)
                                            {
                                                //cease communication
                                                System.out.println("Possible Replay or Reorder Attack2");
                                                System.exit(0);
                                            }
                                        }
                                            
                                        else
                                            message = (Envelope) tempMessage.getObjContents().get(0);
                                        while (message.getMessage().equals("CHUNK")) {
                                            fos.write((byte[]) message.getObjContents().get(0), 0, (Integer) message
                                                    .getObjContents().get(1));
                                            response = new Envelope("READY"); // Success
                                            tempResponse = new Envelope("ENCRYPTED");
                                            messageCounter++;
                                            response.addObject(messageCounter);
                                            tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                                            output.reset();
                                            output.writeObject(tempResponse);

                                            tempMessage = (Envelope) input.readObject();
                                            if (tempMessage.getMessage().equals("ENCRYPTED"))
                                            {
                                                message = Utils.decryptEnv((byte[]) tempMessage.getObjContents().get(0), secretKey, ivSpec);
                                                int numberIndex = message.getObjContents().size() - 1; //gives us the index of the number appended to the message
                                                int seqNumber =(Integer) message.getObjContents().get(numberIndex);
                                                messageCounter++;
                                                if(seqNumber != messageCounter)
                                                {
                                                    //cease communication
                                                    System.out.println("Possible Replay or Reorder Attack3");
                                                    System.exit(0);
                                                }
                                            }
                                                
                                            else
                                                message = (Envelope) tempMessage.getObjContents().get(0);
                                        }

                                        if (message.getMessage().equals("EOF")) {
                                            System.out.printf("Transfer successful file %s\n", remotePath);
                                            my_fs.fileList.addFile(yourToken.getSubject(), group, remotePath);
                                            response = new Envelope("OK"); // Success
                                        } else {
                                            System.out.printf("Error reading file %s from client\n", remotePath);
                                            response = new Envelope("ERROR-TRANSFER"); // Success
                                        }
                                    }
                                }
                            }
                        }

                        messageCounter++;
                        response.addObject(messageCounter);
                        tempResponse = new Envelope("ENCRYPTED");
                        tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                        output.reset();
                        output.writeObject(tempResponse);
                        System.out.println("UPLOADF response sent to client: " + response.toString());
                        break;
                    case "DOWNLOADF":
                        remotePath = (String) message.getObjContents().get(0);
                        t = (Token) message.getObjContents().get(1);
                        sf = my_fs.fileList.getFile("/" + remotePath);
                        if (sf == null) {
                            System.out.printf("Error: File %s doesn't exist\n", remotePath);
                            response = new Envelope("ERROR_FILEMISSING");
                            messageCounter++;
                            response.addObject(messageCounter);
                            tempResponse = new Envelope("ENCRYPTED");
                            tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                            output.reset();
                            output.writeObject(tempResponse);
                        } else if (!t.getGroups().contains(sf.getGroup())) {
                            System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
                            response = new Envelope("ERROR_PERMISSION");

                            tempResponse = new Envelope("ENCRYPTED");
                            messageCounter++;
                            response.addObject(messageCounter);
                            tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                            output.reset();
                            output.writeObject(tempResponse);
                        } else
                            try {
                                File f = new File("shared_files/_" + remotePath.replace('/', '_'));
                                if (!f.exists()) {
                                    System.out.printf("Error file %s missing from disk\n",
                                                      "_" + remotePath.replace('/', '_'));
                                    response = new Envelope("ERROR_NOTONDISK");
                                    messageCounter++;
                                    response.addObject(messageCounter);
                                    tempResponse = new Envelope("ENCRYPTED");
                                    tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                                    output.reset();
                                    output.writeObject(tempResponse);
                                } else {
                                    try (FileInputStream fis = new FileInputStream(f)) {
                                        do {
                                            byte[] buf = new byte[4096];
                                            if (message.getMessage().compareTo("DOWNLOADF") != 0) {
                                                System.out.printf("Server error: %s\n", message.getMessage());
                                                break;
                                            }
                                            response = new Envelope("CHUNK");
                                            int n = fis.read(buf); // can throw an IOException
                                            if (n > 0)
                                                System.out.printf(".");
                                            else if (n < 0)
                                                System.out.println("Read error");

                                            response.addObject(buf);
                                            response.addObject(new Integer(n));
                                            messageCounter++;
                                            response.addObject(messageCounter);
                                            tempResponse = new Envelope("ENCRYPTED");
                                            tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                                            output.reset();
                                            output.writeObject(tempResponse);

                                            tempMessage = (Envelope) input.readObject();
                                            if (tempMessage.getMessage().equals("ENCRYPTED"))
                                            {
                                                message = Utils.decryptEnv((byte[]) tempMessage.getObjContents().get(0), secretKey, ivSpec);
                                                int numberIndex = message.getObjContents().size() - 1; //gives us the index of the number appended to the message
                                                int seqNumber = (Integer) message.getObjContents().get(numberIndex);
                                                messageCounter++;
                                                if(seqNumber != messageCounter)
                                                {
                                                    //cease communication
                                                    System.out.println("Possible Replay or Reorder Attack4");
                                                    System.exit(0);
                                                }
                                            }
                                            else
                                                message = (Envelope) tempMessage.getObjContents().get(0);

                                        } while (fis.available() > 0);
                                    }

                                    // If server indicates success, return the member list
                                    if (message.getMessage().equals("DOWNLOADF")) {
                                        response = new Envelope("EOF");
                                        tempResponse = new Envelope("ENCRYPTED");
                                        messageCounter++;
                                        response.addObject(messageCounter);
                                        tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                                        output.reset();
                                        output.writeObject(tempResponse);

                                        tempMessage = (Envelope) input.readObject();
                                        if (tempMessage.getMessage().equals("ENCRYPTED"))
                                        {
                                            message = Utils.decryptEnv((byte[]) tempMessage.getObjContents().get(0), secretKey, ivSpec);
                                            int numberIndex = message.getObjContents().size() - 1; //gives us the index of the number appended to the message
                                            int seqNumber = (Integer) message.getObjContents().get(numberIndex);
                                            messageCounter++;
                                            if(seqNumber != messageCounter)
                                            {
                                                //cease communication
                                                System.out.println("Possible Replay or Reorder Attack5");
                                                System.exit(0);
                                            }
                                        }
                                        else
                                            message = (Envelope) tempMessage.getObjContents().get(0);
                                        if (message.getMessage().equals("OK"))
                                            System.out.printf("File data upload successful\n");
                                        else
                                            System.out.printf("Upload failed: %s\n", message.getMessage());
                                    } else
                                        System.out.printf("Upload failed: %s\n", message.getMessage());
                                }
                            } catch (IOException | ClassNotFoundException e1) {
                                System.err.println("Error: " + message.getMessage());
                                e1.printStackTrace(System.err);
                            }
                        if (response != null)
                            System.out.println("DOWNLOADF response sent to client: " + response.toString());
                        break;
                    case "DELETEF":
                        remotePath = (String) message.getObjContents().get(0);
                        t = (Token) message.getObjContents().get(1);
                        sf = my_fs.fileList.getFile("/" + remotePath);
                        if (sf == null) {
                            System.out.printf("Error: File %s doesn't exist\n", remotePath);
                            response = new Envelope("ERROR_DOESNTEXIST");
                        } else if (!t.getGroups().contains(sf.getGroup())) {
                            System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
                            response = new Envelope("ERROR_PERMISSION");
                        } else
                            try {
                                File f = new File("shared_files/" + "_" + remotePath.replace('/', '_'));

                                if (!f.exists()) {
                                    System.out.printf("Error file %s missing from disk\n",
                                                      "_" + remotePath.replace('/', '_'));
                                    response = new Envelope("ERROR_FILEMISSING");
                                } else if (f.delete()) {
                                    System.out.printf("File %s deleted from disk\n", "_" + remotePath.replace('/', '_'));
                                    my_fs.fileList.removeFile("/" + remotePath);
                                    response = new Envelope("OK");
                                } else {
                                    System.out.printf("Error deleting file %s from disk\n",
                                                      "_" + remotePath.replace('/', '_'));
                                    response = new Envelope("ERROR_DELETE");
                                }
                            } catch (Exception e1) {
                                System.err.println("Error: " + e1.getMessage());
                                e1.printStackTrace(System.err);
                                response = new Envelope(e1.getMessage());
                            }

                        messageCounter++;
                        response.addObject(messageCounter);
                        tempResponse = new Envelope("ENCRYPTED");
                        tempResponse.addObject(Utils.encryptEnv(response, secretKey, ivSpec));
                        output.reset();
                        output.writeObject(tempResponse);
                        System.out.println("DELETEF response sent to client: " + response.toString());
                        break;
                    case "DISCONNECT":
                        socket.close();
                        proceed = false;
                }

            } while (proceed);
        } catch (EOFException eof) {
            // Do nothing, the client connected to this thread is done talking
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private boolean verifyToken(byte[] signedToken, UserToken token) {
        try {
            // Verify nonce first
            Signature sig = Signature.getInstance("SHA1withRSA", "BC");
            sig.initVerify(my_fs.gsPubKey);

            sig.update(Utils.serializeEnv(token));
            return sig.verify(signedToken);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return false;
    }

// http://exampledepot.8waytrips.com/egs/javax.crypto/KeyAgree.html
    private Envelope diffieHellman(byte[] nonce, byte[] clientPubKeyBytes) {
        Envelope response;
        try {
            response = new Envelope("DH");

            // Server's pub and priv DH key pair
            KeyPair dhKP = Utils.genDHKeyPair();
            PrivateKey servDHPrivKey = dhKP.getPrivate();
            PublicKey servDHPubKey = dhKP.getPublic();

            // Convert the client's DH public key bytes into a PublicKey object
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyBytes);
            KeyFactory keyFact = KeyFactory.getInstance("DH", "BC");
            PublicKey clientPubKey = keyFact.generatePublic(x509KeySpec);

            // Prepare to generate the AES secret key with the server's DH private key and client's DH public key
            KeyAgreement ka = KeyAgreement.getInstance("DH", "BC");
            ka.init(servDHPrivKey);
            ka.doPhase(clientPubKey, true);

            // Generate the secret key
            secretKey = ka.generateSecret("AES");

            // Send pub key and nonce back to client
            Signature sig = Signature.getInstance("SHA1withRSA", "BC");
            sig.initSign(FileServer.fsPrivKey);
            sig.update(nonce);
            byte[] signedNonce = sig.sign();

            response.addObject(signedNonce);
            response.addObject(servDHPubKey.getEncoded());
            return response;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return null;
    }
}
