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
import java.util.Scanner;

public class GroupServer extends Server {

    public static final int GROUP_SERVER_PORT = 8765;
    public static final int MAX_INPUT_LENGTH = 16;
    public UserList userList;
    public GroupList groupList;
    public String firstUser;

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
            System.out.println("UserList File Does Not Exist. Creating UserList...");
            System.out.println("No users currently exist. Your account will be the administrator.");
            System.out.print("Enter your username (max length = 16):\n");
            String username = askForValidUsername();

            // Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
            userList = new UserList();
            userList.addUser(username, "PASSWORD");
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
     * Asks user for a valid username.
     *
     * @return a valid username
     */
    private String askForValidUsername() {
        String username;
        try (Scanner console = new Scanner(System.in)) {
            username = "";
            while (!console.hasNext("[\\w]+")) {
                System.out
                        .println("\nUsername can only have letters, numbers, and underscores.\nEnter your username (max length = 16):");
                console.next();
            }
            username = console.next().toUpperCase();
        }

        if (username.length() > MAX_INPUT_LENGTH)
            return username.substring(0, MAX_INPUT_LENGTH);
        else
            return username;
    }
}
