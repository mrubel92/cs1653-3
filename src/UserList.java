/* This list represents the users on the server */

import java.util.*;
import java.security.Security;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.MessageDigest;

public class UserList implements java.io.Serializable {

    private static final long serialVersionUID = 7600343803563417992L;
    private final Hashtable<String, User> list = new Hashtable<>();

    public synchronized void addUser(String username, String password) {
        //generate salt
        Random r = new Random();
        String salt = null;
        char c;
        byte[] hashedpass;
        for (int i = 0; i < 16; i++) {
            c = (char) (r.nextInt(26) + 'a');
            salt += c;
        }
        String tempPassword = password;
        tempPassword += salt;
        hashedpass = hashPassword(password);
        if (hashedpass != null) {
            User newUser = new User(hashedpass, salt);
            list.put(username, newUser);
        } else {
            //don't create new user
        }
    }

    public static byte[] hashPassword(String original) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        byte[] in = original.getBytes();
        byte[] encrypted;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1", "BC");
            digest.update(in);
            encrypted = digest.digest();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            encrypted = null;
        }

        return encrypted;
    }

    public synchronized boolean checkPassword(String username, String password) {
        String salt = list.get(username).getSalt();
        String tempPassword = password;
        tempPassword += salt;
        byte[] hashedpass = hashPassword(password);
        if (hashedpass != null)
            return list.get(username).checkPassword(hashedpass.toString());
        else
            return false;
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
        private final char[] hashedPassword;
        private final String salt;

        public User(byte[] pass, String psalt) {
            groups = new ArrayList<>();
            ownership = new ArrayList<>();
            hashedPassword = pass.toString().toCharArray();
            salt = psalt;
        }

        public String getSalt() {
            return salt;
        }

        public boolean checkPassword(String userPass) {
            return hashedPassword.toString().equals(userPass);
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
