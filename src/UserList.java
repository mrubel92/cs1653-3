/* This list represents the users on the server */

import java.util.*;
import java.security.Security;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.MessageDigest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.*;


public class UserList implements java.io.Serializable {
	private static final long serialVersionUID = 7600343803563417992L;
	private Hashtable<String, User> list = new Hashtable<String, User>();

	public synchronized void addUser(String username, String password) {
		//generate salt
		Random r = new Random();
		String salt;
		char c;
		byte[] hashedpass;
		for(int i = 0; i < 16; i++)
		{
			c = (char)(r.nextInt(26) + 'a');
			salt = salt + c;
		}
		password = password + salt;
		hashedpass = hashPassword(password);
		if(hashedpass != null)
		{
			User newUser = new User(hashedpass, salt);		
			list.put(username, newUser);
		}
		else
		{
			//error
		}
	}
	
	public static byte[] hashPassword(String original)
	{
		Security.addProvider(new BouncyCastleProvider());
		byte[] in = original.getBytes();
		byte[] encrypted;
		try
		{
			MessageDigest digest1 = MessageDigest.getInstance("SHA-1","BC");
			digest.update(in);
			encrypted = digest1.digest();
		}
		catch (Exception e)
		{
			encrypted = null;
		}
		
		return encrypted;
	}
	
	public synchronized boolean checkPassword(String username, String password)
	{
		String salt = list.get(username).getSalt();
		password = password + salt;
		byte[] hashedpass = hashPassword(password);
		return list.get(username).checkPassword(hashedpass);
	}

	public synchronized void deleteUser(String username) {
		list.remove(username);
	}

	public synchronized boolean checkUser(String username) {
		if (list.containsKey(username))
			return true;
		else
			return false;
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
		private ArrayList<String> groups;
		private ArrayList<String> ownership;
		private char[] hashedPassword;
		private String salt;

		public User(byte[] pass, String psalt) {
			groups = new ArrayList<String>();
			ownership = new ArrayList<String>();
			hashedPassword = pass;
			salt = psalt;
		}
		
		public String getSalt()
		{
			return salt;
		}
		
		public boolean checkPassword(String userPass)
		{
			if(hashedPassword.equals(userPass))
				return true;
			return false;
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
			if (!groups.isEmpty()) {
				if (groups.contains(group)) {
					groups.remove(groups.indexOf(group));
				}
			}
		}

		public void addOwnership(String group) {
			ownership.add(group);
		}

		public void removeOwnership(String group) {
			if (!ownership.isEmpty()) {
				if (ownership.contains(group))
					ownership.remove(ownership.indexOf(group));
			}
		}

	}

}
