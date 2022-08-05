
import javafx.util.Pair;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;


class EmailClientInterface {
    public static void main(String[] args) {
        /**User Interface of the Email Client Application*/
        boolean programOn = true;
        EmailClientProgram program = null;
        try {
            program = new EmailClientProgram();
        } catch (Exception e) {
            System.out.println("Program Crashed While Opening!\nError--->");
            System.out.println(e.getMessage());
            System.exit(1);
        }
        Scanner scanner = new Scanner(System.in);
        String userInput;

        while (programOn) {
            System.out.println("\nEnter option : \n" +
                    "1 - Adding a new recipient\n" +
                    "2 - Sending an email\n" +
                    "3 - Printing out all the recipients who have birthdays\n" +
                    "4 - Printing out details of all the emails sent\n" +
                    "5 - Printing out the number of recipient objects in the application\n" +
                    "6 - Exit");
            int option;
            try {
                option = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Enter a Valid Number!");
                continue;
            }

            switch (option) {
                case 1:
                    System.out.println("Enter recipient in this format --->\n" +
                            "\tofficial: <name>, <email>,<designation>\n" +
                            "\tOffice_friend: <name>,<email>,<designation>,<birthday(yyyy/MM/dd)>\n" +
                            "\tPersonal: <name>,<nick-name>,<email>,<birthday(yyyy/MM/dd)>\n\n" +
                            "\tExample---> Office_friend: kamal,kamal@gmail.com,clerk,2000/12/12");
                    userInput = scanner.nextLine();
                    program.addRecipient(userInput);
                    break;
                case 2:
                    // input format - email, subject, content
                    System.out.println("Enter details in the format ---> <email>, <subject>, <content>");
                    userInput = scanner.nextLine();
                    program.sendEmail(userInput);//exception thrown
                    break;
                case 3:
                    // input format - yyyy/MM/dd (ex: 2018/09/17)
                    System.out.println("Enter date in the format ---> yyyy/MM/dd (ex: 2018/09/17)");
                    userInput = scanner.nextLine();
                    program.printRecipientsWithBirthDate(userInput);
                    break;
                case 4:
                    // input format - yyyy/MM/dd (ex: 2018/09/17)
                    System.out.println("Enter date in the format ---> yyyy/MM/dd (ex: 2018/09/17)");
                    userInput = scanner.nextLine();
                    program.printEmailsSentOnDate(userInput);
                    break;
                case 5:
                    program.printRecipientCount();
                    break;
                case 6:
                    programOn = false;
                    break;
            }
        }
    }
}

class EmailClientProgram {
    /**Back end program of Email Client Application which handles all the operations requested by the user*/
    private final ArrayList<Wishable> birthdayRecipients = new ArrayList<>();
    private final ArrayList<Email> emails = new ArrayList<>();
    private ArrayList<Recipient> allRecipients = new ArrayList<>();
    private ArrayList<Wishable> wishableRecipients = new ArrayList<>();

    public EmailClientProgram() throws IOException, ParseException, ClassNotFoundException, MessagingException {
        System.out.println("Email Client Program is Starting...");
        new File("Emails.ser").createNewFile();
        new File("clientList.txt").createNewFile();
        loadRecipientLists();
        loadBirthdayRecipients();
        sendBirthdayGreetings();
        loadEmails();
    }

    private void loadRecipientLists() throws IOException {
        Pair<ArrayList<Recipient>, ArrayList<Wishable>> pair = RecipientCreator.initializeAndGetRecipientLists();
        allRecipients = pair.getKey();
        wishableRecipients = pair.getValue();
    }

    private void loadBirthdayRecipients() throws ParseException {
        for (Wishable wishable : wishableRecipients) {
            if (DateChecker.isTodayBirthday(wishable.getBirthday())) {
                birthdayRecipients.add(wishable);
            }
        }
    }

    private void sendBirthdayGreetings() throws IOException, MessagingException {
        for (Wishable wishable : birthdayRecipients) {
            Email email = new BirthdayEmailCreator().createEmail(wishable);
            MailComposer.sendEmail(email);
            saveOnDisk(email);
        }
    }

    public void addRecipient(String userInput) {
        String[] recipient = userInput.replaceAll("\\s+", "").split(":|,");
        boolean isAdded = RecipientCreator.addRecipientToList(recipient);
        if (!isAdded) return;
        allRecipients = RecipientCreator.getAllRecipients();
        wishableRecipients = RecipientCreator.getWishableRecipients();
        try {
            saveOnDisk(userInput);
            System.out.println("Recipient added Successfully!");
        } catch (IOException e) {
            System.out.println("Error: Recipient Not Saved on disk!");
            System.out.println(e.getMessage());
        }
    }

