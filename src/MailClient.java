import java.util.List;

/**
 * MailClient: one client per signed-in user.
 * It sends mail through the server and reads mail from the server.
 */
public class MailClient {

    // The server that handles our mail.
    private MailServer server;
    // The user running this client.
    private String user;
    // Reason why the last send failed (empty if it worked).
    private String lastError;

    /**
     * Create a mail client run by user and connected to the given server.
     */
    public MailClient(MailServer server, String user) {
        this.server = server;
        this.user = MailServer.normalize(user);
        this.lastError = "";
    }

    /** @return the username, e.g. "alice". */
    public String getUser() {
        return user;
    }

    /** @return the full address, e.g. "alice@gmail.com". */
    public String getAddress() {
        return user + MailServer.DOMAIN;
    }

    /** @return your own name, e.g. "Alice Khan". */
    public String getDisplayName() {
        return server.getDisplayName(user);
    }

    /** @return the name of any user (or the username if unknown). */
    public String getDisplayName(String username) {
        return server.getDisplayName(username);
    }

    /** @return the reason the last send failed. */
    public String getLastError() {
        return lastError;
    }

    /**
     * Send a mail item to another user (or to yourself).
     *
     * @return true if sent. false if the recipient is empty or does not
     *         exist - use getLastError() to see why.
     */
    public boolean sendMailItem(String to, String subject, String message) {
        String recipient = MailServer.normalize(to);

        if (recipient.isEmpty()) {
            lastError = "Please specify at least one recipient.";
            return false;
        }
        if (!server.userExists(recipient)) {
            lastError = "Address not found. Your message wasn't delivered because "
                    + "\"" + to.trim() + "\" doesn't exist.";
            return false;
        }

        String subj = subject == null ? "" : subject.trim();
        if (subj.isEmpty()) {
            subj = "(no subject)";
        }
        String body = message == null ? "" : message;

        MailItem item = new MailItem(user, recipient, subj, body);
        boolean ok = server.post(item);
        lastError = ok ? "" : "Message could not be delivered.";
        return ok;
    }

    /** @return how many mails are waiting in the inbox. */
    public int howManyMailItems() {
        return server.howManyMailItems(user);
    }

    /** @return how many inbox mails are unread. */
    public int countUnread() {
        return server.howManyUnread(user);
    }

    /** Receive the next unread mail from the server (null if none). */
    public MailItem getNextMailItem() {
        return server.getNextMailItem(user);
    }

    /** Receive the next unread mail and print it (lab-style). */
    public void printNextMailItem() {
        MailItem item = server.getNextMailItem(user);
        if (item == null) {
            System.out.println("No new mail.");
        } else {
            item.print();
        }
    }

    public List<MailItem> getInbox() {
        return server.getInbox(user);
    }

    public List<MailItem> getSent() {
        return server.getSent(user);
    }

    public List<MailItem> getTrash() {
        return server.getTrash(user);
    }

    public List<MailItem> getStarred() {
        return server.getStarred(user);
    }

    /** Mark a received mail as read. */
    public void markRead(MailItem item) {
        item.setRead(true);
    }

    /** Mark a mail as unread. */
    public void markUnread(MailItem item) {
        item.setRead(false);
    }

    /** Star or un-star a mail. */
    public void toggleStar(MailItem item) {
        item.toggleStar();
    }

    /** Move a mail to the trash. */
    public void delete(MailItem item) {
        server.moveToTrash(user, item);
    }

    /** Permanently delete a mail that is already in the trash. */
    public void deleteForever(MailItem item) {
        server.deleteForever(user, item);
    }

    /** Empty the trash. */
    public void emptyTrash() {
        server.emptyTrash(user);
    }
}