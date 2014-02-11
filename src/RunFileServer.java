/* Driver program for FileSharing File Server */

public class RunFileServer {

	public static void main(String[] args) {
		if (args.length == 1) {
			try {
				FileServer server = new FileServer(Integer.parseInt(args[0]));
				server.start();
			} catch (NumberFormatException e) {
				System.out.printf("Enter a valid port number or pass no arguments to use the default port (%d)\n",
						FileServer.FILE_SERVER_PORT);
			}
		} else {
			FileServer server = new FileServer();
			server.start();
		}
	}
}
