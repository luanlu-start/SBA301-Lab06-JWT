package fu.se.lab06jwt.responses;

// expiresIn is returned in milliseconds, matching security.jwt.expiration-time.
public record LoginResponse(String token, long expiresIn) {
}
