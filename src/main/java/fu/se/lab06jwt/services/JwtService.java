package fu.se.lab06jwt.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    private SecretKey signingKey;

    @PostConstruct
    void initializeSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT secret must decode to at least 32 bytes.");
            }
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IllegalStateException("JWT secret must be valid Base64 and contain at least 32 bytes.", exception);
        }
        if (jwtExpiration <= 0) {
            throw new IllegalStateException("JWT expiration-time must be greater than zero.");
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String generateToken(UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        return userDetails.getUsername().equals(claims.getSubject())
                && claims.getExpiration() != null
                && claims.getExpiration().after(new Date());
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
