import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * MailGUI: the Gmail-looking window for the mail system.
 * Run this class (it has main). It creates ONE MailServer and every
 * window you open is a different MailClient talking to that server.
 */
public class MailGUI extends JFrame {

    // ---------- Gmail colours ----------
    static final Color BG = new Color(0xF6F8FC);
    static final Color SEARCH_BG = new Color(0xEAF1FB);
    static final Color COMPOSE_BG = new Color(0xC2E7FF);
    static final Color COMPOSE_HOVER = new Color(0xB1DCF7);
    static final Color COMPOSE_TEXT = new Color(0x001D35);
    static final Color SELECTED = new Color(0xD3E3FD);
    static final Color NAV_HOVER = new Color(0xE8EBF1);
    static final Color ROW_READ = new Color(0xF2F6FC);
    static final Color ROW_HOVER = new Color(0xE6ECF7);
    static final Color TEXT = new Color(0x1F1F1F);
    static final Color GRAY = new Color(0x5F6368);
    static final Color LINE = new Color(0xDADCE0);
    static final Color BLUE = new Color(0x0B57D0);
    static final Color RED = new Color(0xD93025);
    static final Color STAR = new Color(0xF4B400);
    static final String FONT = "Segoe UI";

    static final String[] FOLDERS = {"Inbox", "Starred", "Sent", "Trash"};

    private static int openWindows = 0;

    // ---------- model ----------
    private final MailServer server;
    private MailClient client;
    private String folder = "Inbox";
    private boolean reading = false;
    private MailItem openedItem;
    private String lastSignature = "";

