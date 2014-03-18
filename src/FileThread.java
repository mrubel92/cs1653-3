/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class FileThread extends Thread {

    private final Socket socket;
    private final FileServer my_fs;

    protected SecretKey secretKey;
    private IvParameterSpec ivSpec;

    public FileThread(Socket _socket, FileServer _fs) {
        socket = _socket;
        my_fs = _fs;
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Override
    public void run() {
        boolean proceed = true;
        try {
            // Announces connection and opens object streams
            System.out
                    .println("\n*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + " ***");
            final DataInputStream input = new DataInputStream(socket.getInputStream());
            final DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            do {
                Envelope message = Utils.readBytes(input, secretKey, ivSpec);

                System.out.println("\nMessage received from client: " + message.toString());
                Envelope response = null;
                List<String> usersFiles;
                UserToken yourToken;
                String remotePath;
                Token t;
                ShareFile sf;

                // Handler to list files that this user is allowed to see
                switch (message.getMessage()) {
                    case "DH":
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

                        Utils.sendBytes(output, response, secretKey);
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

                        Utils.sendBytes(output, response, secretKey);
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

                                        Utils.sendBytes(output, response, secretKey);
                                        message = Utils.readBytes(input, secretKey, ivSpec);

                                        while (message.getMessage().equals("CHUNK")) {
                                            fos.write((byte[]) message.getObjContents().get(0), 0, (Integer) message
                                                    .getObjContents().get(1));
                                            response = new Envelope("READY"); // Success

                                            Utils.sendBytes(output, response, secretKey);
                                            message = Utils.readBytes(input, secretKey, ivSpec);
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

                        Utils.sendBytes(output, response, secretKey);
                        System.out.println("UPLOADF response sent to client: " + response.toString());
                        break;
                    case "DOWNLOADF":
                        remotePath = (String) message.getObjContents().get(0);
                        t = (Token) message.getObjContents().get(1);
                        sf = my_fs.fileList.getFile("/" + remotePath);
                        if (sf == null) {
                            System.out.printf("Error: File %s doesn't exist\n", remotePath);
                            response = new Envelope("ERROR_FILEMISSING");

                            Utils.sendBytes(output, response, secretKey);
                        } else if (!t.getGroups().contains(sf.getGroup())) {
                            System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
                            response = new Envelope("ERROR_PERMISSION");

                            Utils.sendBytes(output, response, secretKey);
                        } else
                            try {
                                File f = new File("shared_files/_" + remotePath.replace('/', '_'));
                                if (!f.exists()) {
                                    System.out.printf("Error file %s missing from disk\n",
                                                      "_" + remotePath.replace('/', '_'));
                                    response = new Envelope("ERROR_NOTONDISK");

                                    Utils.sendBytes(output, response, secretKey);
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

                                            Utils.sendBytes(output, response, secretKey);
                                            message = Utils.readBytes(input, secretKey, ivSpec);
                                        } while (fis.available() > 0);
                                    }

                                    // If server indicates success, return the member list
                                    if (message.getMessage().equals("DOWNLOADF")) {
                                        response = new Envelope("EOF");

                                        Utils.sendBytes(output, response, secretKey);
                                        message = Utils.readBytes(input, secretKey, ivSpec);
                                        if (message.getMessage().equals("OK"))
                                            System.out.printf("File data upload successful\n");
                                        else
                                            System.out.printf("Upload failed: %s\n", message.getMessage());
                                    } else
                                        System.out.printf("Upload failed: %s\n", message.getMessage());
                                }
                            } catch (IOException e1) {
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
                        
                        Utils.sendBytes(output, response, secretKey);
                        System.out.println("DELETEF response sent to client: " + response.toString());
                        break;
                    case "DISCONNECT":
                        socket.close();
                        proceed = false;
                }
            } while (proceed);
        } catch (EOFException eof) {
            // Do nothing, the client connected to this thread is done talking
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
