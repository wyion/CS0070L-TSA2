import java.util.Scanner;

// Main application class for the BookBridge TSA2 project.
public class BookBridgeApp {
    private static final Scanner INPUT = new Scanner(System.in);

    public static void main(String[] args) {
        LibraryService service = new LibraryService();

        System.out.println("=== BookBridge TSA2 Library Request System ===");
        System.out.println("Enter five valid book requests.");
        System.out.println();

        // Five direct calls avoid coordinated transaction arrays.
        collectAndProcessRequest(service, 1);
        collectAndProcessRequest(service, 2);
        collectAndProcessRequest(service, 3);
        collectAndProcessRequest(service, 4);
        collectAndProcessRequest(service, 5);

        printPolymorphismDemo();
        service.printSummary();
        INPUT.close();
    }

    // Reads one request, creates its domain objects, and displays its result.
    private static void collectAndProcessRequest(LibraryService service, int number) {
        System.out.println("--- Request " + number + " ---");

        String memberId = readRequiredText("Member ID: ");
        String memberName = readRequiredText("Member name: ");
        Member member = createMember(memberId, memberName, readMembershipType());
        String isbn = readRequiredText("ISBN: ");
        String title = readRequiredText("Book title: ");
        int requestedDays = readIntegerInRange("Requested loan days (1-30): ", 1, 30);
        boolean isAvailable = readAvailability();
        int lateDays = readNonnegativeInteger("Late days: ");

        LibraryBook book = new LibraryBook(isbn, title, isAvailable);
        RequestResult result = service.processRequest(
            member, book, requestedDays, lateDays
        );

        printRequestResult(number, result);
        System.out.println();
    }

    // Creates the correct member subtype from the selected membership type.
    private static Member createMember(String id, String name, String type) {
        switch (type) {
            case "STUDENT":
                return new StudentMember(id, name);
            case "FACULTY":
                return new FacultyMember(id, name);
            case "GUEST":
                return new GuestMember(id, name);
            default:
                throw new IllegalArgumentException("Unsupported membership type.");
        }
    }

    // Displays data from the returned loan and optional reservation objects.
    private static void printRequestResult(int number, RequestResult result) {
        LoanRecord loan = result.getLoanRecord();
        Member member = loan.getMember();
        LibraryBook book = loan.getBook();

        System.out.println("Request " + number + " result");
        System.out.println("Loan number: " + loan.getLoanNumber());
        System.out.println("Member: " + member.getName() + " (" + member.getMemberId() + ")");
        System.out.println("Membership: " + member.getMembershipType());
        System.out.println("Book: " + book.getTitle());
        System.out.println("ISBN: " + book.getIsbn());
        System.out.println("Requested days: " + loan.getRequestedDays());
        System.out.println("Available at request: " + (loan.wasAvailable() ? "Yes" : "No"));
        System.out.println("Outcome: " + loan.getOutcome());
        System.out.println("Estimated late charge: PHP " + loan.getEstimatedLateCharge());

        if (result.getReservation() != null) {
            // The interface reference calls the Reservation implementation.
            Notifiable notice = result.getReservation();
            System.out.println(notice.getNotificationMessage());
        }
    }

    // Demonstrates dynamic dispatch through the common abstract Member type.
    private static void printPolymorphismDemo() {
        System.out.println("=== Runtime Polymorphism Demo ===");
        Member member = new StudentMember("DEMO-S", "Student Demo");
        printMemberPolicy(member);
        member = new FacultyMember("DEMO-F", "Faculty Demo");
        printMemberPolicy(member);
        member = new GuestMember("DEMO-G", "Guest Demo");
        printMemberPolicy(member);
    }

    // The declared type is Member; overridden methods use the actual subtype.
    private static void printMemberPolicy(Member member) {
        System.out.println(
            member.getMembershipType()
                + " policy: max " + member.getMaximumLoanDays()
                + " days; PHP " + member.getDailyFine() + "/day"
        );
    }

