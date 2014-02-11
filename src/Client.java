import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public abstract class Client {

	/*
	 * Protected keyword is like private but subclasses have access to Socket and input/output streams
	 */
	protected Socket sock;
	protected ObjectInputStream input;
	protected ObjectOutputStream output;

	public boolean connect(final String server, final int port) {
		System.out.println("\nAttempting to connect to: " + server + ":" + port);
		try {
			@SuppressWarnings("resource")
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(server, port), 10000); // 10 second timeout
			output = new ObjectOutputStream(socket.getOutputStream());
			input = new ObjectInputStream(socket.getInputStream());
			return true;
		} catch (SocketTimeoutException e2) {
			System.err.println("Error: " + e2.getMessage());
			e2.printStackTrace(System.err);
		} catch (UnknownHostException e1) {
			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace(System.err);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return false;
	}

	public boolean isConnected() {
		if (sock == null || !sock.isConnected())
			return false;
		else
			return true;
	}

	public void disconnect() {
		if (isConnected()) {
			try {
				Envelope message = new Envelope("DISCONNECT");
				output.writeObject(message);
			} catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
}
