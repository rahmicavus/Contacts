package contacts;

import java.io.*;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static Scanner scanner = new Scanner(System.in);
    public static String fileName = "phonebook.db";

    public static void main(String[] args){
        if(args.length>0) {
            fileName = args[0];
        }
        System.out.println("open "+fileName);
        UserInterface ui = new UserInterface();
        ui.startMenu();
    }
}

class UserInterface {

    private Actions actions;
    private Scanner scanner;

    public UserInterface(){
        actions = new Actions();
        scanner = new Scanner(System.in);
    }

    public void startMenu() {

        while (true) {
            System.out.print("\n[menu] Enter action (add, list, search, count, exit): ");
            String action = scanner.nextLine();
            switch (action) {
                case "add":
                    addMenu();
                    break;
                case "list":
                    actions.list();
                    break;
                case "search":
                    actions.search();
                    break;
//                case "remove":
//                    actions.remove();
//                    break;
//                case "edit":
//                    actions.edit();
//                    break;
                case "count":
                    actions.count();
                    break;
//                case "info":
//                    actions.info();
//                    break;
                case "exit":
                    scanner.close();
                    actions.closeScanner();
                    return;
                default:
                    System.out.println("Incorrect action.");
            }
        }
    }

    private void addMenu() {
        System.out.print("Enter the type (person, organization): ");
        String type = scanner.nextLine().toLowerCase();
        switch (type) {
            case "person":
                actions.addPerson();
                break;
            case "organization":
                actions.addOrganization();
                break;
            default:
                System.out.println("Incorrect type.");
                return;
        }
        System.out.println("The record added.");
    }
}

class PhoneBook {
    private List<Contact> phoneBook;

    public PhoneBook() {
        phoneBook = new ArrayList<>();
    }

    public void addContact(Contact contact) {
        phoneBook.add(contact);
        SerializationUtils.serialize(contact,Main.fileName);
    }

    public void removeContact(int recordNumber) {
        phoneBook.remove(recordNumber);
    }

    public Contact getContact(int recordNumber) {
        return phoneBook.get(recordNumber);
    }

    public int getSize() {
        return phoneBook.size();
    }

    public List<Contact> getPhoneBook() {
        return new ArrayList<>(phoneBook);

    }
}
class Actions  {
    PhoneBook phoneBook;
    private Scanner scanner;

