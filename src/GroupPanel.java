
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

public class GroupPanel extends JPanel {

    private static final long serialVersionUID = -8583833759512500805L;
    @SuppressWarnings("rawtypes")
    private final DefaultListModel groupListModel;
    @SuppressWarnings("rawtypes")
    private final DefaultListModel memberListModel;
    private String lastSelectedGroup;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public GroupPanel() {
        lastSelectedGroup = null;

        // Main pane. Buttons on left, lists on right.
        final JSplitPane contentPane = new JSplitPane();

        // Lists
        groupListModel = new DefaultListModel();
        final JList groupList = new JList(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane groupListScrollPane = new JScrollPane(groupList);

        memberListModel = new DefaultListModel();
        final JList memberList = new JList(memberListModel);
        memberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane memberListScrollPane = new JScrollPane(memberList);

        // Setup left panel
        contentPane.setLeftComponent(createLeftPanel(groupList, memberList));

        // Setup right panel
        contentPane.setRightComponent(createRightPanel(memberListScrollPane, groupListScrollPane));

        // Everything to parent
        add(contentPane);
    }

    private Component createRightPanel(final JScrollPane memberListScrollPane, final JScrollPane groupListScrollPane) {
        JSplitPane rightPanel = new JSplitPane();

        // Group list panel: label + list
        JPanel groupPanelContainer = new JPanel();
        groupPanelContainer.add(new JLabel("My Groups"));
        groupPanelContainer.add(groupListScrollPane);
        groupPanelContainer.setLayout(new BoxLayout(groupPanelContainer, BoxLayout.Y_AXIS));

        // Members list panel: label + list
        JPanel membersPanelContainer = new JPanel();
        membersPanelContainer.add(new JLabel("Members"));
        membersPanelContainer.add(memberListScrollPane);
        membersPanelContainer.setLayout(new BoxLayout(membersPanelContainer, BoxLayout.Y_AXIS));

        rightPanel.setLeftComponent(groupPanelContainer);
        rightPanel.setRightComponent(membersPanelContainer);

        return rightPanel;
    }

    @SuppressWarnings("rawtypes")
    private Component createLeftPanel(final JList groupList, final JList memberList) {
        JPanel leftPanel = new JPanel();
        GridBagLayout gbl_leftPanel = new GridBagLayout();
        gbl_leftPanel.columnWidths = new int[]{130, 0};
        gbl_leftPanel.rowHeights = new int[]{15, 25, 25, 25, 25, 0};
        gbl_leftPanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
        gbl_leftPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        leftPanel.setLayout(gbl_leftPanel);

        // Group options label
        JLabel lblGroupOptions = new JLabel("Group Options");
        GridBagConstraints gbc_lblGroupOptions = new GridBagConstraints();
        gbc_lblGroupOptions.anchor = GridBagConstraints.WEST;
        gbc_lblGroupOptions.insets = new Insets(0, 0, 5, 0);
        gbc_lblGroupOptions.gridx = 0;
        gbc_lblGroupOptions.gridy = 0;
        leftPanel.add(lblGroupOptions, gbc_lblGroupOptions);

        // Create Group button
        JButton btnCreateGroup = new JButton("Create Group");
        btnCreateGroup.setActionCommand("Create Group");
        btnCreateGroup.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String newGroupName = RunClient.askForValidInput("Enter new group name", null);
                if (newGroupName != null) {
                    if (groupListModel.contains(newGroupName)) {
                        JOptionPane.showMessageDialog(null, "Group: " + newGroupName + " already exists!");
                        return;
                    }
                    if (!RunClient.gclient.createGroup(newGroupName, RunClient.userToken))
                        JOptionPane.showMessageDialog(null, "Failed to create group: " + newGroupName + "!");
                    else {
                        groupListModel.addElement(newGroupName);
                        memberListModel.removeAllElements();
                    }
                }
            }
        });
        GridBagConstraints gbc_btnCreateGroup = new GridBagConstraints();
        gbc_btnCreateGroup.anchor = GridBagConstraints.WEST;
        gbc_btnCreateGroup.insets = new Insets(0, 0, 5, 0);
        gbc_btnCreateGroup.gridx = 0;
        gbc_btnCreateGroup.gridy = 1;
        gbc_btnCreateGroup.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(btnCreateGroup, gbc_btnCreateGroup);

        // Delete Group button
        JButton btnDeleteGroup = new JButton("Delete Group");
        btnDeleteGroup.setActionCommand("Delete Group");
        btnDeleteGroup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = groupList.getSelectedIndex();
                if (index != -1) {
                    String groupName = groupListModel.get(index).toString();
                    int reply = JOptionPane.showConfirmDialog(null, "Really delete group: " + groupName, "WARNING",
                                                              JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                        if (RunClient.gclient.deleteGroup(groupName, RunClient.userToken)) {
                            groupListModel.remove(index);
                            memberListModel.removeAllElements();
                        } else
                            JOptionPane.showMessageDialog(null, "Failed to delete group: " + groupName + "!");
                } else
                    JOptionPane.showMessageDialog(null, "Select a group");
            }
        });
        GridBagConstraints gbc_btnDeleteGroup = new GridBagConstraints();
        gbc_btnDeleteGroup.anchor = GridBagConstraints.WEST;
        gbc_btnDeleteGroup.insets = new Insets(0, 0, 5, 0);
        gbc_btnDeleteGroup.gridx = 0;
        gbc_btnDeleteGroup.gridy = 2;
        gbc_btnDeleteGroup.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(btnDeleteGroup, gbc_btnDeleteGroup);

        // Add User button
        JButton btnAddUser = new JButton("Add User");
        btnAddUser.setActionCommand("Add User");
        btnAddUser.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = groupList.getSelectedIndex();
                if (index != -1) {
                    String groupName = groupListModel.get(index).toString();
                    String newUserName = RunClient.askForValidInput("Enter user's name to add to: " + groupName, null);
                    if (newUserName != null)
                        if (RunClient.gclient.addUserToGroup(newUserName, groupName, RunClient.userToken)) {
                            JOptionPane.showMessageDialog(null, "Successfully added: " + newUserName + " to: " + groupName);
                            List<String> members = RunClient.gclient.listMembers(groupName, RunClient.userToken);
                            memberListModel.removeAllElements();
                            for (String member : members) {
                                memberListModel.addElement(member);
                            }
                        } else
                            JOptionPane.showMessageDialog(null, "Failed to add: " + newUserName + " to: " + groupName
                                    + "!");
                } else
                    JOptionPane.showMessageDialog(null, "Select a group");
            }
        });
        GridBagConstraints gbc_btnAddUser = new GridBagConstraints();
        gbc_btnAddUser.anchor = GridBagConstraints.WEST;
        gbc_btnAddUser.insets = new Insets(0, 0, 5, 0);
        gbc_btnAddUser.gridx = 0;
        gbc_btnAddUser.gridy = 3;
        gbc_btnAddUser.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(btnAddUser, gbc_btnAddUser);

        // Remove User button
        JButton btnRemoveUser = new JButton("Remove User");
        btnRemoveUser.setActionCommand("Remove User");
        btnRemoveUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int memberIndex = memberList.getSelectedIndex();
                int groupIndex = groupList.getSelectedIndex();
                if ((memberIndex == -1) || (groupIndex == -1))
                    JOptionPane.showMessageDialog(null, "Select a group and a user");
                else {
                    String removeUserName = memberListModel.get(memberIndex).toString();
                    String selectedGroup = groupListModel.get(groupIndex).toString();
                    if (!removeUserName.equals(RunClient.userToken.getSubject())) {
                        int reply = JOptionPane.showConfirmDialog(null, "Really remove user: " + removeUserName
                                + " from " + selectedGroup, "WARNING", JOptionPane.YES_NO_OPTION);
                        if (reply == JOptionPane.YES_OPTION)
                            if (RunClient.gclient.deleteUserFromGroup(removeUserName, selectedGroup,
                                                                      RunClient.userToken))
                                memberListModel.remove(memberIndex);
                            else
                                JOptionPane.showMessageDialog(null, "Failed to remove: " + removeUserName + " from "
                                        + selectedGroup + "!");
                    } else
                        JOptionPane.showMessageDialog(null, "You can't delete yourself you idiot");
                }
            }
        });
        GridBagConstraints gbc_btnRemoveUser = new GridBagConstraints();
        gbc_btnRemoveUser.anchor = GridBagConstraints.WEST;
        gbc_btnRemoveUser.insets = new Insets(0, 0, 5, 0);
        gbc_btnRemoveUser.gridx = 0;
        gbc_btnRemoveUser.gridy = 4;
        gbc_btnRemoveUser.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(btnRemoveUser, gbc_btnRemoveUser);

        // List Users button
        JButton btnListUser = new JButton("List Users");
        btnListUser.setActionCommand("List Users");
        btnListUser.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                int groupIndex = groupList.getSelectedIndex();
                if (groupIndex != -1) {
                    String selectedGroup = groupListModel.get(groupIndex).toString();
                    if (!selectedGroup.equals(lastSelectedGroup)) {
                        lastSelectedGroup = selectedGroup;
                        List<String> members = RunClient.gclient.listMembers(selectedGroup, RunClient.userToken);
                        if (members != null) {
                            memberListModel.removeAllElements();
                            for (String member : members) {
                                if (!memberListModel.contains(member))
                                    memberListModel.addElement(member);
                            }
                        } else
                            JOptionPane.showMessageDialog(null, "Failed to retreive member list for group: "
                                    + selectedGroup);
                    }
                } else
                    JOptionPane.showMessageDialog(null, "Select a group");
            }
        });
        GridBagConstraints gbc_btnListUser = new GridBagConstraints();
        gbc_btnListUser.anchor = GridBagConstraints.WEST;
        gbc_btnListUser.insets = new Insets(0, 0, 5, 0);
        gbc_btnListUser.gridx = 0;
        gbc_btnListUser.gridy = 5;
        gbc_btnListUser.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(btnListUser, gbc_btnListUser);

        // List Groups button
        JButton btnListGroups = new JButton("List Groups");
        btnListGroups.setActionCommand("List Groups");
        btnListGroups.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> groups = RunClient.getGroups();
                groupListModel.removeAllElements();
                for (String group : groups) {
                    groupListModel.addElement(group);
                }
            }
        });
        GridBagConstraints gbc_btnListGroups = new GridBagConstraints();
        gbc_btnListGroups.anchor = GridBagConstraints.WEST;
        gbc_btnListGroups.insets = new Insets(0, 0, 5, 0);
        gbc_btnListGroups.gridx = 0;
        gbc_btnListGroups.gridy = 6;
        gbc_btnListGroups.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(btnListGroups, gbc_btnListGroups);

        // Admin options label
        JLabel lblAdminOptions = new JLabel("Admin Options");
        GridBagConstraints gbc_lblAdminOptions = new GridBagConstraints();
        gbc_lblAdminOptions.anchor = GridBagConstraints.WEST;
        gbc_lblAdminOptions.insets = new Insets(0, 0, 5, 0);
        gbc_lblAdminOptions.gridx = 0;
        gbc_lblAdminOptions.gridy = 7;
        leftPanel.add(lblAdminOptions, gbc_lblAdminOptions);

        // Create User button
        JButton btnCreateUser = new JButton("Create User");
        btnCreateUser.setActionCommand("Create User");
        btnCreateUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newUserName = RunClient.askForValidInput("Enter user's name to create", null);
                if (newUserName != null)
                    if (RunClient.gclient.createUser(newUserName, RunClient.userToken, "PASSWORD"))
                        JOptionPane.showMessageDialog(null, "User: " + newUserName + " successfully created");
                    else
                        JOptionPane.showMessageDialog(null, "Failed to create user: " + newUserName + "!");
            }
        });
        GridBagConstraints gbc_btnCreateUser = new GridBagConstraints();
        gbc_btnCreateUser.anchor = GridBagConstraints.WEST;
        gbc_btnCreateUser.insets = new Insets(0, 0, 5, 0);
        gbc_btnCreateUser.gridx = 0;
        gbc_btnCreateUser.gridy = 8;
        gbc_btnCreateUser.fill = GridBagConstraints.HORIZONTAL;
        if (RunClient.userToken != null)
            if (!RunClient.userToken.getGroups().contains("ADMIN"))
                btnCreateUser.setEnabled(false);
        leftPanel.add(btnCreateUser, gbc_btnCreateUser);

        // Delete User button
        JButton btnDeleteUser = new JButton("Delete User");
        btnDeleteUser.setActionCommand("Delete User");
        btnDeleteUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String deleteUserName = RunClient.askForValidInput("Enter user's name to delete", null);
                if (deleteUserName != null)
                    if (!deleteUserName.equals(RunClient.userToken.getSubject())) {
                        int reply = JOptionPane.showConfirmDialog(null, "Really delete user: " + deleteUserName,
                                                                  "WARNING", JOptionPane.YES_NO_OPTION);
                        if (reply == JOptionPane.YES_OPTION)
                            if (RunClient.gclient.deleteUser(deleteUserName, RunClient.userToken))
                                JOptionPane
                                        .showMessageDialog(null, "User: " + deleteUserName + " successfully deleted");
                            else
                                JOptionPane.showMessageDialog(null, "Failed to delete user: " + deleteUserName + "!");
                    } else
                        JOptionPane.showMessageDialog(null, "You can't delete yourself you idiot");
            }
        });
        GridBagConstraints gbc_btnDeleteUser = new GridBagConstraints();
        gbc_btnDeleteUser.anchor = GridBagConstraints.WEST;
        gbc_btnDeleteUser.insets = new Insets(0, 0, 5, 0);
        gbc_btnDeleteUser.gridx = 0;
        gbc_btnDeleteUser.gridy = 9;
        gbc_btnDeleteUser.fill = GridBagConstraints.HORIZONTAL;
        if (RunClient.userToken != null)
            if (!RunClient.userToken.getGroups().contains("ADMIN"))
                btnDeleteUser.setEnabled(false);
        leftPanel.add(btnDeleteUser, gbc_btnDeleteUser);

        return leftPanel;
    }
}
