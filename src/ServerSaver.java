import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class ServerSaver {
	Server my_server;

	public ServerSaver(Server _server) {
		my_server = _server;
	}

	public void run() {
		ObjectOutputStream outStreamUsers;
		ObjectOutputStream outStreamGroups;
		ObjectOutputStream outStreamFiles;
		try {
			if (my_server instanceof GroupServer) {
				outStreamUsers = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
				outStreamUsers.writeObject(((GroupServer) my_server).userList);

				outStreamGroups = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
				outStreamGroups.writeObject(((GroupServer) my_server).groupList);
			}

			if (my_server instanceof FileServer) {
				outStreamFiles = new ObjectOutputStream(new FileOutputStream("FileList.bin"));
				outStreamFiles.writeObject(((FileServer) my_server).fileList);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

// This thread saves the user and group list
class ShutDownListener extends Thread {
	public ServerSaver saver;

	public ShutDownListener(Server _server) {
		saver = new ServerSaver(_server);
	}

	public void run() {
		System.out.println("Shutting down server");
		saver.run();
	}
}

class AutoSave extends Thread {
	public ServerSaver saver;

	public AutoSave(Server _server) {
		saver = new ServerSaver(_server);
	}

	public void run() {
		do {
			try {
				Thread.sleep(300000); // Save group and user lists every 5 minutes
				System.out.println("Autosaving group and user or file lists...");
				saver.run();
			} catch (Exception e) {
				System.out.println("Autosave Interrupted");
			}
		} while (true);
	}
}
