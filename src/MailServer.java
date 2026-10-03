import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MailServer: one server object is shared by all mail clients.
 * It keeps every user's account (name, password) and mailbox
 * (inbox, sent, trash) and passes mail from one client to another.
 * A new server starts completely empty - no users and no mail.
 */
public class MailServer {

    /** The domain shown after every username. */
    public static final String DOMAIN = "@gmail.com";

    public static final int MIN_USERNAME = 3;
    public static final int MAX_USERNAME = 20;
    public static final int MIN_PASSWORD = 8;

    /** One user's account details and folders. */
    private static class Account {
        String firstName;
        String lastName;
        String password;
        List<MailItem> inbox = new ArrayList<MailItem>();
        List<MailItem> sent = new ArrayList<MailItem>();
        List<MailItem> trash = new ArrayList<MailItem>();

        Account(String firstName, String lastName, String password) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.password = password;
        }
    }

    // username -> account
    private Map<String, Account> accounts;

    /** Create a new, empty mail server. */
    public MailServer() {
        accounts = new LinkedHashMap<String, Account>();
    }

    // ------------------------------------------------------------------
    //  Validation (static so the GUI can check while the user types)
    // ------------------------------------------------------------------

    /**
     * Clean up a username: trim, lower-case and remove "@gmail.com".
     * "  Bob@Gmail.com " becomes "bob".
     * Addresses with any other domain are kept as they are (so they
     * will simply not be found).
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim().toLowerCase();
        if (n.endsWith(DOMAIN)) {
            n = n.substring(0, n.length() - DOMAIN.length());
        }
        return n;
    }

    /**
     * Check a username against the rules.
     *
     * @return null if the username is fine, otherwise the reason it is not.
     */
    public static String checkUsername(String raw) {
        String u = normalize(raw);
        if (u.isEmpty()) {
            return "Enter a username.";
        }
        // integers, decimals and negative values such as 42, 3.14, -7, +5
        if (u.matches("[-+]?([0-9]+\\.?[0-9]*|\\.[0-9]+)")) {
            return "A username can't be a number (no negative or decimal values like -5, 3.14 or 42).";
        }
        if (u.length() < MIN_USERNAME) {
            return "Username must be at least " + MIN_USERNAME + " characters.";
        }
        if (u.length() > MAX_USERNAME) {
            return "Username can be at most " + MAX_USERNAME + " characters.";
        }
        char first = u.charAt(0);
        if (first < 'a' || first > 'z') {
            return "Username must start with a letter.";
        }
        if (!u.matches("[a-z0-9_]+")) {
            return "Use only letters, numbers and underscore (_). No spaces, dots, dashes or symbols.";
        }
        return null;
    }

    /**
     * Check a password against the rules.
     *
     * @return null if the password is fine, otherwise the reason it is not.
     */
    public static String checkPassword(String p) {
        if (p == null || p.isEmpty()) {
            return "Enter a password.";
        }
        if (p.length() < MIN_PASSWORD) {
            return "Password must be at least " + MIN_PASSWORD + " characters.";
        }
        if (!p.matches(".*[A-Za-z].*") || !p.matches(".*[0-9].*")) {
            return "Password must contain both letters and numbers.";
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Accounts
    // ------------------------------------------------------------------

    /**
     * Create a new account.
     *
     * @return null if the account was created, otherwise the reason it
     *         was refused (bad username, username taken, bad password...).
     */
    public String registerUser(String username, String firstName, String lastName, String password) {
        if (firstName == null || firstName.trim().isEmpty()) {
            return "Enter your first name.";
        }
        String err = checkUsername(username);
        if (err != null) {
            return err;
        }
        String user = normalize(username);
        if (accounts.containsKey(user)) {
            return "That username is taken. Try another.";
        }
        err = checkPassword(password);
        if (err != null) {
            return err;
        }
        String last = lastName == null ? "" : lastName.trim();
        accounts.put(user, new Account(firstName.trim(), last, password));
        return null;
    }

    /** @return true if this username has an account. */
    public boolean userExists(String username) {
        return accounts.containsKey(normalize(username));
    }

    /** @return true if the username exists and the password matches. */
    public boolean checkLogin(String username, String password) {
        Account a = accounts.get(normalize(username));
        return a != null && a.password.equals(password);
    }

    /** @return "First Last" for a user, or the username if unknown. */
    public String getDisplayName(String username) {
        Account a = accounts.get(normalize(username));
        if (a == null) {
            return normalize(username);
        }
        return (a.firstName + " " + a.lastName).trim();
    }

    // ------------------------------------------------------------------
    //  Mail
    // ------------------------------------------------------------------

    /**
     * Receive a mail item and deliver it to the recipient.
     * The sender keeps the original in "Sent"; the recipient gets a copy
     * in "Inbox". Sender and recipient may be the same person.
     *
     * @return false if the sender or the recipient does not exist.
     */
    public boolean post(MailItem item) {
        Account sender = accounts.get(normalize(item.getFrom()));
        Account receiver = accounts.get(normalize(item.getTo()));
        if (sender == null || receiver == null) {
            return false;
        }
        item.setRead(true); // you have obviously "read" what you sent
        sender.sent.add(0, item);
        receiver.inbox.add(0, item.copyForRecipient());
        return true;
    }

    /** @return how many mail items are in the user's inbox. */
    public int howManyMailItems(String user) {
        Account box = accounts.get(normalize(user));
        return box == null ? 0 : box.inbox.size();
    }

    /** @return how many inbox mails the user has not read yet. */
    public int howManyUnread(String user) {
        int count = 0;
        for (MailItem m : getInbox(user)) {
            if (!m.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Return the next unread mail for this user (newest first)
     * and mark it as read. Returns null if there is nothing new.
     */
    public MailItem getNextMailItem(String user) {
        for (MailItem m : getInbox(user)) {
            if (!m.isRead()) {
                m.setRead(true);
                return m;
            }
        }
        return null;
    }

    /** @return a copy of the user's inbox list (newest first). */
    public List<MailItem> getInbox(String user) {
        Account box = accounts.get(normalize(user));
        return box == null ? new ArrayList<MailItem>() : new ArrayList<MailItem>(box.inbox);
    }

    /** @return a copy of the user's sent list (newest first). */
    public List<MailItem> getSent(String user) {
        Account box = accounts.get(normalize(user));
        return box == null ? new ArrayList<MailItem>() : new ArrayList<MailItem>(box.sent);
    }

    /** @return a copy of the user's trash list. */
    public List<MailItem> getTrash(String user) {
        Account box = accounts.get(normalize(user));
        return box == null ? new ArrayList<MailItem>() : new ArrayList<MailItem>(box.trash);
    }

    /** @return every starred mail in inbox and sent (not trash). */
    public List<MailItem> getStarred(String user) {
        List<MailItem> result = new ArrayList<MailItem>();
        for (MailItem m : getInbox(user)) {
            if (m.isStarred()) {
                result.add(m);
            }
        }
        for (MailItem m : getSent(user)) {
            if (m.isStarred()) {
                result.add(m);
            }
        }
        return result;
    }

    /** Move a mail from inbox/sent to the trash. */
    public void moveToTrash(String user, MailItem item) {
        Account box = accounts.get(normalize(user));
        if (box == null) {
            return;
        }
        boolean removed = box.inbox.remove(item) || box.sent.remove(item);
        if (removed) {
            box.trash.add(0, item);
        }
    }

    /** Permanently delete a mail from the trash. */
    public void deleteForever(String user, MailItem item) {
        Account box = accounts.get(normalize(user));
        if (box != null) {
            box.trash.remove(item);
        }
    }

    /** Permanently delete everything in the trash. */
    public void emptyTrash(String user) {
        Account box = accounts.get(normalize(user));
        if (box != null) {
            box.trash.clear();
        }
    }
}