/* FileClient provides all the client functionality regarding the file server */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class FileClient extends Client implements FileClientInterface {

    @Override
    public boolean delete(String filename, UserToken token) {
        String remotePath;
        if (filename.charAt(0) == '/')
            remotePath = filename.substring(1);
        else
            remotePath = filename;

        Envelope message, response;
        message = new Envelope("DELETEF"); // Success
        message.addObject(remotePath);
        message.addObject(token);

        Utils.sendBytes(output, message, fsSecretKey);
        response = Utils.readBytes(input, fsSecretKey, ivSpec);

        if (response.getMessage().equals("OK"))
            System.out.printf("File %s deleted successfully\n", filename);
        else {
            System.out.printf("Error deleting file %s (%s)\n", filename, response.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean download(String sourceFile, String destFile, UserToken token) {
        String tempSourceFile = sourceFile;
        if (tempSourceFile.charAt(0) == '/')
            tempSourceFile = tempSourceFile.substring(1);

        File file = new File(tempSourceFile);
        try {
            Envelope message, response;

            if (!file.exists()) {
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);

                message = new Envelope("DOWNLOADF"); // Success
                message.addObject(tempSourceFile);
                message.addObject(token);

                Utils.sendBytes(output, message, fsSecretKey);
                response = Utils.readBytes(input, fsSecretKey, ivSpec);

                while (response.getMessage().equals("CHUNK")) {
                    fos.write((byte[]) response.getObjContents().get(0), 0, (Integer) response.getObjContents().get(1));
                    System.out.printf(".");
                    message = new Envelope("DOWNLOADF"); // Success

                    Utils.sendBytes(output, message, fsSecretKey);
                    response = Utils.readBytes(input, fsSecretKey, ivSpec);
                }
                fos.close();

                if (response.getMessage().equals("EOF")) {
                    fos.close();
                    System.out.printf("\nTransfer successful file %s\n", tempSourceFile);
                    message = new Envelope("OK"); // Success

                    Utils.sendBytes(output, message, fsSecretKey);
                } else {
                    System.out.printf("Error reading file %s (%s)\n", tempSourceFile, response.getMessage());
                    file.delete();
                    return false;
                }
            } else {
                System.out.printf("Error couldn't create file %s\n", destFile);
                return false;
            }
        } catch (IOException e1) {
            System.out.printf("Error couldn't create file %s\n", destFile);
            return false;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "unchecked"})
    @Override
    public List<String> listFiles(UserToken token) {
        Envelope message, response;
        // Tell the server to return the member list
        message = new Envelope("LFILES");
        message.addObject(token); // Add requester's token

        Utils.sendBytes(output, message, fsSecretKey);
        response = Utils.readBytes(input, fsSecretKey, ivSpec);

        // If server indicates success, return the member list
        if (response.getMessage().equals("OK"))
            return (List<String>) response.getObjContents().get(0);
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> listGroupFiles(UserToken token, String group) {
        Envelope message, response;
        // Tell the server to return the member list
        message = new Envelope("LGFILES");
        message.addObject(token); // Add requester's token
        message.addObject(group);

        Utils.sendBytes(output, message, fsSecretKey);
        response = Utils.readBytes(input, fsSecretKey, ivSpec);

        // If server indicates success, return the member list
        if (response.getMessage().equals("OK"))
            return (List<String>) response.getObjContents().get(0);
        return null;
    }

    @Override
    public boolean upload(String sourceFile, String destFile, String group, UserToken token) {
        String tempDestFile = destFile;
        if (tempDestFile.charAt(0) != '/')
            tempDestFile = "/" + tempDestFile;

        try {
            Envelope message, response;
            message = new Envelope("UPLOADF");
            message.addObject(tempDestFile);
            message.addObject(group);
            message.addObject(token); // Add requester's token

            Utils.sendBytes(output, message, fsSecretKey);
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                response = Utils.readBytes(input, fsSecretKey, ivSpec);

                if (response.getMessage().equals("READY"))
                    System.out.printf("Meta data upload successful\n");
                else {
                    System.out.printf("Upload failed: %s\n", response.getMessage());
                    fis.close();
                    return false;
                }

                do {
                    byte[] buf = new byte[4096];
                    if (response.getMessage().compareTo("READY") != 0) {
                        System.out.printf("Server error: %s\n", response.getMessage());
                        fis.close();
                        return false;
                    }
                    message = new Envelope("CHUNK");
                    int n = fis.read(buf); // can throw an IOException
                    if (n > 0)
                        System.out.printf(".");
                    else if (n < 0) {
                        System.out.println("Read error");
                        fis.close();
                        return false;
                    }

                    message.addObject(buf);
                    message.addObject(new Integer(n));

                    Utils.sendBytes(output, message, fsSecretKey);
                    response = Utils.readBytes(input, fsSecretKey, ivSpec);

                } while (fis.available() > 0);
            }

            if (response.getMessage().equals("READY")) {
                message = new Envelope("EOF");

                Utils.sendBytes(output, message, fsSecretKey);
                response = Utils.readBytes(input, fsSecretKey, ivSpec);

                if (response.getMessage().equals("OK"))
                    System.out.printf("\nFile data upload successful\n");
                else {
                    System.out.printf("\nUpload failed: %s\n", response.getMessage());
                    return false;
                }
            } else {
                System.out.printf("Upload failed: %s\n", response.getMessage());
                return false;
            }
        } catch (IOException e1) {
            System.err.println("Error: " + e1.getMessage());
            e1.printStackTrace(System.err);
            return false;
        }
        return true;
    }
}
