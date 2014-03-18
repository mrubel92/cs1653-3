/* FileServer loads files from FileList.bin.  Stores files in shared_files directory. */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer extends Server {

    public static final int FILE_SERVER_PORT = 4321;
    protected FileList fileList;

    public FileServer() {
        super(FILE_SERVER_PORT, "FILE_SERVER");
    }

    public FileServer(int _port) {
        super(_port, "FILE_SERVER");
    }

    @Override
    public void start() {

        // This runs a thread that saves the lists on program exit
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new ShutDownListener(this));

        openFileListFile();

        // Autosave Daemon. Saves lists every 5 minutes
        AutoSave aSave = new AutoSave(this);
        aSave.setDaemon(true);
        aSave.start();

        try {
            @SuppressWarnings("resource")
            final ServerSocket serverSock = new ServerSocket(port);
            System.out.println("FILE SERVER RUNNING AT: " + serverSock.getLocalSocketAddress());

            Socket sock;
            Thread thread;

            while (true) {
                sock = serverSock.accept();
                thread = new FileThread(sock, this);
                thread.start();
            }
        } catch (IllegalArgumentException e1) {
            System.out.println(e1.getMessage());
        } catch (BindException e2) {
            System.out.println(e2.getMessage() + ". Port: " + port);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void openFileListFile() {
        String fileFile = "FileList.bin";
        ObjectInputStream fileStream;

        try {
            FileInputStream fis = new FileInputStream(fileFile);
            fileStream = new ObjectInputStream(fis);
            fileList = (FileList) fileStream.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("FileList Does Not Exist. Creating FileList...");
            fileList = new FileList();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading from FileList file");
            System.exit(-1);
        }

        File file = new File("shared_files");
        if (file.mkdir())
            System.out.println("Created new shared_files directory");
        else if (file.exists())
            System.out.println("Found shared_files directory");
        else
            System.out.println("Error creating shared_files directory");
    }
}