    private void saveOnDisk(String recipient) throws IOException {
        FileWriter writer = new FileWriter("clientList.txt", true);
        writer.write(recipient + "\n");
        writer.close();
    }

    public void sendEmail(String userInput) {
        String[] emailData = userInput.split(",");
        String recipientEmail;
        String subject;
        String content;
        try {
            recipientEmail = emailData[0];
            subject = emailData[1];
            content = emailData[2];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Invalid Email Format!");
            return;
        }

        Email email = new CustomEmailCreator().createEmail(recipientEmail, subject, content);
        try {
            MailComposer.sendEmail(email);
            System.out.println("Email Sent Successfully!");
            emails.add(email);
            try {
                saveOnDisk(email);
            } catch (IOException e) {
                System.out.println("Error: Email not saved to Disk!");
                System.out.println(e.getMessage());
            }
        } catch (MessagingException e) {
            System.out.println("Error: Email Not Sent!");
            System.out.println(e.getMessage());
        }
    }

    private void saveOnDisk(Email email) throws IOException {
        File f = new File("Emails.ser");
        FileOutputStream fos = new FileOutputStream("Emails.ser", true);
        if (f.length() == 0) {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(email);
            oos.close();
        } else {
            MyObjectOutputStream oos = new MyObjectOutputStream(fos);
            oos.writeObject(email);
            oos.close();
        }
        fos.close();
    }

    public void printRecipientCount() {
        System.out.println("Number of Recipients : " + Recipient.RecipientCount);
    }

    public void printEmailsSentOnDate(String inputDate) {
        boolean isPresent = false;
        for (Email email : emails) {
            try {
                if (DateChecker.isEqual(email.getSentDate(), inputDate)) {
                    System.out.println(email.getEmailSummary());
                    isPresent = true;
                }
            } catch (ParseException e) {
                System.out.println("Error:");
                System.out.println(e.getMessage());
                return;
            }
        }
        if (!isPresent) System.out.println("No Emails on given date");
    }

    private void loadEmails() throws IOException, ClassNotFoundException {
        File file = new File("Emails.ser");
        if (file.length() == 0) return;
        FileInputStream fileStream = new FileInputStream(file);
        ObjectInputStream os = new ObjectInputStream(fileStream);

        while (true) {
            try {
                emails.add((Email) os.readObject());
            } catch (EOFException exc) {
                os.close();
                break;
            }
        }
    }

    public void printRecipientsWithBirthDate(String inputDate) {
        boolean isPresent = false;
        for (Wishable wishable : wishableRecipients) {
            try {
                if (DateChecker.isEqual(wishable.getBirthday(), inputDate)) {
                    System.out.println(wishable.getName());
                    isPresent = true;
                }
            } catch (ParseException e) {
                System.out.println("Error:");
                System.out.println(e.getMessage());
                return;
            }
        }
        if (!isPresent) System.out.println("No birthdays on given date");
    }
}

interface Wishable {
    String getBirthdayWishMsg();

    String getName();

    String getEmail();

    String getBirthday();

}

class MyObjectOutputStream extends ObjectOutputStream {

    /** Modified Object Output Stream. Allows appending serialized objects to the same file without replacing the file with a new file */
    // Constructor of this class
    // 1. Default
    MyObjectOutputStream() throws IOException {

        // Super keyword refers to parent class instance
        super();
    }

    // Constructor of this class
    // 1. Parameterized constructor
    public MyObjectOutputStream(OutputStream o) throws IOException {
        super(o);
    }

    // Method of this class
    public void writeStreamHeader() throws IOException {
    }
}

class RecipientCreator {
    /**Create Recipient objects using recipient's data stored in the disk.
     *Create Recipient objects using user input data.
     *Store Created Recipient objects in lists and Return then when requested*/
    private static final ArrayList<Recipient> allRecipients = new ArrayList<>();
    private static final ArrayList<Wishable> wishableRecipients = new ArrayList<>();
    private static final ArrayList<String[]> recipientDataList = new ArrayList<>();

    public static ArrayList<Recipient> getAllRecipients() {
        return allRecipients;
    }

    public static ArrayList<Wishable> getWishableRecipients() {
        return wishableRecipients;
    }

    public static Pair<ArrayList<Recipient>, ArrayList<Wishable>> initializeAndGetRecipientLists() throws IOException {
        loadRecipientLists();
        return new Pair<>(allRecipients, wishableRecipients);
    }

    private static void loadRecipientLists() throws IOException {
        lineByLineText();
        for (String[] recipientData : recipientDataList) {
            addRecipientToList(recipientData);
        }
    }

