/* Implements the GroupClient Interface */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupClient extends Client implements GroupClientInterface {
    private int messageCounter = 0;
    public boolean checkNewPassword(String username) {
        try {
            Envelope message, response;
            message = new Envelope("CHECK_PASS");
            message.addObject(username);
            System.out.println("\nCHECK_PASS message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int) response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());

            if (response.getMessage().equals("NEW"))
                return true;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return false;
    }

    public boolean createNewPassword(String username, String newPassword) {
        try {
            Envelope message, response;
            message = new Envelope("CREATE_PASS");
            message.addObject(username);
            message.addObject(newPassword);
            System.out.println("\nCREATE_PASS message sent to Group Server: " + message.toString());
            messageCounter++;
            message.addObject(messageCounter);
            Envelope tempMessage = new Envelope("ENCRYPTED");
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            System.out.println("Message received from Group Server: " + response.toString());

            if (response.getMessage().equals("OK"))
                return true;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return false;
    }

    @Override
    public Envelope getToken(String username, String password) {
        try {
            UserToken token;
            byte[] signedToken;
            Envelope message, response, tokenEnv;

            // Tell the server to return a token.
            message = new Envelope("GET");
            message.addObject(username); // Add user name string
            message.addObject(password);
            System.out.println("\nGET message sent to Group Server: " + message.toString());
            messageCounter++;
            message.addObject(messageCounter);
            Envelope tempMessage = new Envelope("ENCRYPTED");
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());
            // Successful response
            if (response.getMessage().equals("OK")) {
                // If there is a token in the Envelope, return it
                ArrayList<Object> temp;
                temp = response.getObjContents();

                if (temp.size() == 3) {
                    token = (UserToken) temp.get(0);
                    signedToken = (byte[]) temp.get(1);
                    tokenEnv = new Envelope("TOKEN");
                    tokenEnv.addObject(token);
                    tokenEnv.addObject(signedToken);
                    return tokenEnv;
                }
            }
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    @Override
    public boolean createUser(String username, UserToken token, String password) {
        try {
            Envelope message, response;
            // Tell the server to create a user
            message = new Envelope("CUSER");
            message.addObject(username); // Add user name string
            message.addObject(token); // Add the requester's token
            message.addObject(password); // Add the user's password
            System.out.println("\nCUSER message sent to Group Server: " + message.toString());
            messageCounter++;
            message.addObject(messageCounter);
            Envelope tempMessage = new Envelope("ENCRYPTED");
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());
            return response.getMessage().equals("OK");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public boolean deleteUser(String username, UserToken token) {
        try {
            Envelope message, response;

            // Tell the server to delete a user
            message = new Envelope("DUSER");
            message.addObject(username); // Add user name
            message.addObject(token); // Add requester's token
            System.out.println("\nDUSER message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());
            return response.getMessage().equals("OK");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public boolean createGroup(String groupname, UserToken token) {
        try {
            Envelope message, response;
            // Tell the server to create a group
            message = new Envelope("CGROUP");
            message.addObject(groupname); // Add the group name string
            message.addObject(token); // Add the requester's token
            System.out.println("\nCGROUP message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());
            // If server indicates success, return true
            if (response.getMessage().equals("OK")) {
                token.addGroup(groupname);
                return true;
            }
            return false;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public boolean deleteGroup(String groupname, UserToken token) {
        try {
            Envelope message, response;
            // Tell the server to delete a group
            message = new Envelope("DGROUP");
            message.addObject(groupname); // Add group name string
            message.addObject(token); // Add requester's token
            System.out.println("\nDGROUP message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group : " + response.toString());
            // If server indicates success, return true
            if (response.getMessage().equals("OK")) {
                token.removeGroup(groupname);
                return true;
            }
            return false;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "unchecked"})
    @Override
    public List<String> listMembers(String group, UserToken token) {
        try {
            Envelope message, response;
            // Tell the server to return the member list
            message = new Envelope("LMEMBERS");
            message.addObject(group); // Add group name string
            message.addObject(token); // Add requester's token
            System.out.println("\nLMEMBERS message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());

            // If server indicates success, return the member list
            if (response.getMessage().equals("OK"))
                return (List<String>) response.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "unchecked"})
    public List<String> listGroups(UserToken token) {
        try {
            Envelope message, response;
            // Tell the server to return the member list
            message = new Envelope("LGROUPS");
            message.addObject(token); // Add requester's token
            System.out.println("\nLGROUPS message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());

            // If server indicates success, return the member list
            if (response.getMessage().equals("OK"))
                return (List<String>) response.getObjContents().get(0); // This cast creates compiler warnings. Sorry.
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    @Override
    public boolean addUserToGroup(String username, String groupname, UserToken token) {
        try {
            Envelope message, response;
            // Tell the server to add a user to the group
            message = new Envelope("AUSERTOGROUP");
            message.addObject(username); // Add user name string
            message.addObject(groupname); // Add group name string
            message.addObject(token); // Add requester's token
            System.out.println("\nAUSERTOGROUP message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());
            return response.getMessage().equals("OK");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public boolean deleteUserFromGroup(String username, String groupname, UserToken token) {
        try {
            Envelope message, response;
            // Tell the server to remove a user from the group
            message = new Envelope("RUSERFROMGROUP");
            message.addObject(username); // Add user name string
            message.addObject(groupname); // Add group name string
            message.addObject(token); // Add requester's token
            System.out.println("\nRUSERFROMGROUP message sent to Group Server: " + message.toString());

            Envelope tempMessage = new Envelope("ENCRYPTED");
            messageCounter++;
            message.addObject(messageCounter);
            tempMessage.addObject(Utils.encryptEnv(message, gsSecretKey, ivSpec));
            output.reset();
            output.writeObject(tempMessage);

            Envelope tempResponse = (Envelope) input.readObject();
            response = Utils.decryptEnv((byte[]) tempResponse.getObjContents().get(0), gsSecretKey, ivSpec);
            int numberIndex = response.getObjContents().size() - 1; //gives us the index of the number appended to the message
            int seqNumber = (int)response.getObjContents().get(numberIndex);
            messageCounter++;
            if(seqNumber != messageCounter)
            {
                //cease communication
                System.out.println("Possible Replay or Reorder Attack");
                System.exit(0);
            }
            System.out.println("Message received from Group Server: " + response.toString());
            return response.getMessage().equals("OK");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }
}
