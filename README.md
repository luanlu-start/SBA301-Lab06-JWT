# SBA301 Lab 06 - Spring Security and JWT

Backend case study: Orchid Management user registration, login, and protected user APIs.

## Included

- Java 21, Maven, Spring Boot 3.4.5, Spring Security, Spring Data JPA, SQL Server, Validation, and JJWT 0.12.6.
- BCrypt password hashing; raw passwords are never saved or returned.
- Stateless bearer authentication. JwtAuthenticationFilter validates signature and expiry before setting the principal in SecurityContext.
- POST /auth/signup and POST /auth/login are public. GET /users/me and GET /users require a valid bearer token.
- Core Lab 06 has no role restriction. USER/ADMIN is an optional advanced extension in the guide.
- Automated security tests use an isolated H2 database, so test users do not enter SQL Server.

## Project structure

~~~text
src/main/java/fu/se/lab06jwt/
├── Lab06JwtApplication.java
├── configurations/
│   ├── ApplicationConfiguration.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfiguration.java
├── controllers/
│   ├── AuthenticationController.java
│   └── UserController.java
├── dtos/
│   ├── LoginUserDto.java
│   └── RegisterUserDto.java
├── entities/User.java
├── exceptions/
│   ├── ApiError.java
│   ├── DuplicateEmailException.java
│   └── GlobalExceptionHandler.java
├── repositories/UserRepository.java
├── responses/
│   ├── LoginResponse.java
│   └── UserResponse.java
└── services/
    ├── AuthenticationService.java
    ├── JwtService.java
    └── UserService.java
~~~

## Local setup

1. Start SQL Server. The guide's database name is OrchidJWTLab. A guarded create script is at database/create-database.sql; it does not drop or reset an existing database.
2. Copy application-local.properties.example to application-local.properties in this project folder.
3. Set DB_USER and DB_PASSWORD for your SQL Server login. Set JWT_SECRET to Base64 text that decodes to at least 32 random bytes. The local file is ignored by Git.
4. In IntelliJ, open this folder as a Maven project, use JDK 21, and set the Run Configuration working directory to this project folder. Alternatively, run the included PowerShell script.

The application listens at http://localhost:8005. Hibernate creates/updates the users table in OrchidJWTLab. The default token lifetime is 3,600,000 milliseconds (one hour).

## Run and test

~~~powershell
PowerShell -ExecutionPolicy Bypass -File run.ps1 -Test
PowerShell -ExecutionPolicy Bypass -File run.ps1
~~~

To use Maven directly from this folder:

~~~powershell
mvn test
mvn spring-boot:run
~~~

For IntelliJ, run Lab06JwtApplication. Test settings are separate from SQL Server and are activated automatically by the test class.

## API contract

| Method | Endpoint | Access | Success |
| --- | --- | --- | --- |
| POST | /auth/signup | Public | 201 with safe user fields |
| POST | /auth/login | Public | 200 with token and expiresIn (milliseconds) |
| GET | /users/me | Bearer token | 200 with the current user |
| GET | /users | Bearer token | 200 with users; no password/hash fields |

Expected failures include invalid input 400, duplicate email 409, invalid credentials 401, and missing/invalid/expired bearer token 401.

## Postman

Import postman/SBA301_Lab06_JWT.postman_collection.json and postman/SBA301_Lab06_Local.postman_environment.json. Select the environment, run Signup then Login, and the login request saves token for protected requests. The collection includes duplicate signup, wrong password, missing/malformed/tampered token, an expired-token procedure, and validation checks.

For a manual expired-token check, temporarily set security.jwt.expiration-time to 5000, restart, log in, wait more than five seconds, and call /users/me; restore 3600000 afterward. Automated tests already cover an expired JWT without changing local SQL data.

## Request flow

~~~text
POST /auth/login
  -> AuthenticationController
  -> AuthenticationService
  -> AuthenticationManager / DaoAuthenticationProvider
  -> UserDetailsService / UserRepository / BCrypt match
  -> JwtService signs a token

GET /users/me + Authorization: Bearer <token>
  -> SecurityFilterChain
  -> JwtAuthenticationFilter verifies signature and expiry
  -> UserDetailsService loads the subject
  -> SecurityContext receives Authentication
  -> authenticated() authorization rule
  -> UserController
~~~

The JWT payload is signed, not encrypted. Do not put passwords, database credentials, or other secrets in its claims.