    public Actions() {
        phoneBook = new PhoneBook();
        scanner = new Scanner(System.in);
    }
    public void addPerson(){
        System.out.print("Enter the name: ");
        String name = scanner.nextLine();

        System.out.print("Enter the surname: ");
        String surname = scanner.nextLine();

        System.out.print("Enter the birth date: ");
        LocalDate birthDate = setBirthDate(scanner.nextLine());

        System.out.print("Enter the gender (M, F): ");
        String gender = setGender(scanner.nextLine());

        System.out.print("Enter the number: ");
        String number = setPhoneNumber(scanner.nextLine());

        phoneBook.addContact(
                new Person(
                        name,
                        surname,
                        birthDate,
                        gender,
                        number
                ));
    }
    public void addOrganization() {
        System.out.print("Enter the organization name: ");
        String name = scanner.nextLine();

        System.out.print("Enter the address: ");
        String address = scanner.nextLine();

        System.out.print("Enter the number: ");
        String number = setPhoneNumber(scanner.nextLine());

        phoneBook.addContact(
                new Organization(
                        name,
                        address,
                        number
                ));
    }
    public void search() {

        List<Integer> contactIdx;

        contactIdx = searchItems();

        System.out.print("[search] Enter action ([number], back, again): > ");
        String strChoice;
        do {
            strChoice = scanner.nextLine();

            if(strChoice.equals("back")) { break;}
            else if(strChoice.equals("again")) {
                contactIdx = searchItems();
                System.out.println();
                System.out.print("[search] Enter action ([number], back, again): ");
                continue;
            }
            else {
                int recNum = Integer.parseInt(strChoice);
                System.out.println(phoneBook.getContact(contactIdx.get(recNum - 1)));

                System.out.println();
                System.out.print("[record] Enter action (edit, delete, menu): > ");

                String actChoice;
                do {
                     actChoice = scanner.nextLine();

                    if (actChoice.equals("menu")) {
                         strChoice = "back";
                         break;
                     }
                    else if (actChoice.equals("edit")) {

                        Contact contact = phoneBook.getContact(contactIdx.get(recNum - 1));
                        switch (contact.getClass().getSimpleName())
                            {
                            case "Person":
                                editPerson(contact);
                                SerializationUtils.serialize(contact,Main.fileName);
                                break;
                            case "Organization":
                                editOrganization(contact);
                                SerializationUtils.serialize(contact,Main.fileName);
                                break;
                             }
                        System.out.println("Saved");
                        System.out.println(contact);
                        System.out.print("[record] Enter action (edit, delete, menu): ");
                     }
                    else if(actChoice.equals("delete")) {
                        phoneBook.removeContact(contactIdx.get(recNum - 1));
                        System.out.println("Removed");
                        System.out.println();
                        System.out.print("[record] Enter action (edit, delete, menu): ");
                    }
             }while (!actChoice.equals("menu"));
            }
        }while (!strChoice.equals("back"));
    }
    private List<Integer> searchItems() {

        System.out.print("Enter search query: > ");
        String key = scanner.nextLine();
        Pattern pattern = Pattern.compile(key.toLowerCase(),Pattern.CASE_INSENSITIVE);


        List<Integer> foundContactsList = new ArrayList<>();

        for (Contact contact : phoneBook.getPhoneBook()) {
            for(String str : contact.allFieldsForChange()) {
                Matcher matcher = pattern.matcher(contact.getFieldVal(str));

                if(matcher.find()) {
                    foundContactsList.add(phoneBook.getPhoneBook().indexOf(contact));
                    break;
                }
            }
        }

        System.out.println("Found "+foundContactsList.size() +" results:");

        for(int i= 0; i<foundContactsList.size();i++) {
            System.out.println((i+1)+". "+ phoneBook.getPhoneBook().get(foundContactsList.get(i)).getFullName());
        }
        return foundContactsList;
    }
    public void remove() {
        if(phoneBook.getSize() == 0) {
            System.out.println("No records to remove!");
            return;
        }

        showInfo();
        System.out.print("Enter index to remove: ");
        int index = isCorrectIndex(scanner.nextLine());
        if (index== -1) {
            System.out.println("Wrong index!");
        } else {
            phoneBook.removeContact(index-1);
            System.out.println("The record removed.");
        }
    }
    public void edit() {
        if (phoneBook.getSize() == 0) {
            System.out.println("No records to edit!");
            return;
        }
        showInfo();
        System.out.print("Select a record: ");
        int index = isCorrectIndex(scanner.nextLine());
        if (index == -1) {
            System.out.println("Wrong index!");
        } else {
            Contact contact = phoneBook.getContact(index - 1);
            switch (contact.getClass().getSimpleName()) {
                case "Person":
                    editPerson(contact);
                    SerializationUtils.serialize(contact,Main.fileName);
                    break;
                case "Organization":
                    editOrganization(contact);
                    SerializationUtils.serialize(contact,Main.fileName);
                    break;
            }
            System.out.println("The record updated!");
        }
    }
    private void editPerson(Contact contact) {
        Person person = (Person) contact;
        System.out.print("Select a field (name, surname, birth, gender, number): ");
        String field = scanner.nextLine().toLowerCase();
        switch (field) {
            case "name":
                System.out.print("Enter name: ");
                //person.setName(scanner.nextLine());
                person.setFieldVal("name",scanner.nextLine());
                break;
            case "surname":
                System.out.print("Enter surname: ");
                //person.setSurname(scanner.nextLine());
                person.setFieldVal("surname",scanner.nextLine());
                break;
            case "birth":
                System.out.print("Enter birth date: ");
                //person.setBirthDate(setBirthDate(scanner.nextLine()));
                person.setFieldVal("birth",scanner.nextLine());
                break;
            case "gender":
                System.out.print("Enter gender: ");
                //person.setGender(setGender(scanner.nextLine()));
                person.setFieldVal("gender",scanner.nextLine());
                break;
            case "number":
                System.out.print("Enter number: ");
                //person.setPhoneNumber(setPhoneNumber(scanner.nextLine()));
                person.setFieldVal("number",scanner.nextLine());
                break;
            default:
                System.out.println("Incorrect field.");
        }
    }
    private void editOrganization(Contact contact) {
        Organization organization = (Organization) contact;
        System.out.print("Select a field (name, address, number): ");
        String field = scanner.nextLine().toLowerCase();
        switch (field) {
            case "name":
                System.out.print("Enter name: ");
                //organization.setName(scanner.nextLine());
                organization.setFieldVal("name",scanner.nextLine());
                break;
            case "address":
                System.out.print("Enter address: ");
                //organization.setAddress(scanner.nextLine());
                organization.setFieldVal("address",scanner.nextLine());
                break;
            case "number":
                System.out.print("Enter number: ");
                //organization.setPhoneNumber(setPhoneNumber(scanner.nextLine()));
                organization.setFieldVal("number",scanner.nextLine());
                break;
            default:
                System.out.println("Incorrect field.");
        }
    }
    public void count() {
        System.out.printf("The phone book has %s records.\n", phoneBook.getSize());
    }
    public void closeScanner() {
        scanner.close();
    }
    public void info() {
        if (phoneBook.getSize() == 0) {
            System.out.println("Phone book is empty.");
            return;
        }
        showInfo();
        System.out.print("Enter index to show info: ");
        int index = isCorrectIndex(scanner.nextLine());
        if (index == -1) {
            System.out.println("Wrong index!");
        } else {
            System.out.println(phoneBook.getContact(index - 1));
        }
    }
    private int isCorrectIndex(String input) {
        int index;
        try {
            index = Integer.parseInt(input);
        } catch (Exception e) {
            index =-1;
        }
        if(index > phoneBook.getSize()) {
            index = -1;
        }

        return index;
    }
    private void showInfo() {
        int index = 0;
        for(Contact contact : phoneBook.getPhoneBook()) {
            System.out.printf("%d. %s\n",++index,contact.getFullName());
        }
    }
    private String setPhoneNumber(String phoneNumber) {
        boolean isValidNumber = Validator.isValidPhoneNumber(phoneNumber);
        if(!isValidNumber) {
            System.out.println("Wrong number format!");
        }
        return isValidNumber ? phoneNumber : "[no data]";
    }
    private String setGender(String gender) {
        boolean validGender = Validator.isValidGender(gender);
        if(!validGender) {
            System.out.println("Bad gender!");
        }
        return  validGender ? gender : "[no data]";
    }
    private LocalDate setBirthDate(String birthDate) {
        boolean validBirthDate = Validator.isValidBirthDate(birthDate);
        if(!validBirthDate) {
            System.out.println("Bad birth date!");
        }
        return validBirthDate ? LocalDate.parse(birthDate,DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
    }

    public void list() {
        int k = 1;
        for ( int i=0 ; i<phoneBook.getSize(); i++) {
            System.out.println(k +". "+ phoneBook.getContact(i).getFullName());
            k++;
        }

        System.out.println();
        System.out.print("list] Enter action ([number], back): > ");
        String listChoice;
        do{
            listChoice = scanner.nextLine();
            if(listChoice.equals("back")) { break;}
            else {

                int recNum = Integer.parseInt(listChoice);
                System.out.println(phoneBook.getContact(recNum - 1));

                System.out.println();
                System.out.print("[record] Enter action (edit, delete, menu): > ");

                String actChoice;
                do {
                    actChoice = scanner.nextLine();

                    if (actChoice.equals("menu")) {
                        listChoice = "back";
                        break;
                    }
                    else if (actChoice.equals("edit")) {

                        Contact contact = phoneBook.getContact(recNum - 1);
                        switch (contact.getClass().getSimpleName())
                        {
                            case "Person":
                                editPerson(contact);
                                SerializationUtils.serialize(contact,Main.fileName);
                                break;
                            case "Organization":
                                editOrganization(contact);
                                SerializationUtils.serialize(contact,Main.fileName);
                                break;
                        }
                        System.out.println("Saved");
                        System.out.println(contact);
                        System.out.print("[record] Enter action (edit, delete, menu): ");
                    }
                    else if(actChoice.equals("delete")) {
                        phoneBook.removeContact(recNum - 1);
                        System.out.println("Removed");
                        System.out.println();
                        System.out.print("[record] Enter action (edit, delete, menu): ");
                    }
                }while (!actChoice.equals("menu"));

            }
        }
        while (!listChoice.equals("back"));


    }
}
abstract class Contact implements Serializable {

