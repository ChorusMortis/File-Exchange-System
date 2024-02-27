import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;

public class ClientView extends JFrame {
    private JPanel mainPanel;
    private JTextField chatTitle;
    private JTextArea chatLogs;
    private JTextField chatbox;

    public ClientView() {
        this.addFonts();
        this.setTitle("File Exchange System - Client");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1280, 720);
        this.setResizable(false);
        this.setLayout(new BorderLayout());
        this.getContentPane().setBackground(Color.decode("#eef7ff"));
        initializeGUI();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        chatbox.requestFocusInWindow();
    }

    public void setChatLogsText(String s) {
        chatLogs.setText(s);
    }

    public void appendChatLogsText(String s) {
        if (s == null) {
            return;
        }
        chatLogs.append(s + "\n");
    }

    public void addChatboxActionListener(ActionListener actionListener) {
        chatbox.addActionListener(actionListener);
    }

    public void setChatboxText(String s) {
        chatbox.setText(s);
    }

    public String getChatboxText() {
        return chatbox.getText();
    }

    private void addFonts() {
        // import custom fonts

        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("fonts\\Poppins-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("fonts\\noir-pro-bold.ttf")));
        } catch (IOException|FontFormatException e) {
            e.printStackTrace();
        }
    }

    private void initializeGUI() {
        // create main panel for window
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.decode("#eef7ff"));
        mainPanel.setPreferredSize(new Dimension(740, 720));

        // create panel wherein chat logs and chatbox are contained
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));

        // create title panel
        chatTitle = new JTextField("FILE EXCHANGE SYSTEM");
        chatTitle.setFont(new Font("Poppins", Font.PLAIN, 30));
        chatTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatTitle.setEditable(false);
        chatTitle.setHorizontalAlignment(JTextField.CENTER);
        chatTitle.setBackground(Color.decode("#001836"));
        chatTitle.setForeground(Color.decode("#eef7ff"));

        // display text for client here
        chatLogs = new JTextArea();
        DefaultCaret chatLogsCaret = (DefaultCaret) chatLogs.getCaret();
        chatLogsCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        chatLogs.setFont(new Font("Consolas", Font.PLAIN, 16));
        chatLogs.setLineWrap(true);
        chatLogs.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatLogs.setEditable(false);
        chatLogs.setBackground(Color.decode("#eef7ff"));
        chatLogs.setForeground(Color.decode("#001836"));

        // scroll bar for scrolling text area
        JScrollPane scrollBar = new JScrollPane(chatLogs);
        scrollBar.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollBar.setForeground(Color.decode("#002e59"));
        // scrollBar.setPreferredSize(new Dimension(50, 670));
        chatPanel.add(scrollBar, BorderLayout.CENTER);

        // border for chatbox
        JPanel chatboxBorder = new JPanel(new BorderLayout());
        chatboxBorder.setBorder(BorderFactory.createLineBorder(Color.WHITE));

        // chatbox where commands are entered
        JPanel chatboxPanel = new JPanel(new BorderLayout());
        chatbox = new JTextField("Enter command");
        chatbox.setFont(new Font("Poppins", Font.PLAIN, 16));
        chatbox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatbox.setBackground(Color.decode("#001836"));
        chatbox.setForeground(Color.decode("#eef7ff"));
        chatboxBorder.add(chatbox, BorderLayout.CENTER);
        chatboxPanel.add(chatboxBorder, BorderLayout.SOUTH);

        // modify text and appearance of chatbox depending on focus
        chatbox.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (chatbox.getText().equals("Enter command")) {
                    chatbox.setText("");
                    chatboxPanel.setForeground(Color.decode("#eef7ff"));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (chatbox.getText().isEmpty()) {
                    chatbox.setText("Enter command");
                    chatbox.setForeground(Color.decode("#eef7ff"));
                }
            }
        });

        // add elements to main panel
        chatPanel.add(chatboxPanel, BorderLayout.SOUTH);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(chatTitle, BorderLayout.NORTH);
        this.add(mainPanel);
    }
}
