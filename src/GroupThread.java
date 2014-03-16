/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

public class GroupThread extends Thread {

    private final Socket socket;
    private final GroupServer my_gs;
    protected SecretKey secretKey;

    public GroupThread(Socket _socket, GroupServer _gs) {
        socket = _socket;
        my_gs = _gs;
    }

    @Override
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
                Envelope response;
                String username;
                String password;

                switch (message.getMessage()) {
                    case "DH":
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL");
                        else {
                            response = new Envelope("FAIL");
                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null) {
                                    byte[] nonce = (byte[]) message.getObjContents().get(0);
                                    byte[] clientPubKeyBytes = (byte[]) message.getObjContents().get(1);
                                    if (nonce != null && clientPubKeyBytes != null)
                                        response = diffieHellman(nonce, clientPubKeyBytes);
                                }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("DH response sent to client: " + response.toString());
                        break;
                    case "CHECK_PASS":
                        username = (String) message.getObjContents().get(0); // Get the username

                        if (username == null)
                            response = new Envelope("FAIL");
                        else if (my_gs.userList.checkPassword(username, "PASSWORD"))
                            response = new Envelope("NEW");
                        else
                            response = new Envelope("NOT_NEW");
                        output.reset();
                        output.writeObject(response);
                        System.out.println("CHECK_PASS response sent to client: " + response.toString());
                        break;
                    case "CREATE_PASS":
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL");
                        else {
                            response = new Envelope("FAIL");
                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null) {
                                    username = (String) message.getObjContents().get(0); // Get the username
                                    password = (String) message.getObjContents().get(1);
                                    if (username != null && password != null) {
                                        my_gs.userList.addUser(username, password);
                                        response = new Envelope("OK");
                                    }
                                }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("CREATE_PASS response sent to client: " + response.toString());
                        break;
                    case "GET": // Client wants a token
                        username = (String) message.getObjContents().get(0); // Get the username
                        password = (String) message.getObjContents().get(1);
                        if (username == null || password == null) {
                            response = new Envelope("FAIL");
                            response.addObject(null);
                        } else {
                            UserToken yourToken = createToken(username, password); // Create a token
                            // Respond to the client. On error, the client will receive a null token
                            response = new Envelope("OK");
                            response.addObject(yourToken);
                        }

                        output.reset();
                        output.writeObject(response);
                        System.out.println("GET response sent to client: " + response.toString());
                        break;
                    case "CUSER": // Client wants to create a user
                        if (message.getObjContents().size() < 3)
                            response = new Envelope("FAIL");
                        else {
                            response = new Envelope("FAIL");
                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null)
                                    if (message.getObjContents().get(2) != null) {

                                        username = (String) message.getObjContents().get(0); // Extract the username
                                        UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token
                                        if (createUser(username, yourToken))
                                            response = new Envelope("OK"); // Success
                                    }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("CUSER response sent to client: " + response.toString());
                        break;
                    case "DUSER": // Client wants to delete a user
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL");
                        else {
                            response = new Envelope("FAIL");
                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null) {
                                    username = (String) message.getObjContents().get(0); // Extract the username
                                    UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token

                                    if (deleteUser(username, yourToken))
                                        response = new Envelope("OK"); // Success
                                }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("DUSER response sent to client: " + response.toString());
                        break;
                    case "CGROUP": // Client wants to create a group
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL");
                        else {
                            response = new Envelope("FAIL");
                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null) {
                                    String groupname = (String) message.getObjContents().get(0); // Extract the groupname
                                    UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token
                                    if (createGroup(groupname, yourToken))
                                        response = new Envelope("OK"); // Success
                                }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("CUSER response sent to client: " + response.toString());
                        break;
                    case "DGROUP": // Client wants to delete a group
                        if (message.getObjContents().size() < 2)
                            response = new Envelope("FAIL");
                        else {
                            response = new Envelope("FAIL");

                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null) {
                                    String groupname = (String) message.getObjContents().get(0); // Extract the groupname
                                    UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token

                                    if (deleteGroup(groupname, yourToken))
                                        response = new Envelope("OK"); // Success
                                }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("DGROUP response sent to client: " + response.toString());
                        break;
                    case "LMEMBERS": // Client wants a list of members in a group
                        if (message.getObjContents().size() < 2) {
                            response = new Envelope("FAIL");
                            response.addObject(null);
                        } else {
                            response = new Envelope("FAIL");
                            if (message.getObjContents().get(0) != null)
                                if (message.getObjContents().get(1) != null) {
                                    String groupname = (String) message.getObjContents().get(0); // Extract the groupname
                                    UserToken yourToken = (UserToken) message.getObjContents().get(1); // Extract the token

                                    if (my_gs.userList.checkUser(yourToken.getSubject()))
                                        if (my_gs.groupList.checkGroup(groupname)) {
                                            List<String> allMembers = my_gs.groupList.getGroupUsers(groupname);
                                            response = new Envelope("OK"); // Success
                                            response.addObject(allMembers);
                                        }
                                }
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("LMEMBERS response sent to client: " + response.toString());
                        break;
                    case "LGROUPS":
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
                        break;
                    case "AUSERTOGROUP": // Client wants to add user to a group
                        if (message.getObjContents().size() < 3)
                            response = new Envelope("FAIL");
                        else {
                            username = (String) message.getObjContents().get(0);
                            String groupname = (String) message.getObjContents().get(1);
                            UserToken yourToken = (UserToken) message.getObjContents().get(2);

                            if (addUserToGroup(username, groupname, yourToken))
                                response = new Envelope("OK");
                            else
                                response = new Envelope("FAIL");
                        }

                        output.reset();
                        output.writeObject(response);
                        System.out.println("AUSERTOGROUP response sent to client: " + response.toString());
                        break;
                    case "RUSERFROMGROUP": // Client wants to remove user from a group
                        if (message.getObjContents().size() < 3)
                            response = new Envelope("FAIL");
                        else {
                            username = (String) message.getObjContents().get(0);
                            String groupname = (String) message.getObjContents().get(1);
                            UserToken yourToken = (UserToken) message.getObjContents().get(2);

                            if (deleteUserFromGroup(username, groupname, yourToken))
                                response = new Envelope("OK");
                            else
                                response = new Envelope("FAIL");
                        }
                        output.reset();
                        output.writeObject(response);
                        System.out.println("RUSERFROMGROUP response sent to client: " + response.toString());
                        break;
                    case "DISCONNECT": // Client wants to disconnect
                        socket.close(); // Close the socket
                        proceed = false; // End this communication loop
                        break;
                    default:
                        response = new Envelope("FAIL"); // Server does not understand client request
                        output.reset();
                        output.writeObject(response);
                        break;
                }
            } while (proceed);
        } catch (EOFException eof) {
            // Do nothing, the client connected to this thread is done talking
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private boolean addUserToGroup(String username, String groupname, UserToken yourToken) {
        String requester = yourToken.getSubject();
        // check requester exists
        if (my_gs.userList.checkUser(requester))
            // check user to delete exists
            if (my_gs.userList.checkUser(username))
                // check group exists
                if (my_gs.groupList.checkGroup(groupname)) {
                    // check group ownership
                    ArrayList<String> groupOwners = my_gs.groupList.getGroupOwnership(groupname);
                    if (groupOwners.contains(requester))
                        if (!my_gs.groupList.getGroupUsers(groupname).contains(username)) {
                            // add member and send back OK
                            my_gs.groupList.addMember(groupname, username);
                            my_gs.userList.addGroup(username, groupname);
                            return true;
                        } else
                            return false;
                    else
                        return false; // not group owner
                } else
                    return false; // group doesn't exist
            else
                return false;
        else
            return false; // user doesn't exist
    }

    private boolean deleteUserFromGroup(String username, String groupname, UserToken yourToken) {
        String requester = yourToken.getSubject();
        // check requester exists
        if (my_gs.userList.checkUser(requester))
            // check user to delete exists
            if (my_gs.userList.checkUser(username))
                // check group exists
                if (my_gs.groupList.checkGroup(groupname)) {
                    // check group ownership
                    ArrayList<String> groupOwners = my_gs.groupList.getGroupOwnership(groupname);
                    if (groupOwners.contains(requester))
                        if (my_gs.groupList.getGroupUsers(groupname).contains(username)) {
                            // add member and send back OK
                            my_gs.groupList.removeMember(groupname, username);
                            my_gs.userList.removeGroup(username, groupname);
                            return true;
                        } else
                            return false;
                    else
                        return false; // not group owner
                } else
                    return false; // group doesn't exist
            else
                return false;
        else
            return false; // user doesn't exist
    }

    private boolean deleteGroup(String groupname, UserToken yourToken) {
        String requester = yourToken.getSubject();

        // Check if requester exists
        if (my_gs.userList.checkUser(requester))
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
            } else // Group doesn't exist

                return false;
        else
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
    private UserToken createToken(String username, String password) {
        // Check that user exists
        if (my_gs.userList.checkUser(username))
            if (my_gs.userList.checkPassword(username, password)) {
                // Issue a new token with server's name, user's name, and user's groups
                UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
                return yourToken;
            }
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
            if (temp.contains("ADMIN"))
                // Does user already exist?
                if (my_gs.userList.checkUser(username))
                    return false; // User already exists
                else {
                    my_gs.userList.addUser(username, "PASSWORD");
                    return true;
                }
            else
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
            if (temp.contains("ADMIN"))
                // Does user exist?
                if (my_gs.userList.checkUser(username)) {
                    // User needs deleted from the groups they belong
                    ArrayList<String> deleteFromGroups = new ArrayList<>();

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
                    ArrayList<String> deleteOwnedGroup = new ArrayList<>();

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
            else
                return false; // requester is not an administer
        } else
            return false; // requester does not exist
    }

    // http://exampledepot.8waytrips.com/egs/javax.crypto/KeyAgree.html
    private Envelope diffieHellman(byte[] nonce, byte[] clientPubKeyBytes) {
        Envelope response = new Envelope("FAIL");
        try {
            response = new Envelope("OK");
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            
            // Server's pub and priv DH key pair
            KeyPair dhKP = Utils.genDHKeyPair();
            PrivateKey servDHPrivKey = dhKP.getPrivate();
            PublicKey servDHPubKey = dhKP.getPublic();
            
            // Convert the client's DH public key bytes into a PublicKey object
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(clientPubKeyBytes);
            KeyFactory keyFact = KeyFactory.getInstance("DH", "BC");
            PublicKey clientPubKey = keyFact.generatePublic(x509KeySpec);
            
            // Prepare to generate the AES secret key with the server's DH private key and client's DH public key
            KeyAgreement ka = KeyAgreement.getInstance("DH", "BC");
            ka.init(servDHPrivKey);
            ka.doPhase(clientPubKey, true);
            
            // Generate the secret key
            secretKey = ka.generateSecret("AES");
            
            // Send pub key and nonce back to client
            Signature sig = Signature.getInstance("SHA1withRSA", "BC");
            sig.initSign(GroupServer.gsPrivKey);
            sig.update(nonce);
            byte[] signedNonce = sig.sign();
            
            response.addObject(signedNonce);
            response.addObject(servDHPubKey.getEncoded());
            return response;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException | NoSuchProviderException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return response;
    }
}
