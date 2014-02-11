/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GroupThread extends Thread {
	private final Socket socket;
	private GroupServer my_gs;

	public GroupThread(Socket _socket, GroupServer _gs) {
		socket = _socket;
		my_gs = _gs;
	}

	public void run() {
		boolean proceed = true;
		try {
			// Announces connection and opens object streams
			System.out
					.println("\n*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + " ***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());

			do {
				Envelope message = (Envelope) input.readObject();
				System.out.println("\nMessage received from client: " + message.toString());
				Envelope response = null;

				if (message.getMessage().equals("GET")) // Client wants a token
				{
					String username = (String) message.getObjContents().get(0); // Get the username
					if (username == null) {
						response = new Envelope("FAIL");
						response.addObject(null);
						output.reset();
						output.writeObject(response);
					} else {
						UserToken yourToken = createToken(username); // Create a token
						// Respond to the client. On error, the client will receive a null token
						response = new Envelope("OK");
						response.addObject(yourToken);
						output.reset();
						output.writeObject(response);
					}
					System.out.println("GET response sent to client: " + response.toString());
				} else if (message.getMessage().equals("CUSER")) // Client wants to create a user
				{
					if (message.getObjContents().size() < 2)
						response = new Envelope("FAIL");
					else {
						response = new Envelope("FAIL");

						if (message.getObjContents().get(0) != null) {
							if (message.getObjContents().get(1) != null) {
								String username = (String) message.getObjContents().get(0); // Extract the username
								UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token

								if (createUser(username, yourToken))
									response = new Envelope("OK"); // Success
							}
						}
					}
					output.reset();
					output.writeObject(response);
					System.out.println("CUSER response sent to client: " + response.toString());
				} else if (message.getMessage().equals("DUSER")) // Client wants to delete a user
				{
					if (message.getObjContents().size() < 2)
						response = new Envelope("FAIL");
					else {
						response = new Envelope("FAIL");

						if (message.getObjContents().get(0) != null) {
							if (message.getObjContents().get(1) != null) {
								String username = (String) message.getObjContents().get(0); // Extract the username
								UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token

								if (deleteUser(username, yourToken))
									response = new Envelope("OK"); // Success
							}
						}
					}
					output.reset();
					output.writeObject(response);
					System.out.println("DUSER response sent to client: " + response.toString());
				} else if (message.getMessage().equals("CGROUP")) // Client wants to create a group
				{
					if (message.getObjContents().size() < 2)
						response = new Envelope("FAIL");
					else {
						response = new Envelope("FAIL");

						if (message.getObjContents().get(0) != null) {
							if (message.getObjContents().get(1) != null) {
								String groupname = (String) message.getObjContents().get(0); // Extract the groupname
								UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token
								if (createGroup(groupname, yourToken))
									response = new Envelope("OK"); // Success
							}
						}
					}
					output.reset();
					output.writeObject(response);
					System.out.println("CUSER response sent to client: " + response.toString());
				} else if (message.getMessage().equals("DGROUP")) // Client wants to delete a group
				{
					if (message.getObjContents().size() < 2)
						response = new Envelope("FAIL");
					else {
						response = new Envelope("FAIL");

						if (message.getObjContents().get(0) != null) {
							if (message.getObjContents().get(1) != null) {
								String groupname = (String) message.getObjContents().get(0); // Extract the groupname
								UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token

								if (deleteGroup(groupname, yourToken))
									response = new Envelope("OK"); // Success
							}
						}
					}
					output.reset();
					output.writeObject(response);
					System.out.println("DGROUP response sent to client: " + response.toString());
				} else if (message.getMessage().equals("LMEMBERS")) // Client wants a list of members in a group
				{
					if (message.getObjContents().size() < 2) {
						response = new Envelope("FAIL");
						response.addObject(null);
					} else {
						response = new Envelope("FAIL");
						if (message.getObjContents().get(0) != null) {
							if (message.getObjContents().get(1) != null) {
								String groupname = (String) message.getObjContents().get(0); // Extract the groupname
								UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token

								if (my_gs.userList.checkUser(yourToken.getSubject())) {
									if (my_gs.groupList.checkGroup(groupname)) {
										List<String> allMembers = my_gs.groupList.getGroupUsers(groupname);
										response = new Envelope("OK"); // Success
										response.addObject(allMembers);
									}
								}
							}
						}
					}
					output.reset();
					output.writeObject(response);
					System.out.println("LMEMBERS response sent to client: " + response.toString());
					
				} else if (message.getMessage().equals("LGROUPS")) {	
					if (message.getObjContents().size() < 1) {
						response = new Envelope("FAIL");
						response.addObject(null);
					} else {
						response = new Envelope("FAIL");
						if (message.getObjContents().get(0) != null) {
								UserToken yourToken = (UserToken) message.getObjContents().get(0); // Extract the token
							if (my_gs.userList.checkUser(yourToken.getSubject())) {
								List<String> allGroups = my_gs.userList.getUserGroups(yourToken.getSubject());
								response = new Envelope("OK"); // Success
								response.addObject(allGroups);
							}
						}
					}
					output.reset();
					output.writeObject(response);
					System.out.println("LGROUPS response sent to client: " + response.toString());
				} else if (message.getMessage().equals("AUSERTOGROUP")) // Client wants to add user to a group
				{
					if (message.getObjContents().size() < 3)
						response = new Envelope("FAIL");
					else {
						String username = (String) message.getObjContents().get(0);
						String groupname = (String) message.getObjContents().get(1);
						UserToken yourToken = (UserToken) message.getObjContents().get(2);

						if (addUserToGroup(username, groupname, yourToken))
							response = new Envelope("OK");
						else
							response = new Envelope("FAIL");

						output.reset();
						output.writeObject(response);
						System.out.println("AUSERTOGROUP response sent to client: " + response.toString());
					}
				} else if (message.getMessage().equals("RUSERFROMGROUP")) // Client wants to remove user from a group
				{
					if (message.getObjContents().size() < 3)
						response = new Envelope("FAIL");
					else {
						String username = (String) message.getObjContents().get(0);
						String groupname = (String) message.getObjContents().get(1);
						UserToken yourToken = (UserToken) message.getObjContents().get(2);

						if (deleteUserFromGroup(username, groupname, yourToken))
							response = new Envelope("OK");
						else
							response = new Envelope("FAIL");

						output.reset();
						output.writeObject(response);
						System.out.println("RUSERFROMGROUP response sent to client: " + response.toString());
					}
				} else if (message.getMessage().equals("DISCONNECT")) // Client wants to disconnect
				{
					socket.close(); // Close the socket
					proceed = false; // End this communication loop
				} else {
					response = new Envelope("FAIL"); // Server does not understand client request
					output.reset();
					output.writeObject(response);
				}
			} while (proceed);
		} catch (EOFException eof) {
			// Do nothing, the client connected to this thread is done talking
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage() + "\n\n" + e.toString());
			e.printStackTrace(System.err);
		}
	}

	private boolean addUserToGroup(String username, String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// check requester exists
		if (my_gs.userList.checkUser(requester)) {
			// check user to delete exists
			if (my_gs.userList.checkUser(username)) {
				// check group exists
				if (my_gs.groupList.checkGroup(groupname)) {
					// check group ownership
					ArrayList<String> groupOwners = my_gs.groupList.getGroupOwnership(groupname);
					if (groupOwners.contains(requester)) {
						if(!my_gs.groupList.getGroupUsers(groupname).contains(username)) {
							// add member and send back OK
							my_gs.groupList.addMember(groupname, username);
							my_gs.userList.addGroup(username, groupname);
						return true;
						} else
							return false;
					} else
						return false; // not group owner
				} else
					return false; // group doesn't exist
			} else
				return false;
		} else
			return false; // user doesn't exist
	}

	private boolean deleteUserFromGroup(String username, String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// check requester exists
		if (my_gs.userList.checkUser(requester)) {
			// check user to delete exists
			if (my_gs.userList.checkUser(username)) {
				// check group exists
				if (my_gs.groupList.checkGroup(groupname)) {
					// check group ownership
					ArrayList<String> groupOwners = my_gs.groupList.getGroupOwnership(groupname);
					if (groupOwners.contains(requester)) {
						if (my_gs.groupList.getGroupUsers(groupname).contains(username)) {
							// add member and send back OK
							my_gs.groupList.removeMember(groupname, username);
							my_gs.userList.removeGroup(username, groupname);
							return true;
						} else
							return false;
					} else
						return false; // not group owner
				} else
					return false; // group doesn't exist
			} else
				return false;
		} else
			return false; // user doesn't exist
	}

	private boolean deleteGroup(String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();

		// Check if requester exists
		if (my_gs.userList.checkUser(requester)) {
			if (my_gs.groupList.checkGroup(groupname)) {
				if (groupname.equals("ADMIN"))
					return false;

				// Check user has the privilege to delete the group
				List<String> ownersOfGroup = my_gs.groupList.getGroupOwnership(groupname);
				if (!ownersOfGroup.contains(requester))
					return false;

				List<String> groupsUsers = my_gs.groupList.getGroupUsers(groupname);
				for (String user : groupsUsers) {
					my_gs.userList.removeGroup(user, groupname);
				}

				List<String> groupsOwners = my_gs.groupList.getGroupOwnership(groupname);
				for (String owner : groupsOwners) {
					my_gs.userList.removeOwnership(owner, groupname);
				}
				my_gs.groupList.deleteGroup(groupname);
				return true;
			} else
				// Group doesn't exist
				return false;
		} else
			return false; // Requester does not exist
	}

	private boolean createGroup(String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();

		// Check if requester exists
		if (my_gs.userList.checkUser(requester)) {
			// If group doesn't exist, create it and assign ownership
			if (!my_gs.groupList.checkGroup(groupname)) {
				my_gs.groupList.addGroup(groupname);
				my_gs.groupList.addOwnership(groupname, requester);
				my_gs.groupList.addMember(groupname, requester);
				my_gs.userList.addOwnership(requester, groupname);
				my_gs.userList.addGroup(requester, groupname);
				return true;
			}
			return false;
		} else
			return false; // requester does not exist
	}

	// Method to create tokens
	private UserToken createToken(String username) {
		// Check that user exists
		if (my_gs.userList.checkUser(username)) {
			// Issue a new token with server's name, user's name, and user's groups
			UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
			return yourToken;
		} else
			return null;
	}

	// Method to create a user
	private boolean createUser(String username, UserToken yourToken) {
		String requester = yourToken.getSubject();

		// Check if requester exists
		if (my_gs.userList.checkUser(requester)) {
			// Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			// requester needs to be an administrator
			if (temp.contains("ADMIN")) {
				// Does user already exist?
				if (my_gs.userList.checkUser(username))
					return false; // User already exists
				else {
					my_gs.userList.addUser(username);
					return true;
				}
			} else
				return false; // requester not an administrator
		} else
			return false; // requester does not exist
	}

	// Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken) {
		String requester = yourToken.getSubject();

		// Does requester exist?
		if (my_gs.userList.checkUser(requester)) {
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			// requester needs to be an administer
			if (temp.contains("ADMIN")) {
				// Does user exist?
				if (my_gs.userList.checkUser(username)) {
					// User needs deleted from the groups they belong
					ArrayList<String> deleteFromGroups = new ArrayList<String>();

					// This will produce a hard copy of the list of groups this user belongs
					for (int index = 0; index < my_gs.userList.getUserGroups(username).size(); index++) {
						deleteFromGroups.add(my_gs.userList.getUserGroups(username).get(index));
					}

					// Delete the user from the groups
					// If user is the owner, removeMember will automatically delete group!
					for (int index = 0; index < deleteFromGroups.size(); index++) {
						my_gs.groupList.removeMember(deleteFromGroups.get(index), username);
					}

					// If groups are owned, they must be deleted
					ArrayList<String> deleteOwnedGroup = new ArrayList<String>();

					// Make a hard copy of the user's ownership list
					for (int index = 0; index < my_gs.userList.getUserOwnership(username).size(); index++) {
						deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));
					}

					// Delete owned groups
					for (int index = 0; index < deleteOwnedGroup.size(); index++) {
						// Use the delete group method. Token must be created for this action
						my_gs.groupList.deleteGroup(deleteOwnedGroup.get(index));
					}

					// Delete the user from the user list
					my_gs.userList.deleteUser(username);
					return true;
				} else
					return false; // User does not exist
			} else
				return false; // requester is not an administer
		} else
			return false; // requester does not exist
	}
}