    public static boolean addRecipientToList(String[] recipientData) {
        String friendState = recipientData[0].toLowerCase().trim();
        switch (friendState) {
            case "official":
                allRecipients.add(new OfficialRecipient(recipientData[1], recipientData[2], recipientData[3]));
                return true;
            case "office_friend":
                OfficeFriendRecipient officeFriendRecipient = new OfficeFriendRecipient(recipientData[1], recipientData[2], recipientData[3], recipientData[4]);
                allRecipients.add(officeFriendRecipient);
                wishableRecipients.add(officeFriendRecipient);
                return true;
            case "personal":
                PersonalRecipient personalRecipient = new PersonalRecipient(recipientData[1], recipientData[2], recipientData[3], recipientData[4]);
                allRecipients.add(personalRecipient);
                wishableRecipients.add(personalRecipient);
                return true;
            default:
                System.out.println("Invalid Recipient data!");
                return false;
        }
    }

    private static void lineByLineText() throws IOException {

        FileReader reader = new FileReader("clientList.txt");
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            String[] recipientData = line.split(":|\\,");
            recipientDataList.add(recipientData);
        }
        reader.close();
    }

}

class MailComposer {
    private static final SendEmailTLS sendEmailTLS = new SendEmailTLS();

    private MailComposer() {
    }

    public static void sendEmail(Email email) throws MessagingException {
        sendEmailTLS.send(email.getRecipientEmail(), email.getSubject(), email.getContent());
    }
}

class SendEmailTLS {

    /** Send an email via Gmail SMTP server with TLS*/
    private final String username = "isurupramudith.20@cse.mrt.ac.lk";//My email
    private final String password = "cuhdilhfhzgocxzd";//Application-specific password generated using Google App password
    private Session session;

    SendEmailTLS() {
        initialize();
    }

    private void initialize() {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true"); //TLS

        session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
    }

    void send(String recipientEmail, String subject, String content) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("from@gmail.com"));
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(recipientEmail)
        );
        message.setSubject(subject);
        message.setText(content);

        Transport.send(message);
    }
}

class DateChecker {
    private static final SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd");

    public static boolean isEqual(String date1, String date2) throws ParseException {
        Date d1 = sdFormat.parse(date1);
        Date d2 = sdFormat.parse(date2);
        boolean isEqual = sdFormat.format(d1).equals(sdFormat.format(d2));
        return isEqual;
    }

    public static boolean isTodayBirthday(String date) throws ParseException {
        Date inputDate = sdFormat.parse(date);
        Date today = new Date();
        return (today.getMonth() == inputDate.getMonth() && today.getDate() == inputDate.getDate());
    }

    public static String getCurrentDate() {
        return sdFormat.format(new Date());
    }
}

abstract class Recipient {
    public static int RecipientCount = 0;
    String name;
    String email;

    public Recipient(String name, String email) {
        RecipientCount++;
        this.name = name;
        this.email = email;
    }

}

class PersonalRecipient extends Recipient implements Wishable {
    private final String birthday;
    private final String nickname;

    public PersonalRecipient(String name, String nickname, String email, String birthday) {
        super(name, email);
        this.nickname = nickname;
        this.birthday = birthday;
    }

    @Override
    public String getBirthdayWishMsg() {
        return "Hugs and love on your birthday!";
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public String getBirthday() {
        return this.birthday;
    }
}

class OfficialRecipient extends Recipient {
    private final String designation;

    public OfficialRecipient(String name, String email, String designation) {
        super(name, email);
        this.designation = designation;
    }
}

class OfficeFriendRecipient extends Recipient implements Wishable {
    private final String designation;
    private final String birthday;

    public OfficeFriendRecipient(String name, String email, String designation, String birthday) {
        super(name, email);
        this.birthday = birthday;
        this.designation = designation;
    }

    @Override
    public String getBirthdayWishMsg() {
        return "Wish you a Happy Birthday!";
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return email;
    }

    public String getBirthday() {
        return this.birthday;
    }
}

class Email implements Serializable {
    private final String subject;
    private final String content;
    private final String recipientEmail;
    private final String sentDate;

    public Email(String recipientEmail, String subject, String content) {
        this.subject = subject;
        this.content = content;
        this.recipientEmail = recipientEmail;
        this.sentDate = DateChecker.getCurrentDate();
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getSentDate() {
        return sentDate;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getEmailSummary() {
        return ("Recipient:\t" + recipientEmail +
                "\nSubject:\t" + subject + "\n");
    }
}

abstract class EmailCreator {
    Email email;
}

class CustomEmailCreator extends EmailCreator {

    public Email createEmail(String recipientEmail, String subject, String content) {
        this.email = new Email(recipientEmail, subject, content);
        return email;
    }
}

class BirthdayEmailCreator extends EmailCreator {

    public Email createEmail(Wishable wishable) {
        this.email = new Email(wishable.getEmail(), "Birthday Wishes!", wishable.getBirthdayWishMsg() + "\n\n" + "Isuru");
        return email;
    }
}


