package fu.se.lab06jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.lab06jwt.repositories.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityContractTest {

    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String EMAIL = "student06@example.com";
    private static final String PASSWORD = "P@ssword123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void signupSavesBCryptHashAndDoesNotReturnPassword() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists());

        var savedUser = userRepository.findByEmail(EMAIL).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, savedUser.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(PASSWORD, savedUser.getPassword()));
    }

    @Test
    void duplicateSignupReturnsConflict() throws Exception {
        signup(EMAIL);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(EMAIL)))
                .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertEquals(1, userRepository.count());
    }

    @Test
    void invalidSignupReturnsBadRequest() throws Exception {
        String invalidBody = objectMapper.writeValueAsString(Map.of(
                "fullName", "Student",
                "email", "not-an-email",
                "password", "short"
        ));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, userRepository.count());
    }

    @Test
    void loginIssuesTokenAndTokenOpensProtectedUserEndpoints() throws Exception {
        signup(EMAIL);
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EMAIL))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(not(containsString("password"))));
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void wrongPasswordIsUnauthorizedAndReturnsNoToken() throws Exception {
        signup(EMAIL);
        String invalidLogin = objectMapper.writeValueAsString(Map.of(
                "email", EMAIL,
                "password", "wrong-password"
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLogin))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void malformedTamperedAndExpiredTokensAreUnauthorized() throws Exception {
        signup(EMAIL);
        String validToken = login(EMAIL, PASSWORD);
        String[] tokenParts = validToken.split("\\.");
        String payload = new String(Decoders.BASE64URL.decode(tokenParts[1]), StandardCharsets.UTF_8);
        String changedPayload = payload.replace(EMAIL, "attacker@example.com");
        tokenParts[1] = Encoders.BASE64URL.encode(changedPayload.getBytes(StandardCharsets.UTF_8));
        String tamperedToken = String.join(".", tokenParts);

        assertUnauthorized("abc");
        assertUnauthorized(tamperedToken);
        assertUnauthorized(expiredToken());
    }

    private void signup(String email) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email)))
                .andExpect(status().isCreated());
    }

    private String signupBody(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "fullName", "SBA301 Student",
                "email", email,
                "password", PASSWORD
        ));
    }

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(60000))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String expiredToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(EMAIL)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