    // Reads nonblank text and keeps prompting after blank input.
    private static String readRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = INPUT.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be blank. Please try again.");
        }
    }

    // Reads one of the permitted membership types.
    private static String readMembershipType() {
        while (true) {
            System.out.print("Membership type (STUDENT/FACULTY/GUEST): ");
            String type = INPUT.nextLine().trim().toUpperCase();
            if (type.equals("STUDENT") || type.equals("FACULTY") || type.equals("GUEST")) {
                return type;
            }
            System.out.println("Enter STUDENT, FACULTY, or GUEST.");
        }
    }

    // Reads an integer and repeats the prompt if it is not numeric.
    private static int readInteger(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = INPUT.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    // Reads the requested loan period, restricted to 1 through 30 days.
    private static int readIntegerInRange(String prompt, int minimum, int maximum) {
        while (true) {
            int value = readInteger(prompt);
            if (value >= minimum && value <= maximum) {
                return value;
            }
            System.out.println("Enter a number from " + minimum + " to " + maximum + ".");
        }
    }

    // Reads late days and rejects negative values.
    private static int readNonnegativeInteger(String prompt) {
        while (true) {
            int value = readInteger(prompt);
            if (value >= 0) {
                return value;
            }
            System.out.println("Late days cannot be negative. Please try again.");
        }
    }

    // Reads book availability as yes or no.
    private static boolean readAvailability() {
        while (true) {
            System.out.print("Is the book available? (yes/no): ");
            String answer = INPUT.nextLine().trim().toLowerCase();
            if (answer.equals("yes")) {
                return true;
            }
            if (answer.equals("no")) {
                return false;
            }
            System.out.println("Enter yes or no.");
        }
    }
}

// Shared member identity and policy contract for all member subtypes.
abstract class Member {
    private final String memberId;
    private final String name;

    Member(String memberId, String name) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("Member ID cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Member name cannot be blank.");
        }
        this.memberId = memberId.trim();
        this.name = name.trim();
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public abstract int getMaximumLoanDays();

    public abstract int getDailyFine();

    public abstract String getMembershipType();
}

// Student-specific loan policy.
class StudentMember extends Member {
    StudentMember(String memberId, String name) {
        super(memberId, name);
    }

    @Override
    public int getMaximumLoanDays() {
        return 14;
    }

    @Override
    public int getDailyFine() {
        return 5;
    }

    @Override
    public String getMembershipType() {
        return "STUDENT";
    }
}

// Faculty-specific loan policy.
class FacultyMember extends Member {
    FacultyMember(String memberId, String name) {
        super(memberId, name);
    }

    @Override
    public int getMaximumLoanDays() {
        return 30;
    }

    @Override
    public int getDailyFine() {
        return 2;
    }

    @Override
    public String getMembershipType() {
        return "FACULTY";
    }
}

// Guest-specific loan policy.
class GuestMember extends Member {
    GuestMember(String memberId, String name) {
        super(memberId, name);
    }

    @Override
    public int getMaximumLoanDays() {
        return 7;
    }

    @Override
    public int getDailyFine() {
        return 10;
    }

    @Override
    public String getMembershipType() {
        return "GUEST";
    }
}

// A capability that allows an object to provide a notice message.
interface Notifiable {
    String getNotificationMessage();
}

// Book identity and availability; reserve() changes availability safely.
class LibraryBook {
    private final String isbn;
    private final String title;
    private boolean available;

    LibraryBook(String isbn, String title, boolean available) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN cannot be blank.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be blank.");
        }
        this.isbn = isbn.trim();
        this.title = title.trim();
        this.available = available;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAvailable() {
        return available;
    }

    public void reserve() {
        available = false;
    }
}

// A loan request, its outcome, and estimated late charge.
class LoanRecord {
    private static int nextLoanNumber = 1;

    private final int loanNumber;
    private final Member member;
    private final LibraryBook book;
    private final int requestedDays;
    private final int lateDays;
    private final boolean availableAtRequest;
    private final String outcome;
    private boolean returned;

    LoanRecord(Member member, LibraryBook book, int requestedDays, int lateDays) {
        if (member == null || book == null) {
            throw new IllegalArgumentException("Member and book are required.");
        }
        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException("Requested days must be from 1 to 30.");
        }
        if (lateDays < 0) {
            throw new IllegalArgumentException("Late days cannot be negative.");
        }

