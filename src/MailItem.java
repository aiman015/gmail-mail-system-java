import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * MailItem: stores the contents of one email.
 * A MailItem is never created directly by the user - it is created
 * inside MailClient and passed around by MailServer.
 */
public class MailItem {

    // The sender of the item.
    private String from;
    // The intended recipient.
    private String to;
    // The subject line.
    private String subject;
    // The text of the message.
    private String message;
    // When the mail was sent.
    private Date time;
    // Has the recipient opened this mail?
    private boolean read;
    // Has the owner starred this mail?
    private boolean starred;

    /**
     * Create a mail item from sender to the given recipient,
     * containing the given subject and message.
     *
     * @param from    The sender of this item.
     * @param to      The intended recipient of this item.
     * @param subject The subject of the mail.
     * @param message The text of the message to be sent.
     */
    public MailItem(String from, String to, String subject, String message) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.message = message;
        this.time = new Date();
        this.read = false;
        this.starred = false;
    }

    /** @return The sender of this mail. */
    public String getFrom() {
        return from;
    }

    /** @return The intended recipient of this mail. */
    public String getTo() {
        return to;
    }

    /** @return The subject of this mail. */
    public String getSubject() {
        return subject;
    }

    /** @return The text of the message. */
    public String getMessage() {
        return message;
    }

    /** @return true if this mail has been read. */
    public boolean isRead() {
        return read;
    }

    /** Mark the mail as read / unread. */
    public void setRead(boolean read) {
        this.read = read;
    }

    /** @return true if this mail is starred. */
    public boolean isStarred() {
        return starred;
    }

    /** Star / un-star this mail. */
    public void toggleStar() {
        this.starred = !this.starred;
    }

    /**
     * Make a separate copy of this mail for the recipient's inbox.
     * (When you mail yourself, the Sent copy and the Inbox copy must be
     * different objects so that "read" and "star" are tracked separately.)
     */
    public MailItem copyForRecipient() {
        MailItem copy = new MailItem(this.from, this.to, this.subject, this.message);
        copy.time = this.time;
        return copy;
    }

    /** @return Short time like "3:45 PM" (today) or "Oct 2" (older). */
    public String getShortTime() {
        SimpleDateFormat day = new SimpleDateFormat("yyyyMMdd");
        boolean today = day.format(time).equals(day.format(new Date()));
        return new SimpleDateFormat(today ? "h:mm a" : "MMM d").format(time);
    }

    /** @return Date and time for the mail list, e.g. "Oct 3, 6:36 AM" (year added if not this year). */
    public String getListTime() {
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        boolean sameYear = year.format(time).equals(year.format(new Date()));
        return new SimpleDateFormat(sameYear ? "MMM d, h:mm a" : "MMM d, yyyy, h:mm a").format(time);
    }

    /** @return Full time like "Fri, Oct 2, 2026, 3:45 PM". */
    public String getFullTime() {
        return new SimpleDateFormat("EEE, MMM d, yyyy, h:mm a").format(time);
    }

    /** @return The message flattened to one line (for the inbox preview). */
    public String getSnippet() {
        return message.replace('\n', ' ').replace('\r', ' ').trim();
    }

    /** Print the contents of this mail to the terminal. */
    public void print() {
        System.out.println("From:    " + from);
        System.out.println("To:      " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Date:    " + getFullTime());
        System.out.println();
        System.out.println(message);
        System.out.println("-----------------------------------");
    }
}