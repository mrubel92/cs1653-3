/* Group server. Server loads the users from UserList.bin.
 * If user list does not exists, it creates a new list and makes the user the server administrator.
 * On exit, the server saves the user list to file. 
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;

public class GroupServer extends Server {

    public static final int GROUP_SERVER_PORT = 8765;
    private static final int MAX_INPUT_LENGTH = 16;
    protected UserList userList;
    protected GroupList groupList;
    private String firstUser;
    protected static PrivateKey gsPrivKey;
    protected static PublicKey gsPubKey;

    static {
        gsPrivKey = Utils.getPrivKey("gs_private_key.der");
        gsPubKey = Utils.getPubKey("gs_public_key.der");
    }

    public GroupServer() {
        super(GROUP_SERVER_PORT, "GROUP_SERVER");
    }

    public GroupServer(int _port) {
        super(_port, "GROUP_SERVER");
    }

    @Override
    public void start() {
	// Overwrote server.start() because if no user file exists, initial admin account needs to be created

        // This runs a thread that saves the lists on program exit
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new ShutDownListener(this));

        // Try and open user and group file
        openUserFile();
        openGroupFile();

        // Autosave Daemon. Saves lists every 5 minutes
        AutoSave aSave = new AutoSave(this);
        aSave.setDaemon(true);
        aSave.start();

        // This block listens for connections and creates threads on new connections
        try {
            @SuppressWarnings("resource")
            final ServerSocket serverSock = new ServerSocket(port);
            System.out.println("GROUP SERVER RUNNING AT: " + serverSock.getLocalSocketAddress());

            Socket sock;
            GroupThread thread;

            while (true) {
                sock = serverSock.accept();
                thread = new GroupThread(sock, this);
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

    private void openUserFile() {
        String userFile = "UserList.bin";
        ObjectInputStream userStream;

        try {
            FileInputStream fis = new FileInputStream(userFile);
            userStream = new ObjectInputStream(fis);
            userList = (UserList) userStream.readObject();
        } catch (FileNotFoundException e) {
            String username;
            String password;
            try (Scanner console = new Scanner(System.in)) {
                System.out.println("UserList File Does Not Exist. Creating UserList...");
                System.out.println("No users currently exist. Your account will be the administrator.");
                System.out.print("Enter your username (min length = 3, max length = 16):\n");
                username = askForValidInput("\\w{3,16}", true, console);
                System.out.print("Enter your password (min length = 6, max length = 16):\n");
                password = askForValidInput("[ -~]{6,16}", false, console);
            }

            // Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
            userList = new UserList();
            userList.addUser(username, password);
            userList.addGroup(username, "ADMIN");
            userList.addOwnership(username, "ADMIN");

            firstUser = username;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading from UserList file");
            System.exit(-1);
        }
    }

    private void openGroupFile() {
        String groupFile = "GroupList.bin";
        ObjectInputStream groupStream;

        try {
            FileInputStream fis = new FileInputStream(groupFile);
            groupStream = new ObjectInputStream(fis);
            groupList = (GroupList) groupStream.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("\nGroupList File Does Not Exist. Creating GroupList...");
            System.out.println("No groups currently exist. Your account will be in the ADMIN group.");

            // Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
            groupList = new GroupList();
            groupList.addGroup("ADMIN");
            groupList.addMember("ADMIN", firstUser);
            groupList.addOwnership("ADMIN", firstUser);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading from GroupList file");
            System.exit(-1);
        }
    }

    /**
     * Asks user for a valid password.
     *
     * @return a valid password
     */
    private String askForValidInput(String regex, boolean username, Scanner console) {
        String input;
        input = "";
        while (!console.hasNext(regex)) {
            System.out
                    .println("Bad input format, try again.\n(minimum username length = 3, minimum password length = 6, max length = 16):");
            console.next();
        }
        if (username)
            input = console.next().toUpperCase();
        else
            input = console.next();

        if (input.length() > MAX_INPUT_LENGTH)
            return input.substring(0, MAX_INPUT_LENGTH);
        else
            return input;
    }
}