        this.loanNumber = nextLoanNumber++;
        this.member = member;
        this.book = book;
        this.requestedDays = requestedDays;
        this.lateDays = lateDays;
        this.availableAtRequest = book.isAvailable();

        if (!availableAtRequest) {
            this.outcome = "RESERVED";
        } else if (requestedDays <= member.getMaximumLoanDays()) {
            this.outcome = "APPROVED";
        } else {
            this.outcome = "DENIED";
        }
    }

    public int getLoanNumber() {
        return loanNumber;
    }

    public Member getMember() {
        return member;
    }

    public LibraryBook getBook() {
        return book;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public int getLateDays() {
        return lateDays;
    }

    public boolean wasAvailable() {
        return availableAtRequest;
    }

    public String getOutcome() {
        return outcome;
    }

    public int getEstimatedLateCharge() {
        return lateDays * member.getDailyFine();
    }

    public boolean isReturned() {
        return returned;
    }

    // Changes return status through a controlled behavior method.
    public void markReturned() {
        returned = true;
    }
}

// A reservation is created for an unavailable book and can provide a notice.
class Reservation implements Notifiable {
    private static int nextReservationNumber = 1;

    private final int reservationNumber;
    private final Member member;
    private final LibraryBook book;

    Reservation(Member member, LibraryBook book) {
        if (member == null || book == null) {
            throw new IllegalArgumentException("Member and book are required.");
        }
        if (book.isAvailable()) {
            throw new IllegalArgumentException("Only unavailable books can be reserved.");
        }
        this.reservationNumber = nextReservationNumber++;
        this.member = member;
        this.book = book;
    }

    public int getReservationNumber() {
        return reservationNumber;
    }

    public Member getMember() {
        return member;
    }

    public LibraryBook getBook() {
        return book;
    }

    @Override
    public String getNotificationMessage() {
        return "Reservation " + reservationNumber + " recorded for " + book.getTitle() + ".";
    }
}

// Holds the result of processing one request without using parallel arrays.
class RequestResult {
    private final LoanRecord loanRecord;
    private final Reservation reservation;

    RequestResult(LoanRecord loanRecord, Reservation reservation) {
        this.loanRecord = loanRecord;
        this.reservation = reservation;
    }

    public LoanRecord getLoanRecord() {
        return loanRecord;
    }

    public Reservation getReservation() {
        return reservation;
    }
}

// Coordinates domain objects and maintains the operational summary.
class LibraryService {
    private int approvedCount;
    private int reservedCount;
    private int deniedCount;
    private int totalEstimatedCharge;
    private int greatestRequestedDays;
    private int greatestRequestNumber;
    private int processedCount;

    public RequestResult processRequest(
        Member member,
        LibraryBook book,
        int requestedDays,
        int lateDays
    ) {
        LoanRecord loan = new LoanRecord(member, book, requestedDays, lateDays);
        Reservation reservation = null;

        if (loan.getOutcome().equals("APPROVED")) {
            approvedCount++;
        } else if (loan.getOutcome().equals("RESERVED")) {
            reservedCount++;
            book.reserve();
            reservation = new Reservation(member, book);
        } else {
            deniedCount++;
        }

        processedCount++;
        totalEstimatedCharge += loan.getEstimatedLateCharge();
        if (requestedDays > greatestRequestedDays) {
            greatestRequestedDays = requestedDays;
            greatestRequestNumber = processedCount;
        }

        return new RequestResult(loan, reservation);
    }

    // Prints totals after all five requests have been processed.
    public void printSummary() {
        System.out.println("=== Final Summary ===");
        System.out.println("Approved requests: " + approvedCount);
        System.out.println("Reserved requests: " + reservedCount);
        System.out.println("Denied requests: " + deniedCount);
        System.out.println(
            "Greatest requested loan: " + greatestRequestedDays
                + " days (Request " + greatestRequestNumber + ")"
        );
        System.out.println("Total estimated late charge: PHP " + totalEstimatedCharge);
    }
}
