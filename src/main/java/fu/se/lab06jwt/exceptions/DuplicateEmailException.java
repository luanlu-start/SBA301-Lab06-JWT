package fu.se.lab06jwt.exceptions;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Email already exists");
    }
}
