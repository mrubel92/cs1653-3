/* This list represents the users on the server */

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.security.Security;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class UserList implements java.io.Serializable {

    private static final long serialVersionUID = 7600343803563417992L;
    private final Hashtable<String, User> list = new Hashtable<>();
    private static final int SALT_LENGTH = 16;

    public synchronized void addUser(String username, String password) {
        // Generate salt
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        byte[] hashedpass = hashPassword(password, salt);
        User newUser = new User(hashedpass, salt);
        list.put(username, newUser);
    }
    
    public synchronized void changePass(String username, String password) {
        byte[] hashedpass = hashPassword(password, list.get(username).getSalt());
        list.get(username).setPassword(hashedpass);
    }

    public static byte[] hashPassword(String password, byte[] salt) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1", "BC");
            digest.reset();
            digest.update(salt);
            return digest.digest(password.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | NoSuchProviderException | UnsupportedEncodingException e) {
            System.err.println("Error: " + e.getMessage() + "\n\n" + e.toString());
            e.printStackTrace(System.err);
        }
        return null;
    }

    public synchronized boolean checkPassword(String username, String password) {
        if (list.get(username) == null)
            return false;
        byte[] salt = list.get(username).getSalt();
        byte[] hashedpass = hashPassword(password, salt);
        return list.get(username).checkPassword(hashedpass);
    }

    public synchronized void deleteUser(String username) {
        list.remove(username);
    }

    public synchronized boolean checkUser(String username) {
        return list.containsKey(username);
    }

    public synchronized ArrayList<String> getUserGroups(String username) {
        return list.get(username).getGroups();
    }

    public synchronized ArrayList<String> getUserOwnership(String username) {
        return list.get(username).getOwnership();
    }

    public synchronized void addGroup(String user, String groupname) {
        list.get(user).addGroup(groupname);
    }

    public synchronized void removeGroup(String user, String groupname) {
        list.get(user).removeGroup(groupname);
    }

    public synchronized void addOwnership(String user, String groupname) {
        list.get(user).addOwnership(groupname);
    }

    public synchronized void removeOwnership(String user, String groupname) {
        list.get(user).removeOwnership(groupname);
    }

    class User implements java.io.Serializable {

        private static final long serialVersionUID = -6699986336399821598L;
        private final ArrayList<String> groups;
        private final ArrayList<String> ownership;
        private byte[] hashedPassword;
        private final byte[] salt;

        public User(byte[] pass, byte[] psalt) {
            groups = new ArrayList<>();
            ownership = new ArrayList<>();
            hashedPassword = pass;
            salt = psalt;
        }

        public byte[] getSalt() {
            return salt;
        }

        public boolean checkPassword(byte[] userPass) {
            return Arrays.equals(userPass, hashedPassword);
        }
        
        public void setPassword(byte[] password) {
            hashedPassword = password;
        }

        public ArrayList<String> getGroups() {
            return groups;
        }

        public ArrayList<String> getOwnership() {
            return ownership;
        }

        public void addGroup(String group) {
            groups.add(group);
        }

        public void removeGroup(String group) {
            if (!groups.isEmpty())
                if (groups.contains(group))
                    groups.remove(groups.indexOf(group));
        }

        public void addOwnership(String group) {
            ownership.add(group);
        }

        public void removeOwnership(String group) {
            if (!ownership.isEmpty())
                if (ownership.contains(group))
                    ownership.remove(ownership.indexOf(group));
        }

    }

}