    // ---------- shared widgets ----------
    private final CardLayout rootCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards);
    private javax.swing.Timer refreshTimer;
    private javax.swing.Timer snackTimer;

    // login
    private PlaceholderField loginField;
    private PlaceholderPassword loginPass;
    private JLabel loginError;

    // create account
    private PlaceholderField suFirst, suLast, suUser;
    private PlaceholderPassword suPass, suConfirm;
    private JLabel suError;
    private final ReqRow[] userReq = new ReqRow[4];
    private final ReqRow[] passReq = new ReqRow[3];
    private JPanel listHeader;

    // mail screen
    private JPanel sidebar;
    private NavButton[] navButtons;
    private PlaceholderField searchField;
    private AvatarButton avatar;
    private final DefaultListModel<MailItem> listModel = new DefaultListModel<MailItem>();
    private JList<MailItem> mailList;
    private int hoverIndex = -1;
    private final CardLayout contentCards = new CardLayout();
    private JPanel content;
    private JLabel listTitle;
    private JButton emptyTrashBtn;
    private JLabel emptyLabel;
    private JLabel snackbar;

    // reader
    private JLabel rSubject, rFrom, rTo;
    private JTextArea rBody;
    private StarButton rStar;
    private PillButton rReply, rForward;

    // =====================================================================
    //  Window setup
    // =====================================================================

    public MailGUI(MailServer server) {
        this.server = server;
        setTitle("Gmail");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // CHANGED: window size fits the screen instead of a fixed 1180 x 740
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1180, (int) (scr.width * 0.85)),
                Math.min(740, (int) (scr.height * 0.85)));
        setMinimumSize(new Dimension(600, 400));

        setLocation(120 + openWindows * 34, 60 + openWindows * 34);
        openWindows++;

        root.add(buildLoginPanel(), "login");
        root.add(buildSignupPanel(), "signup");
        root.add(buildMailPanel(), "mail");
        setContentPane(root);
        rootCards.show(root, "login");

        refreshTimer = new javax.swing.Timer(1500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                poll();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                refreshTimer.stop();
                openWindows--;
                if (openWindows <= 0) {
                    System.exit(0);
                }
            }
        });
    }

    // =====================================================================
    //  LOGIN SCREEN
    // =====================================================================

    /** A rounded text box (optionally with a grey suffix like @gmail.com). */
    private RoundedPanel boxed(JTextField field, String suffix) {
        field.setFont(new Font(FONT, Font.PLAIN, 16));
        field.setBorder(new EmptyBorder(0, 0, 0, 0));
        RoundedPanel box = new RoundedPanel(new BorderLayout(8, 0), Color.WHITE, 8, new Color(0x747775));
        box.setBorder(new EmptyBorder(14, 14, 14, 14));
        box.add(field, BorderLayout.CENTER);
        if (suffix != null) {
            box.add(label(suffix, 15, Font.PLAIN, GRAY), BorderLayout.EAST);
        }
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    /**
     * CHANGED (new): wraps a card so it is centred, never wider than maxWidth,
     * shrinks on narrow windows, and scrolls only when the window is too short.
     */
    private JComponent responsive(JPanel card, int maxWidth) {
        JPanel page = new ScrollPage(new CenterLayout(maxWidth));
        page.setBackground(BG);
        page.add(card);
        JScrollPane scroll = new JScrollPane(page,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JComponent buildLoginPanel() {
        RoundedPanel card = new RoundedPanel(null, Color.WHITE, 28, LINE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 48, 32, 48));
        // CHANGED: removed card.setPreferredSize(450, 500)

        LogoIcon logo = new LogoIcon(56, 42);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = label("Sign in", 26, Font.PLAIN, TEXT);
        JLabel sub = label("to continue to Gmail", 16, Font.PLAIN, TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginField = new PlaceholderField("Username");
        loginPass = new PlaceholderPassword("Password");

        loginError = label(" ", 13, Font.PLAIN, RED);
        loginError.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton create = linkButton("Create account");
        PillButton next = new PillButton("Sign in", BLUE, Color.WHITE, false);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(create, BorderLayout.WEST);
        buttons.add(next, BorderLayout.EAST);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(18));
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(sub);
        card.add(Box.createVerticalStrut(28));
        card.add(boxed(loginField, MailServer.DOMAIN));
        card.add(Box.createVerticalStrut(14));
        card.add(boxed(loginPass, null));
        card.add(Box.createVerticalStrut(10));
        card.add(loginError);
        card.add(Box.createVerticalStrut(24)); // CHANGED: was vertical glue
        card.add(buttons);

        ActionListener signIn = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSignIn();
            }
        };
        next.addActionListener(signIn);
        loginField.addActionListener(signIn);
        loginPass.addActionListener(signIn);
        create.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSignup();
            }
        });

        return responsive(card, 450); // CHANGED
    }

    private void doSignIn() {
        String name = MailServer.normalize(loginField.getText());
        String pass = new String(loginPass.getPassword());
        if (name.isEmpty()) {
            loginError.setText("Enter a username");
        } else if (!server.userExists(name)) {
            loginError.setText("Couldn't find your Gmail account. Check the username or create an account.");
        } else if (pass.isEmpty()) {
            loginError.setText("Enter a password");
        } else if (!server.checkLogin(name, pass)) {
            loginError.setText("Wrong password. Try again.");
        } else {
            enterMailbox(name);
        }
    }

    // =====================================================================
    //  CREATE ACCOUNT SCREEN
    // =====================================================================

    private JComponent buildSignupPanel() {
        RoundedPanel card = new RoundedPanel(null, Color.WHITE, 28, LINE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 44, 26, 44));
        // CHANGED: removed card.setPreferredSize(520, 800)

        LogoIcon logo = new LogoIcon(44, 33);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = label("Create a Gmail Account", 24, Font.PLAIN, TEXT);
        JLabel sub = label("Fill in your details to get started", 14, Font.PLAIN, GRAY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        suFirst = new PlaceholderField("First name");
        suLast = new PlaceholderField("Last name (optional)");
        suUser = new PlaceholderField("Username");
        suPass = new PlaceholderPassword("Password");
        suConfirm = new PlaceholderPassword("Confirm password");

        JPanel names = new JPanel(new GridLayout(1, 2, 12, 0));
        names.setOpaque(false);
        names.add(boxed(suFirst, null));
        names.add(boxed(suLast, null));
        names.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        names.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] userText = {"3 to 20 characters long",
                "Starts with a letter (not a number)",
                "Only letters, numbers and _ (no spaces, dots or minus signs)",
                "Username is available"};
        String[] passText = {"At least 8 characters",
                "Contains letters and numbers",
                "Both passwords match"};
        for (int i = 0; i < userReq.length; i++) {
            userReq[i] = new ReqRow(userText[i]);
        }
        for (int i = 0; i < passReq.length; i++) {
            passReq[i] = new ReqRow(passText[i]);
        }

        suError = label(" ", 13, Font.PLAIN, RED);
        suError.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton back = linkButton("Sign in instead");
        PillButton create = new PillButton("Create account", BLUE, Color.WHITE, false);
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(back, BorderLayout.WEST);
        buttons.add(create, BorderLayout.EAST);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(12));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(20));
        card.add(names);
        card.add(Box.createVerticalStrut(14));
        card.add(boxed(suUser, MailServer.DOMAIN));
        card.add(Box.createVerticalStrut(6));
        for (int i = 0; i < userReq.length; i++) {
            card.add(userReq[i]);
        }
        card.add(Box.createVerticalStrut(14));
        card.add(boxed(suPass, null));
        card.add(Box.createVerticalStrut(12));
        card.add(boxed(suConfirm, null));
        card.add(Box.createVerticalStrut(6));
        for (int i = 0; i < passReq.length; i++) {
            card.add(passReq[i]);
        }
        card.add(Box.createVerticalStrut(8));
        card.add(suError);
        card.add(Box.createVerticalStrut(16)); // CHANGED: was vertical glue
        card.add(buttons);

        DocumentListener live = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateRequirements(); }
            public void removeUpdate(DocumentEvent e) { updateRequirements(); }
            public void changedUpdate(DocumentEvent e) { updateRequirements(); }
        };
        suUser.getDocument().addDocumentListener(live);
        suPass.getDocument().addDocumentListener(live);
        suConfirm.getDocument().addDocumentListener(live);

        create.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCreateAccount();
            }
        });
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                rootCards.show(root, "login");
            }
        });

        return responsive(card, 520); // CHANGED: replaces old page/scroll/wrap code
    }

    private void showSignup() {
        suFirst.setText("");
        suLast.setText("");
        suUser.setText("");
        suPass.setText("");
        suConfirm.setText("");
        suError.setText(" ");
        updateRequirements();
        rootCards.show(root, "signup");
    }

    /** Ticks the requirement list while the user types. */
    private void updateRequirements() {
        String u = MailServer.normalize(suUser.getText());
        boolean formatOk = MailServer.checkUsername(u) == null;
        userReq[0].set(u.length() >= MailServer.MIN_USERNAME && u.length() <= MailServer.MAX_USERNAME);
        userReq[1].set(!u.isEmpty() && u.charAt(0) >= 'a' && u.charAt(0) <= 'z');
        userReq[2].set(u.matches("[a-z0-9_]+"));
        userReq[3].set(formatOk && !server.userExists(u));

        String p = new String(suPass.getPassword());
        String c = new String(suConfirm.getPassword());
        passReq[0].set(p.length() >= MailServer.MIN_PASSWORD);
        passReq[1].set(p.matches(".*[A-Za-z].*") && p.matches(".*[0-9].*"));
        passReq[2].set(!p.isEmpty() && p.equals(c));
    }

    private void doCreateAccount() {
        String first = suFirst.getText().trim();
        String last = suLast.getText().trim();
        String pass = new String(suPass.getPassword());
        String confirm = new String(suConfirm.getPassword());

        if (first.isEmpty()) {
            suError.setText("Enter your first name.");
            return;
        }
        if (!first.matches("[\\p{L} ]{1,30}") || !last.matches("[\\p{L} ]{0,30}")) {
            suError.setText("Names can only contain letters (max 30).");
            return;
        }
        String err = MailServer.checkUsername(suUser.getText());
        if (err == null && server.userExists(suUser.getText())) {
            err = "That username is taken. Try another.";
        }
        if (err == null) {
            err = MailServer.checkPassword(pass);
        }
        if (err == null && !pass.equals(confirm)) {
            err = "Passwords didn't match. Try again.";
        }
        if (err == null) {
            err = server.registerUser(suUser.getText(), first, last, pass);
        }
        if (err != null) {
            suError.setText("<html>" + esc(err) + "</html>");
            return;
        }
        String name = MailServer.normalize(suUser.getText());
        enterMailbox(name);
        showSnack("Welcome, " + client.getDisplayName() + "! Your address is " + client.getAddress());
    }

    // =====================================================================
    //  MAIL SCREEN
    // =====================================================================

    private JPanel buildMailPanel() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);

        page.add(buildTopBar(), BorderLayout.NORTH);
        page.add(buildSidebar(), BorderLayout.WEST);

        // ----- main white area with rounded corners -----
        content = new JPanel(contentCards);
        content.setOpaque(false);
        content.add(buildListCard(), "list");
        content.add(buildEmptyCard(), "empty");
        content.add(buildReaderCard(), "read");

        RoundedPanel white = new RoundedPanel(new BorderLayout(), Color.WHITE, 24, null);
        listHeader = buildListHeader();
        white.add(listHeader, BorderLayout.NORTH);
        white.add(content, BorderLayout.CENTER);

        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(BG);
        centre.setBorder(new EmptyBorder(0, 0, 8, 16));
        centre.add(white, BorderLayout.CENTER);
        page.add(centre, BorderLayout.CENTER);

        // ----- snackbar ("Message sent.") -----
        snackbar = new JLabel("", SwingConstants.LEFT);
        snackbar.setOpaque(true);
        snackbar.setBackground(new Color(0x323232));
        snackbar.setForeground(Color.WHITE);
        snackbar.setFont(new Font(FONT, Font.PLAIN, 14));
        snackbar.setBorder(new EmptyBorder(12, 24, 12, 24));
        snackbar.setVisible(false);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        south.setOpaque(false);
        south.add(snackbar);
        page.add(south, BorderLayout.SOUTH);

        return page;
    }

    // ---------------- top bar ----------------

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG);
        bar.setBorder(new EmptyBorder(8, 12, 8, 16));

        // left: menu + logo + "Gmail"
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(236, 48));
        MenuButton menu = new MenuButton();
        menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sidebar.setVisible(!sidebar.isVisible());
                revalidate();
            }
        });
        left.add(menu);
        left.add(new LogoIcon(32, 24));
        left.add(label("Gmail", 22, Font.PLAIN, GRAY));
        bar.add(left, BorderLayout.WEST);

        // centre: search
        RoundedPanel search = new RoundedPanel(new BorderLayout(10, 0), SEARCH_BG, 48, null);
        search.setBorder(new EmptyBorder(0, 18, 0, 18));
        search.setPreferredSize(new Dimension(300, 48)); // CHANGED: was 720
        searchField = new PlaceholderField("Search mail");
        searchField.setOpaque(false);
        searchField.setBorder(null);
        searchField.setFont(new Font(FONT, Font.PLAIN, 16));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onSearch(); }
            public void removeUpdate(DocumentEvent e) { onSearch(); }
            public void changedUpdate(DocumentEvent e) { onSearch(); }
        });
        search.add(new SearchIcon(), BorderLayout.WEST);
        search.add(searchField, BorderLayout.CENTER);

        // CHANGED: BorderLayout wrapper so the search bar stretches / shrinks with the window
        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setOpaque(false);
        searchWrap.setBorder(new EmptyBorder(0, 0, 0, 40));
        searchWrap.add(search, BorderLayout.CENTER);
        bar.add(searchWrap, BorderLayout.CENTER);

        // right: avatar with account menu
        avatar = new AvatarButton();
        avatar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAccountMenu();
            }
        });
        bar.add(avatar, BorderLayout.EAST);
        return bar;
    }

    private void showAccountMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem name = new JMenuItem(client == null ? "" : client.getDisplayName());
        name.setEnabled(false);
        JMenuItem who = new JMenuItem(client == null ? "" : client.getAddress());
        who.setEnabled(false);
        JMenuItem another = new JMenuItem("Add another account (new window)");
        another.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new MailGUI(server).setVisible(true);
            }
        });
        JMenuItem out = new JMenuItem("Sign out");
        out.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                signOut();
            }
        });
        menu.add(name);
        menu.add(who);
        menu.addSeparator();
        menu.add(another);
        menu.add(out);
        menu.show(avatar, avatar.getWidth() - 260, avatar.getHeight() + 4);
    }

    // ---------------- sidebar ----------------

    private JPanel buildSidebar() {
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG);
        sidebar.setBorder(new EmptyBorder(8, 8, 8, 8));
        sidebar.setPreferredSize(new Dimension(256, 100));

        ComposeButton compose = new ComposeButton();
        compose.setAlignmentX(Component.LEFT_ALIGNMENT);
        compose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openCompose("", "", "");
            }
        });
        sidebar.add(compose);
        sidebar.add(Box.createVerticalStrut(18));

        navButtons = new NavButton[FOLDERS.length];
        for (int i = 0; i < FOLDERS.length; i++) {
            final String name = FOLDERS[i];
            navButtons[i] = new NavButton(name, i);
            navButtons[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            navButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectFolder(name);
                }
            });
            sidebar.add(navButtons[i]);
        }
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    // ---------------- list ----------------

    /** Folder title + Refresh button. Always visible except while reading a mail. */
    private JPanel buildListHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(8, 20, 8, 16));
        listTitle = label("Inbox", 15, Font.BOLD, TEXT);
        header.add(listTitle, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        emptyTrashBtn = linkButton("Empty Trash now");
        emptyTrashBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                client.emptyTrash();
                refresh();
                showSnack("Trash emptied.");
            }
        });
        PillButton refreshBtn = new PillButton("Refresh", Color.WHITE, BLUE, true, 6, 18);
        refreshBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresh();
                showSnack("Refreshed.");
            }
        });
        actions.add(emptyTrashBtn);
        actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildListCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);

        mailList = new JList<MailItem>(listModel);
        mailList.setCellRenderer(new RowRenderer());
        mailList.setFixedCellHeight(46);
        mailList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mailList.setFocusable(false);
        MouseAdapter mouse = new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (i != hoverIndex) {
                    hoverIndex = i;
                    mailList.setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    mailList.repaint();
                }
            }

            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                mailList.repaint();
            }

            public void mouseClicked(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (i < 0) {
                    return;
                }
                MailItem m = listModel.get(i);
                if (e.getX() < 46) {
                    client.toggleStar(m);
                    refresh();
                } else {
                    openMail(m);
                }
            }
        };
        mailList.addMouseListener(mouse);
        mailList.addMouseMotionListener(mouse);

        JScrollPane scroll = new JScrollPane(mailList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private int indexAt(Point p) {
        int i = mailList.locationToIndex(p);
        if (i < 0) {
            return -1;
        }
        Rectangle r = mailList.getCellBounds(i, i);
        return (r != null && r.contains(p)) ? i : -1;
    }

    private JPanel buildEmptyCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setOpaque(false);
        emptyLabel = label("Your inbox is empty", 18, Font.PLAIN, GRAY);
        card.add(emptyLabel);
        return card;
    }

    // ---------------- reader ----------------

    private JPanel buildReaderCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);

        // toolbar: back / delete / mark unread
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        tools.setOpaque(false);
        JButton back = linkButton("\u2190 Back");
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeReader();
            }
        });
        JButton del = linkButton("Delete");
        del.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteOpened();
            }
        });
        JButton unread = linkButton("Mark as unread");
        unread.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (openedItem != null) {
                    client.markUnread(openedItem);
                }
                closeReader();
            }
        });
        tools.add(back);
        tools.add(del);
        tools.add(unread);
        card.add(tools, BorderLayout.NORTH);

        // header: subject + from/to
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(8, 28, 8, 28));

        rSubject = label(" ", 22, Font.PLAIN, TEXT);
        rFrom = new JLabel(" ");
        rFrom.setFont(new Font(FONT, Font.PLAIN, 14));
        rTo = label(" ", 12, Font.PLAIN, GRAY);
        rStar = new StarButton();
        rStar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (openedItem != null) {
                    client.toggleStar(openedItem);
                    rStar.setOn(openedItem.isStarred());
                }
            }
        });

        JPanel subjRow = new JPanel(new BorderLayout());
        subjRow.setOpaque(false);
        subjRow.add(rSubject, BorderLayout.CENTER);
        subjRow.add(rStar, BorderLayout.EAST);
        subjRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rFrom.setAlignmentX(Component.LEFT_ALIGNMENT);
        rTo.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(subjRow);
        top.add(Box.createVerticalStrut(14));
        top.add(rFrom);
        top.add(rTo);

        rBody = new JTextArea();
        rBody.setEditable(false);
        rBody.setLineWrap(true);
        rBody.setWrapStyleWord(true);
        rBody.setFont(new Font(FONT, Font.PLAIN, 15));
        rBody.setForeground(TEXT);
        rBody.setBorder(new EmptyBorder(8, 28, 8, 28));
        JScrollPane bodyScroll = new JScrollPane(rBody);
        bodyScroll.setBorder(null);

        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.add(top, BorderLayout.NORTH);
        mid.add(bodyScroll, BorderLayout.CENTER);
        card.add(mid, BorderLayout.CENTER);

        // bottom: reply / forward
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(0, 16, 8, 0));
        rReply = new PillButton("\u21A9  Reply", Color.WHITE, TEXT, true);
        rForward = new PillButton("\u21AA  Forward", Color.WHITE, TEXT, true);
        rReply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                replyToOpened();
            }
        });
        rForward.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                forwardOpened();
            }
        });
        bottom.add(rReply);
        bottom.add(rForward);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    // =====================================================================
    //  BEHAVIOUR
    // =====================================================================

    private void enterMailbox(String username) {
        client = new MailClient(server, username);
        avatar.setUser(client.getDisplayName());
        searchField.setText("");
        folder = "Inbox";
        reading = false;
        loginError.setText(" ");
        loginField.setText("");
        loginPass.setText("");
        rootCards.show(root, "mail");
        selectFolder("Inbox");
        refreshTimer.start();
    }

    private void signOut() {
        refreshTimer.stop();
        client = null;
        reading = false;
        openedItem = null;
        listModel.clear();
        loginField.setText("");
        loginPass.setText("");
        loginError.setText(" ");
        setTitle("Gmail");
        rootCards.show(root, "login");
    }

    private void selectFolder(String name) {
        folder = name;
        reading = false;
        openedItem = null;
        if (!searchField.getText().isEmpty()) {
            searchField.setText(""); // triggers refresh via the listener
        }
        for (int i = 0; i < navButtons.length; i++) {
            navButtons[i].setSelectedNav(FOLDERS[i].equals(name));
        }
        listTitle.setText(name);
        emptyTrashBtn.setVisible(name.equals("Trash"));
        setTitle(name + " - " + client.getAddress() + " - Gmail");
        refresh();
    }

    private void onSearch() {
        if (client == null) {
            return;
        }
        reading = false;
        refresh();
    }

    /** Called by the timer: only redraws when something changed. */
    private void poll() {
        if (client == null) {
            return;
        }
        String sig = client.getInbox().size() + ":" + client.countUnread() + ":"
                + client.getSent().size() + ":" + client.getTrash().size();
        if (!sig.equals(lastSignature)) {
            refresh();
        }
    }

    private List<MailItem> visibleItems() {
        List<MailItem> all;
        if (folder.equals("Inbox")) {
            all = client.getInbox();
        } else if (folder.equals("Starred")) {
            all = client.getStarred();
        } else if (folder.equals("Sent")) {
            all = client.getSent();
        } else {
            all = client.getTrash();
        }
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            return all;
        }
        List<MailItem> found = new ArrayList<MailItem>();
        for (MailItem m : all) {
            String hay = (m.getFrom() + " " + m.getTo() + " " + m.getSubject() + " " + m.getMessage()).toLowerCase();
            if (hay.contains(q)) {
                found.add(m);
            }
        }
        return found;
    }

    private void refresh() {
        if (client == null) {
            return;
        }
        lastSignature = client.getInbox().size() + ":" + client.countUnread() + ":"
                + client.getSent().size() + ":" + client.getTrash().size();

        List<MailItem> items = visibleItems();
        listModel.clear();
        for (MailItem m : items) {
            listModel.addElement(m);
        }
        navButtons[0].setCount(client.countUnread());

        if (!reading) {
            String q = searchField.getText().trim();
            if (items.isEmpty()) {
                if (!q.isEmpty()) {
                    emptyLabel.setText("No messages matched your search.");
                } else if (folder.equals("Inbox")) {
                    emptyLabel.setText("Your inbox is empty.");
                } else if (folder.equals("Starred")) {
                    emptyLabel.setText("No starred messages.");
                } else if (folder.equals("Sent")) {
                    emptyLabel.setText("No sent messages.");
                } else {
                    emptyLabel.setText("No conversations in Trash.");
                }
                showCard("empty");
            } else {
                showCard("list");
            }
        }
        mailList.repaint();
    }

    private void openMail(MailItem m) {
        openedItem = m;
        reading = true;
        client.markRead(m);

        boolean mine = m.getFrom().equals(client.getUser());
        rSubject.setText(m.getSubject());
        rFrom.setText("<html><b>" + esc(client.getDisplayName(m.getFrom())) + "</b> &nbsp;<font color='#5F6368'>&lt;"
                + esc(m.getFrom()) + MailServer.DOMAIN + "&gt;</font>"
                + "&nbsp;&nbsp;&nbsp;<font color='#5F6368' size='3'>" + esc(m.getFullTime()) + "</font></html>");
        rTo.setText("to " + (m.getTo().equals(client.getUser()) ? "me" : m.getTo() + MailServer.DOMAIN));
        rBody.setText(m.getMessage());
        rBody.setCaretPosition(0);
        rStar.setOn(m.isStarred());
        rReply.setText(mine ? "\u21A9  Send again" : "\u21A9  Reply");
        showCard("read");
        navButtons[0].setCount(client.countUnread());
        mailList.repaint();
    }

    private void closeReader() {
        reading = false;
        openedItem = null;
        refresh();
    }

    private void deleteOpened() {
        if (openedItem == null) {
            return;
        }
        if (folder.equals("Trash")) {
            client.deleteForever(openedItem);
            showSnack("Conversation deleted forever.");
        } else {
            client.delete(openedItem);
            showSnack("Conversation moved to Trash.");
        }
        closeReader();
    }

    private void replyToOpened() {
        if (openedItem == null) {
            return;
        }
        boolean mine = openedItem.getFrom().equals(client.getUser());
        String to = mine ? openedItem.getTo() : openedItem.getFrom();
        String subj = openedItem.getSubject();
        if (!subj.toLowerCase().startsWith("re:")) {
            subj = "Re: " + subj;
        }
        String quote = "\n\nOn " + openedItem.getFullTime() + ", " + openedItem.getFrom() + MailServer.DOMAIN
                + " wrote:\n> " + openedItem.getMessage().replace("\n", "\n> ");
        openCompose(to, subj, quote);
    }

    private void forwardOpened() {
        if (openedItem == null) {
            return;
        }
        String subj = openedItem.getSubject();
        if (!subj.toLowerCase().startsWith("fwd:")) {
            subj = "Fwd: " + subj;
        }
        String body = "\n\n---------- Forwarded message ---------\nFrom: " + openedItem.getFrom() + MailServer.DOMAIN
                + "\nDate: " + openedItem.getFullTime() + "\nSubject: " + openedItem.getSubject()
                + "\nTo: " + openedItem.getTo() + MailServer.DOMAIN + "\n\n" + openedItem.getMessage();
        openCompose("", subj, body);
    }

    private void openCompose(String to, String subject, String body) {
        ComposeDialog d = new ComposeDialog(client, to, subject, body);
        d.setVisible(true);
    }

    private void showCard(String name) {
        contentCards.show(content, name);
        listHeader.setVisible(!name.equals("read"));
    }

    private void showSnack(String text) {
        snackbar.setText(text);
        snackbar.setVisible(true);
        if (snackTimer != null) {
            snackTimer.stop();
        }
        snackTimer = new javax.swing.Timer(3000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                snackbar.setVisible(false);
            }
        });
        snackTimer.setRepeats(false);
        snackTimer.start();
    }

    // =====================================================================
    //  COMPOSE WINDOW
    // =====================================================================

    private class ComposeDialog extends JDialog {
        private final MailClient sender;
        private final JTextField toField = new JTextField();
        private final JTextField subjectField = new JTextField();
        private final JTextArea bodyArea = new JTextArea();
        private final JLabel error = new JLabel(" ");

        ComposeDialog(MailClient sender, String to, String subject, String body) {
            super(MailGUI.this, "New Message", false);
            this.sender = sender;

            // CHANGED: dialog size also fits inside the parent window / screen
            int dw = Math.min(580, Math.max(360, MailGUI.this.getWidth() - 40));
            int dh = Math.min(540, Math.max(380, MailGUI.this.getHeight() - 40));
            setSize(dw, dh);
            setMinimumSize(new Dimension(360, 380));
            setLocation(Math.max(0, MailGUI.this.getX() + MailGUI.this.getWidth() - dw - 40),
                    Math.max(0, MailGUI.this.getY() + MailGUI.this.getHeight() - dh - 40));

            JPanel main = new JPanel(new BorderLayout());
            main.setBackground(Color.WHITE);

            // header bar
            JPanel head = new JPanel(new BorderLayout());
            head.setBackground(new Color(0xF2F6FC));
            head.setBorder(new EmptyBorder(10, 16, 10, 16));
            head.add(label("New Message", 14, Font.BOLD, TEXT), BorderLayout.WEST);
            main.add(head, BorderLayout.NORTH);

            // fields
            JPanel fields = new JPanel();
            fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
            fields.setOpaque(false);
            fields.add(fieldRow("To", toField));
            fields.add(fieldRow("Subject", subjectField));

            bodyArea.setFont(new Font(FONT, Font.PLAIN, 15));
            bodyArea.setLineWrap(true);
            bodyArea.setWrapStyleWord(true);
            bodyArea.setBorder(new EmptyBorder(10, 16, 10, 16));
            JScrollPane scroll = new JScrollPane(bodyArea);
            scroll.setBorder(null);

            JPanel mid = new JPanel(new BorderLayout());
            mid.setOpaque(false);
            mid.add(fields, BorderLayout.NORTH);
            mid.add(scroll, BorderLayout.CENTER);
            main.add(mid, BorderLayout.CENTER);

            // bottom: error + send
            error.setForeground(RED);
            error.setFont(new Font(FONT, Font.PLAIN, 13));
            error.setBorder(new EmptyBorder(0, 16, 6, 16));

            PillButton send = new PillButton("Send", BLUE, Color.WHITE, false);
            JButton discard = linkButton("Discard");
            discard.setForeground(GRAY);

            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(4, 16, 14, 16));
            row.add(send, BorderLayout.WEST);
            row.add(discard, BorderLayout.EAST);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            bottom.add(error, BorderLayout.NORTH);
            bottom.add(row, BorderLayout.SOUTH);
            main.add(bottom, BorderLayout.SOUTH);

            setContentPane(main);

            toField.setText(to);
            subjectField.setText(subject);
            bodyArea.setText(body);
            bodyArea.setCaretPosition(0);

            send.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doSend();
                }
            });
            discard.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }

        private JPanel fieldRow(String name, JTextField field) {
            field.setBorder(new EmptyBorder(0, 8, 0, 0));
            field.setFont(new Font(FONT, Font.PLAIN, 15));
            JLabel l = label(name, 14, Font.PLAIN, GRAY);
            l.setPreferredSize(new Dimension(60, 20));
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(Color.WHITE);
            p.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE8EAED)),
                    new EmptyBorder(10, 16, 10, 16)));
            p.add(l, BorderLayout.WEST);
            p.add(field, BorderLayout.CENTER);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            return p;
        }

        private void doSend() {
            boolean ok = sender.sendMailItem(toField.getText(), subjectField.getText(), bodyArea.getText());
            if (!ok) {
                error.setText("<html>" + esc(sender.getLastError()) + "</html>");
                return;
            }
            dispose();
            showSnack("Message sent.");
            refresh();
        }
    }

    // =====================================================================
    //  HELPERS
    // =====================================================================

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, style, size));
        l.setForeground(color);
        return l;
    }

    static JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font(FONT, Font.BOLD, 14));
        b.setForeground(BLUE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static Path2D star(double cx, double cy, double outer, double inner) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double a = Math.PI / 2 + i * Math.PI / 5;
            double x = cx + r * Math.cos(a);
            double y = cy - r * Math.sin(a);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.closePath();
        return p;
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    static String clip(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) {
            return s;
        }
        String dots = "...";
        while (s.length() > 0 && fm.stringWidth(s + dots) > width) {
            s = s.substring(0, s.length() - 1);
        }
        return s + dots;
    }

    // =====================================================================
    //  CUSTOM COMPONENTS (Gmail look)
    // =====================================================================

    /**
     * CHANGED (new): lays out ONE child centred, at most maxW wide.
     * On a narrow window the child shrinks to fit (16px margin each side).
     */
    static class CenterLayout implements LayoutManager {
        private final int maxW;

        CenterLayout(int maxW) {
            this.maxW = maxW;
        }

        public void addLayoutComponent(String n, Component c) {
        }

        public void removeLayoutComponent(Component c) {
        }

        public Dimension preferredLayoutSize(Container p) {
            Component c = p.getComponent(0);
            return new Dimension(maxW + 32, c.getPreferredSize().height + 32);
        }

        public Dimension minimumLayoutSize(Container p) {
            return new Dimension(0, p.getComponent(0).getPreferredSize().height + 32);
        }

        public void layoutContainer(Container p) {
            Component c = p.getComponent(0);
            int w = Math.min(maxW, p.getWidth() - 32);
            int h = c.getPreferredSize().height;
            c.setBounds((p.getWidth() - w) / 2, Math.max(16, (p.getHeight() - h) / 2), w, h);
        }
    }

    /**
     * CHANGED (new): panel that always matches the viewport width and only
     * scrolls vertically when the content is taller than the window.
     */
    static class ScrollPage extends JPanel implements Scrollable {
        ScrollPage(LayoutManager lm) {
            super(lm);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 16;
        }

        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport
                    && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** Panel with rounded corners (and optional border). */
    static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color border;

        RoundedPanel(LayoutManager lm, Color bg, int arc, Color border) {
            super(lm);
            this.arc = arc;
            this.border = border;
            setOpaque(false);
            setBackground(bg);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
        }

        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
            super.paintChildren(g2);
            g2.dispose();
        }
    }

    /** Rounded button (filled or outlined). */
    static class PillButton extends JButton {
        private final Color base;
        private final boolean outlined;

        PillButton(String text, Color bg, Color fg, boolean outlined) {
            this(text, bg, fg, outlined, 10, 26);
        }

        PillButton(String text, Color bg, Color fg, boolean outlined, int vpad, int hpad) {
            super(text);
            this.base = bg;
            this.outlined = outlined;
            setForeground(fg);
            setFont(new Font(FONT, Font.BOLD, 14));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(vpad, hpad, vpad, hpad));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            boolean over = getModel().isRollover();
            Color fill = outlined ? (over ? ROW_READ : base) : (over ? base.darker() : base);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            if (outlined) {
                g2.setColor(LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Text field that shows grey hint text while empty. */
    static class PlaceholderField extends JTextField {
        private final String hint;

        PlaceholderField(String hint) {
            this.hint = hint;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = smooth(g);
                g2.setColor(new Color(0x80868B));
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    /** Password box that shows grey hint text while empty. */
    static class PlaceholderPassword extends JPasswordField {
        private final String hint;

        PlaceholderPassword(String hint) {
            this.hint = hint;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0) {
                Graphics2D g2 = smooth(g);
                g2.setColor(new Color(0x80868B));
                g2.setFont(getFont());
                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, in.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    /** One line of the requirement list: grey circle -> green tick when met. */
    static class ReqRow extends JComponent {
        private static final Color GREEN = new Color(0x188038);
        private final String text;
        private boolean ok = false;

        ReqRow(String text) {
            this.text = text;
            setPreferredSize(new Dimension(380, 22));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void set(boolean met) {
            if (met != ok) {
                ok = met;
                repaint();
            }
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int cy = getHeight() / 2;
            if (ok) {
                g2.setColor(GREEN);
                g2.fillOval(2, cy - 7, 14, 14);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(5, cy, 8, cy + 3);
                g2.drawLine(8, cy + 3, 13, cy - 3);
            } else {
                g2.setColor(new Color(0x9AA0A6));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(3, cy - 6, 12, 12);
            }
            g2.setColor(ok ? GREEN : GRAY);
            g2.setFont(new Font(FONT, Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 26, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** The Gmail "M" envelope logo. */
    static class LogoIcon extends JComponent {
        LogoIcon(int w, int h) {
            setPreferredSize(new Dimension(w, h));
            setMaximumSize(new Dimension(w, h));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.scale(getWidth() / 32.0, getHeight() / 24.0);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, 32, 24, 4, 4);
            g2.setColor(new Color(0xE0E0E0));
            g2.drawRoundRect(0, 0, 31, 23, 4, 4);
            g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0x4285F4));
            g2.drawLine(4, 20, 4, 5);
            g2.setColor(new Color(0x34A853));
            g2.drawLine(28, 20, 28, 5);
            g2.setColor(new Color(0xEA4335));
            Path2D m = new Path2D.Double();
            m.moveTo(4, 5);
            m.lineTo(16, 14);
            m.lineTo(28, 5);
            g2.draw(m);
            g2.dispose();
        }
    }

    /** Hamburger button. */
    static class MenuButton extends JButton {
        MenuButton() {
            setPreferredSize(new Dimension(40, 40));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            if (getModel().isRollover()) {
                g2.setColor(NAV_HOVER);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            }
            g2.setColor(GRAY);
            g2.setStroke(new BasicStroke(2f));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            for (int i = -1; i <= 1; i++) {
                g2.drawLine(cx - 8, cy + i * 6, cx + 8, cy + i * 6);
            }
            g2.dispose();
        }
    }

    /** Magnifier icon inside the search bar. */
    static class SearchIcon extends JComponent {
        SearchIcon() {
            setPreferredSize(new Dimension(24, 24));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(GRAY);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = getHeight() / 2;
            g2.drawOval(3, cy - 9, 12, 12);
            g2.drawLine(13, cy + 1, 20, cy + 8);
            g2.dispose();
        }
    }

    /** Round account button showing the first letter of the username. */
    static class AvatarButton extends JButton {
        private String user = "?";

        AvatarButton() {
            setPreferredSize(new Dimension(40, 40));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setUser(String u) {
            this.user = u;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            Color[] palette = {new Color(0x1A73E8), new Color(0xD93025), new Color(0x188038),
                    new Color(0xE37400), new Color(0x9334E6), new Color(0x007B83)};
            g2.setColor(palette[Math.abs(user.hashCode()) % palette.length]);
            g2.fillOval(2, 2, getWidth() - 5, getHeight() - 5);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(FONT, Font.BOLD, 17));
            String s = user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Big light-blue Compose button with a pencil. */
    static class ComposeButton extends JButton {
        ComposeButton() {
            setPreferredSize(new Dimension(148, 56));
            setMaximumSize(new Dimension(148, 56));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(getModel().isRollover() ? COMPOSE_HOVER : COMPOSE_BG);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(COMPOSE_TEXT);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(20, 36, 33, 21);          // pencil body
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawLine(18, 38, 23, 37);          // pencil tip
            g2.setFont(new Font(FONT, Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("Compose", 50, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Star toggle used in the reader. */
    static class StarButton extends JButton {
        private boolean on;

        StarButton() {
            setPreferredSize(new Dimension(40, 36));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setOn(boolean v) {
            on = v;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            Path2D s = star(getWidth() / 2.0, getHeight() / 2.0 + 1, 10, 4.6);
            if (on) {
                g2.setColor(STAR);
                g2.fill(s);
            } else {
                g2.setColor(GRAY);
                g2.setStroke(new BasicStroke(1.7f));
                g2.draw(s);
            }
            g2.dispose();
        }
    }

    /** Sidebar entry (Inbox / Starred / Sent / Trash). */
    static class NavButton extends JButton {
        private final String name;
        private final int type;
        private int count = 0;
        private boolean selectedNav = false;

        NavButton(String name, int type) {
            this.name = name;
            this.type = type;
            setPreferredSize(new Dimension(240, 36));
            setMaximumSize(new Dimension(240, 36));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setCount(int c) {
            count = c;
            repaint();
        }

        void setSelectedNav(boolean s) {
            selectedNav = s;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            if (selectedNav) {
                g2.setColor(SELECTED);
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            } else if (getModel().isRollover()) {
                g2.setColor(NAV_HOVER);
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            }

            Color ink = selectedNav ? COMPOSE_TEXT : GRAY;
            g2.setColor(ink);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 28;
            int cy = h / 2;
            if (type == 0) {            // inbox tray
                g2.drawRoundRect(cx - 9, cy - 8, 18, 16, 3, 3);
                Path2D p = new Path2D.Double();
                p.moveTo(cx - 9, cy + 1);
                p.lineTo(cx - 4, cy + 1);
                p.lineTo(cx - 3, cy + 4);
                p.lineTo(cx + 3, cy + 4);
                p.lineTo(cx + 4, cy + 1);
                p.lineTo(cx + 9, cy + 1);
                g2.draw(p);
            } else if (type == 1) {     // star
                Path2D s = star(cx, cy + 1, 10, 4.6);
                if (selectedNav) {
                    g2.fill(s);
                } else {
                    g2.draw(s);
                }
            } else if (type == 2) {     // paper plane
                Path2D p = new Path2D.Double();
                p.moveTo(cx - 9, cy - 8);
                p.lineTo(cx + 9, cy);
                p.lineTo(cx - 9, cy + 8);
                p.lineTo(cx - 6, cy);
                p.closePath();
                if (selectedNav) {
                    g2.fill(p);
                } else {
                    g2.draw(p);
                }
            } else {                    // trash can
                g2.drawRect(cx - 6, cy - 3, 12, 12);
                g2.drawLine(cx - 8, cy - 5, cx + 8, cy - 5);
                g2.drawLine(cx - 3, cy - 8, cx + 3, cy - 8);
                g2.drawLine(cx - 2, cy, cx - 2, cy + 6);
                g2.drawLine(cx + 2, cy, cx + 2, cy + 6);
            }

            boolean bold = selectedNav || (type == 0 && count > 0);
            g2.setColor(selectedNav ? COMPOSE_TEXT : TEXT);
            g2.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, 14));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(name, 56, (h - fm.getHeight()) / 2 + fm.getAscent());

            if (type == 0 && count > 0) {
                String c = String.valueOf(count);
                g2.setFont(new Font(FONT, Font.BOLD, 12));
                fm = g2.getFontMetrics();
                g2.setColor(selectedNav ? COMPOSE_TEXT : TEXT);
                g2.drawString(c, w - 18 - fm.stringWidth(c), (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    /** Draws one row of the mail list exactly like Gmail. */
    private class RowRenderer extends JPanel implements ListCellRenderer<MailItem> {
        private MailItem item;
        private boolean hover;

        RowRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList<? extends MailItem> list, MailItem value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            item = value;
            hover = (index == hoverIndex);
            return this;
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth();
            int h = getHeight();
            boolean unread = !item.isRead();

            g2.setColor(hover ? ROW_HOVER : (unread ? Color.WHITE : ROW_READ));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(0xE8EAED));
            g2.drawLine(0, h - 1, w, h - 1);

            // star
            Path2D s = star(26, h / 2.0 + 1, 9, 4.2);
            if (item.isStarred()) {
                g2.setColor(STAR);
                g2.fill(s);
            } else {
                g2.setColor(new Color(0x9AA0A6));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(s);
            }

            Font normal = new Font(FONT, Font.PLAIN, 14);
            Font bold = new Font(FONT, Font.BOLD, 14);
            Font f = unread ? bold : normal;
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int base = (h - fm.getHeight()) / 2 + fm.getAscent();

            // sender (or "To: x" in Sent)
            String who = folder.equals("Sent") ? "To: " + (item.getTo().equals(client.getUser()) ? "me" : client.getDisplayName(item.getTo()))
                    : client.getDisplayName(item.getFrom());
            if (!folder.equals("Sent") && item.getFrom().equals(client.getUser())) {
                who = "me";
            }
            g2.setColor(TEXT);
            g2.drawString(clip(who, fm, 190), 56, base);

            // time (right)
            String time = item.getListTime();
            int tw = fm.stringWidth(time);
            g2.setColor(unread ? TEXT : GRAY);
            g2.drawString(time, w - 20 - tw, base);

            // subject - snippet
            int x = 270;
            int avail = w - x - tw - 50;
            if (avail > 40) {
                String subj = clip(item.getSubject(), fm, avail);
                g2.setColor(TEXT);
                g2.drawString(subj, x, base);
                int used = fm.stringWidth(subj);
                if (avail - used > 60 && !item.getSnippet().isEmpty()) {
                    g2.setFont(normal);
                    FontMetrics nf = g2.getFontMetrics();
                    g2.setColor(GRAY);
                    g2.drawString(clip(" - " + item.getSnippet(), nf, avail - used), x + used, base);
                }
            }
            g2.dispose();
        }
    }

    // =====================================================================
    //  MAIN
    // =====================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ignored) {
                }

                // ONE server shared by every client window.
                // It starts completely empty: create an account to begin.
                MailServer server = new MailServer();

                new MailGUI(server).setVisible(true);
            }
        });
    }
}