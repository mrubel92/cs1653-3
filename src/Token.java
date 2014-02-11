import java.util.ArrayList;
import java.util.List;

public class Token implements UserToken {
	private static final long serialVersionUID = 678708483223677463L;
	String issuer;
	String subject;
	ArrayList<String> usersGroups;

	public Token(String name, String username, ArrayList<String> userGroups) {
		issuer = name;
		subject = username;
		usersGroups = new ArrayList<String>(userGroups.size());
		for (String group : userGroups) {
			usersGroups.add(group);
		}
	}

	@Override
	public String getIssuer() {
		return issuer;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public List<String> getGroups() {
		return usersGroups;
	}

	@Override
	public String toString() {
		return "Token [issuer=" + issuer + ", subject=" + subject + ", usersGroups=" + usersGroups + "]";
	}
	
	@Override
	public void addGroup(String groupToAdd) {
		usersGroups.add(groupToAdd);
	}
	
	@Override
	public void removeGroup(String groupToDel) {
		usersGroups.remove(groupToDel);
	}
}
