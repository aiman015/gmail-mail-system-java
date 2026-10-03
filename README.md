# Lab 05 – Object Interaction II: Mail System

**Course:** Software Construction Lab (Fall-2026)
**Instructor:** ENGR. SAAD MAZHAR
**Lab:** Lab 05 – Object Interaction II (Debugging, the `this` keyword, abstraction & modularization)

---

## About the Project

A Gmail-style mail system written in Java. Several users can create accounts, sign in, and exchange emails through one shared server. The project is based on the Lab 05 mail-system example (`MailServer`, `MailClient`, `MailItem`) and includes a Swing GUI designed to look like Gmail.

## Classes

| Class        | Role                                                                                                                                                              |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MailItem`   | Stores the contents of one email, including sender, recipient, subject, message, time, read status, and starred status. It is never created directly by the user. |
| `MailServer` | A shared server used by all clients. It manages accounts and mailboxes such as inbox, sent, and trash, and transfers mail between clients.                        |
| `MailClient` | Represents one client for a signed-in user. It sends emails through the server and retrieves emails from it.                                                      |
| `MailGUI`    | The Gmail-style Swing interface. It contains `main`; each window opened represents a separate `MailClient` connected to the same `MailServer`.                    |

## Features

* Create an account with live username and password validation
* Sign in and sign out, with multiple accounts open in separate windows
* Compose and send emails, including sending an email to yourself
* Inbox, Starred, Sent, and Trash folders
* Read, reply, forward, star, and mark emails as unread
* Move emails to Trash, delete them permanently, and empty Trash
* Search emails
* Unread email counter with automatic refresh
* Resizable window
* Sign-in and sign-up screens adapt to the window size

## Lab Requirements Covered

* Mail exchange between two mail clients through the server
* Sending email
* Receiving email
* Reading and displaying email
* OOP concepts including classes and objects, encapsulation, abstraction, modularization, object interaction, and the `this` keyword

## Account Rules

**Username:** 3–20 characters, starts with a letter, and contains only letters, numbers, and `_`. It cannot be a number, for example `42`, `3.14`, or `-7`.

**Password:** At least 8 characters and must contain both letters and numbers.

All email addresses end with `@gmail.com`.

## How to Run

**Requirements:** JDK 8 or newer. The project was developed in Eclipse using JavaSE-25.

**Eclipse**

1. Import the project or copy the four `.java` files into `src/` (default package).
2. Right-click `MailGUI.java`, then select **Run As → Java Application**.

**Command line**

```bash
cd src
javac *.java
java MailGUI
```

The server starts with no accounts. Click **Create account** to add the first user. To test email exchange, open the avatar menu and select **Add another account (new window)**. Create a second user and send a message between the two windows.

## Project Structure

```text
Lab05/
├── src/
│   ├── MailClient.java
│   ├── MailGUI.java
│   ├── MailItem.java
│   └── MailServer.java
├── .gitignore
└── README.md
```