    private String phoneNumber;
    private final LocalDateTime timeCreated;
    private LocalDateTime lastEdit;

    public Contact(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        timeCreated = LocalDateTime.now().withSecond(0).withNano(0);
        lastEdit = timeCreated;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setLastEdit(LocalDateTime.now());
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        setLastEdit(LocalDateTime.now());
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public LocalDateTime getLastEdit() {
        return lastEdit;
    }

    public void setLastEdit(LocalDateTime lastEdit) {
        this.lastEdit = lastEdit.withSecond(0).withNano(0);
    }

    public abstract  String toString();
    public abstract String getFullName();
    public abstract List<String> allFieldsForChange();
    abstract String getFieldVal(String fName);
    abstract void setFieldVal(String fName, String newVal);

}
class Person extends Contact implements Serializable {
    private String surname;
    private String gender;
    private LocalDate birthDate;

    public Person(String name, String surname, LocalDate birthDate, String gender, String phoneNumber) {
        super(name,phoneNumber);
        this.surname = surname;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public String getSurname() {
        return surname;

    }

    public void setSurname(String surname) {
        this.surname = surname;
        setLastEdit(LocalDateTime.now());
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
        setLastEdit(LocalDateTime.now());
    }

    public String getBirthDate() {
        String str="";
        if(birthDate == null) {
            str ="[no data]";
        } else {
            str = birthDate.toString();
        }
        return str;

    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        setLastEdit(LocalDateTime.now());
    }
    public void setBirthDate(String birthDate) {
        LocalDate dt = LocalDate.parse(birthDate,DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        this.birthDate = dt;
        setLastEdit(LocalDateTime.now());
    }

    @Override
    public String toString() {
        String birthDate = getBirthDate() == null ? "[no data]" : getBirthDate().toString();
        return "Name: " + getName() + "\n"
                + "Surname: " + getSurname() + "\n"
                + "Birth date: " + birthDate + "\n"
                + "Gender: " + getGender() + "\n"
                + "Number: " + getPhoneNumber() + "\n"
                + "Time created: " + getTimeCreated() + "\n"
                + "Time last edit: " + getLastEdit();
    }

    @Override
    public String getFullName() {
        return getName() + " " + getSurname();
    }

    @Override
    public List<String> allFieldsForChange() {
        List<String> fields = new ArrayList<>();
        fields.add("name");
        fields.add("surname");
        fields.add("birth");
        fields.add("gender");
        fields.add("number");
        return fields;
    }

    @Override
    String getFieldVal(String fName) {
        switch (fName) {
            case "name": return getName();
            case "surname": return getSurname();
            case "birth": return getBirthDate().toString();
            case "gender": return getGender();
            case "number": return getPhoneNumber();
            default:
                System.out.println("bad field name");return "";
        }
    }

    @Override
    void setFieldVal(String fName, String newVal) {
        switch (fName) {
            case "name": setName(newVal);break;
            case "surname": setSurname(newVal);break;
            case "birth": setBirthDate(LocalDate.parse(newVal,DateTimeFormatter.ofPattern("dd/MM/yyyy")));break;
            case "gender": setGender(newVal);break;
            case "number": setPhoneNumber(newVal);break;
            default:
                System.out.println("bad field value");
        }
    }
}
class Organization extends Contact implements Serializable {
    private String address;

    public Organization(String name, String address, String phoneNumber) {
        super(name,phoneNumber);
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        setLastEdit(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "Organization name: " + getName() + "\n"
                + "Address: " + getAddress() + "\n"
                + "Number: " + getPhoneNumber() + "\n"
                + "Time created: " + getTimeCreated() + "\n"
                + "Time last edit: " + getLastEdit();
    }

    @Override
    public String getFullName() {
        return getName();
    }

    @Override
    public List<String> allFieldsForChange() {
        List<String> fields = new ArrayList<>();
        fields.add("name");
        fields.add("address");
        fields.add("number");
        return fields;
    }

    @Override
    String getFieldVal(String fName) {
        switch (fName) {
            case "name": return getName();
            case "number": return getPhoneNumber();
            case "address": return getAddress();
            default:
                System.out.println("bad field name");return "";
        }
    }

    @Override
    void setFieldVal(String fName, String newVal) {
        switch (fName) {
            case "name": setName(newVal);break;
            case "address": setAddress(newVal);break;
            case "number": setPhoneNumber(newVal);break;
            default:
                System.out.println("bad field value");
        }

    }
}
class Validator {
    private final static Pattern phoneNumberPattern = Pattern.compile(
            "\\+?((\\([0-9A-Za-z]+\\)|[0-9A-Za-z]+)|" +
                    "([0-9A-Za-z]+[ -]\\([0-9A-Za-z]{2,}\\))|" +
                    "[0-9A-Za-z]+[ -][0-9A-Za-z]{2,})([ -][0-9A-Za-z]{2,}[ -]?)*"
    );

    public static boolean isValidPhoneNumber(String number) {
        Matcher matcher = phoneNumberPattern.matcher(number);
        return matcher.matches();
    }

    public static boolean isValidBirthDate(String birthDate)  {
        try {
            LocalDate.parse(birthDate,DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeException e) {
            return false;
        }
        return true;
    }

    public static boolean isValidGender(String gender) {
        return gender.equals("M") || gender.equals("F");
    }
}

class SerializationUtils implements Serializable {
    public static void serialize(Object obj, String fileName) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(bos);

            oos.writeObject(obj);
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object deserialize(String fileName)  {
        try {
            FileInputStream fis = new FileInputStream(fileName);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(bis);

            Object obj = ois.readObject();
            ois.close();
            return obj;
        }
        catch (IOException | ClassNotFoundException e)
        {

            return null;
        }



    }

}
