import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;


public class HomePanel extends JPanel {

	private static final long serialVersionUID = -8742357999390766582L;
	private JTextField username;
	private JTextField ipAddress;
	private JTextField portNum;
	protected static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
											  + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
											  + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
											  + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	protected static final String PORT_PATTERN = "\\d{1,5}";
	private static Pattern p;
	private static Pattern p2;
	private static Pattern p3;

	public HomePanel(final JTabbedPane tabbedPanel) {
		p = Pattern.compile(IP_PATTERN);
		p2 = Pattern.compile(RunClient.NAME_PATTERN);
		p3 = Pattern.compile(PORT_PATTERN);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		// Title
		JLabel lblCryptoshare = new JLabel("CryptoShare");
		lblCryptoshare.setFont(new Font("Dialog", Font.BOLD, 20));
		GridBagConstraints gbc_lblCryptoshare = new GridBagConstraints();
		gbc_lblCryptoshare.insets = new Insets(0, 0, 5, 5);
		gbc_lblCryptoshare.gridx = 5;
		gbc_lblCryptoshare.gridy = 1;
		add(lblCryptoshare, gbc_lblCryptoshare);

		JLabel lblUsername = new JLabel("Username");
		GridBagConstraints gbc_lblUsername = new GridBagConstraints();
		gbc_lblUsername.insets = new Insets(0, 0, 5, 5);
		gbc_lblUsername.gridx = 5;
		gbc_lblUsername.gridy = 3;
		add(lblUsername, gbc_lblUsername);

		// Username text field
		username = new JTextField();
		username.setText("tdoshea90");
		GridBagConstraints gbc_username = new GridBagConstraints();
		gbc_username.insets = new Insets(0, 0, 5, 5);
		gbc_username.fill = GridBagConstraints.HORIZONTAL;
		gbc_username.gridx = 5;
		gbc_username.gridy = 4;
		add(username, gbc_username);
		username.setColumns(10);

		JLabel lblGroupServerIp = new JLabel("Group Server IP");
		GridBagConstraints gbc_lblGroupServerIp = new GridBagConstraints();
		gbc_lblGroupServerIp.insets = new Insets(0, 0, 5, 5);
		gbc_lblGroupServerIp.gridx = 5;
		gbc_lblGroupServerIp.gridy = 5;
		add(lblGroupServerIp, gbc_lblGroupServerIp);

		// IP address text field
		ipAddress = new JTextField();
		ipAddress.setText("127.0.0.1");
		ipAddress.setFont(new Font("Monospaced", Font.PLAIN, 10));
		GridBagConstraints gbc_ipAddress = new GridBagConstraints();
		gbc_ipAddress.insets = new Insets(0, 0, 5, 5);
		gbc_ipAddress.fill = GridBagConstraints.HORIZONTAL;
		gbc_ipAddress.gridx = 5;
		gbc_ipAddress.gridy = 6;
		add(ipAddress, gbc_ipAddress);
		ipAddress.setColumns(10);

		JLabel lblPort = new JLabel("Port");
		GridBagConstraints gbc_lblPort = new GridBagConstraints();
		gbc_lblPort.insets = new Insets(0, 0, 5, 5);
		gbc_lblPort.gridx = 5;
		gbc_lblPort.gridy = 7;
		add(lblPort, gbc_lblPort);

		// Port text field
		portNum = new JTextField();
		portNum.setText("8765");
		GridBagConstraints gbc_portNum = new GridBagConstraints();
		gbc_portNum.insets = new Insets(0, 0, 5, 5);
		gbc_portNum.fill = GridBagConstraints.HORIZONTAL;
		gbc_portNum.gridx = 5;
		gbc_portNum.gridy = 8;
		add(portNum, gbc_portNum);
		portNum.setColumns(10);

		// Login button and logic
		final JButton btnLogin = new JButton("Login");
		GridBagConstraints gbc_btnLogin = new GridBagConstraints();
		gbc_btnLogin.insets = new Insets(0, 0, 0, 5);
		gbc_btnLogin.gridx = 5;
		gbc_btnLogin.gridy = 9;
		btnLogin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String un = username.getText();
				Matcher m2 = p2.matcher(un);
				if (!m2.matches()) {
					JOptionPane.showMessageDialog(null, "Bad username format!", "Bad Username",
							JOptionPane.OK_CANCEL_OPTION);
					return;
				}

				if (connectToServer(ipAddress, portNum, RunClient.gclient)) {
					RunClient.userToken = RunClient.gclient.getToken(un.toUpperCase());
					if (RunClient.userToken == null) {
						JOptionPane.showMessageDialog(null,
								"User does not exist! Only administrators can create users.", "Bad User",
								JOptionPane.OK_CANCEL_OPTION);
						return;
					}
					btnLogin.setEnabled(false);
					tabbedPanel.setEnabledAt(1, true);
					tabbedPanel.setEnabledAt(2, true);
				}
			}
		});
		add(btnLogin, gbc_btnLogin);
	}

	protected static boolean connectToServer(JTextField ipText, JTextField portText, Client client) {
		String ip = ipText.getText();
		Matcher m = p.matcher(ip);
		if (!m.matches()) {
			JOptionPane.showMessageDialog(null, "Bad IP address format!", "Bad IP",
					JOptionPane.OK_CANCEL_OPTION);
			return false;
		}

		String port = portText.getText();
		Matcher m3 = p3.matcher(port);
		if (!m3.matches()) {
			JOptionPane.showMessageDialog(null, "Bad port format!", "Bad port", JOptionPane.OK_CANCEL_OPTION);
			return false;
		}

		if (!client.connect(ip, Integer.parseInt(port))) {
			JOptionPane.showMessageDialog(null, "Failed to connect to server!", "Connection Failed",
					JOptionPane.OK_CANCEL_OPTION);
			System.out.println("Failed to connect to: " + ip + ":" + port);
			return false;
		}
		JOptionPane.showMessageDialog(null, "Successfully connected to: " + ip + ":" + port);
		System.out.println("Successfully connected to: " + ip + ":" + port);
		return true;
	}
}
