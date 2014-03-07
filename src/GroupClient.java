/* Implements the GroupClient Interface */

import java.util.ArrayList;
import java.util.List;


public class GroupClient extends Client implements GroupClientInterface {

	public UserToken getToken(String username) {
		try {
			UserToken token = null;
			Envelope message = null, response = null;

			// Tell the server to return a token.
			message = new Envelope("GET");
			message.addObject(username); // Add user name string
			System.out.println("\nGET message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			// Get the response from the server
			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());
			// Successful response
			if (response.getMessage().equals("OK")) {
				// If there is a token in the Envelope, return it
				ArrayList<Object> temp = null;
				temp = response.getObjContents();

				if (temp.size() == 1) {
					token = (UserToken) temp.get(0);
					return token;
				}
			}
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean createUser(String username, UserToken token, String password) {
		try {
			Envelope message = null, response = null;
			// Tell the server to create a user
			message = new Envelope("CUSER");
			message.addObject(username); // Add user name string
			message.addObject(token); // Add the requester's token
			message.addObject(password); // Add the user's password
			System.out.println("\nCUSER message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
				return true;
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteUser(String username, UserToken token) {
		try {
			Envelope message = null, response = null;

			// Tell the server to delete a user
			message = new Envelope("DUSER");
			message.addObject(username); // Add user name
			message.addObject(token); // Add requester's token
			System.out.println("\nDUSER message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
				return true;
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean createGroup(String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			// Tell the server to create a group
			message = new Envelope("CGROUP");
			message.addObject(groupname); // Add the group name string
			message.addObject(token); // Add the requester's token
			System.out.println("\nCGROUP message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());
			// If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				token.addGroup(groupname);
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteGroup(String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			// Tell the server to delete a group
			message = new Envelope("DGROUP");
			message.addObject(groupname); // Add group name string
			message.addObject(token); // Add requester's token
			System.out.println("\nDGROUP message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());
			// If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				token.removeGroup(groupname);
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> listMembers(String group, UserToken token) {
		try {
			Envelope message = null, response = null;
			// Tell the server to return the member list
			message = new Envelope("LMEMBERS");
			message.addObject(group); // Add group name string
			message.addObject(token); // Add requester's token
			System.out.println("\nLMEMBERS message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());

			// If server indicates success, return the member list
			if (response.getMessage().equals("OK"))
				return (List<String>) response.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
						
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<String> listGroups(UserToken token) {
		try {
			Envelope message = null, response = null;
			// Tell the server to return the member list
			message = new Envelope("LGROUPS");
			message.addObject(token); // Add requester's token
			System.out.println("\nLGROUPS message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());

			// If server indicates success, return the member list
			if (response.getMessage().equals("OK"))
				return (List<String>) response.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
						
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean addUserToGroup(String username, String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			// Tell the server to add a user to the group
			message = new Envelope("AUSERTOGROUP");
			message.addObject(username); // Add user name string
			message.addObject(groupname); // Add group name string
			message.addObject(token); // Add requester's token
			System.out.println("\nAUSERTOGROUP message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
				return true;
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteUserFromGroup(String username, String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			// Tell the server to remove a user from the group
			message = new Envelope("RUSERFROMGROUP");
			message.addObject(username); // Add user name string
			message.addObject(groupname); // Add group name string
			message.addObject(token); // Add requester's token
			System.out.println("\nRUSERFROMGROUP message sent to Group Server: " + message.toString());
			output.reset();
			output.writeObject(message);

			response = (Envelope) input.readObject();
			System.out.println("Message received from Group Server: " + response.toString());
			// If server indicates success, return true
			if (response.getMessage().equals("OK"))
				return true;
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}
}
