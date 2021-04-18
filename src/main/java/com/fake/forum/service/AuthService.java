package com.fake.forum.service;

import com.fake.forum.dto.RegisterRequest;
import com.fake.forum.exceptions.SpringRedditException;
import com.fake.forum.model.NotificationEmail;
import com.fake.forum.model.User;
import com.fake.forum.model.VerificationToken;
import com.fake.forum.repository.UserRepository;
import com.fake.forum.repository.VerificationTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository repository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;

    @Transactional
    public void signUp(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreated(Instant.now());
        user.setEnabled(false);
        repository.save(user);
        String token = generateVerificationToken(user);
        mailService.sendMail(
                new NotificationEmail("Please activate your accont",
                        user.getEmail(),
                        "Please click on the below url: http://localhost:8080/api/auth/accountVerification/"
                                + token));
    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    public void verifyAccount(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);
        verificationToken.orElseThrow(() -> new SpringRedditException("Invalid token"));
        fetchUserAndEnable(verificationToken.get());
    }

    @Transactional
    private void fetchUserAndEnable(VerificationToken verificationToken) {
        String username = verificationToken.getUser().getUsername();
        User user = repository.findByUsername(username).
                orElseThrow(() -> new SpringRedditException("User not found with name: " + username));
        user.setEnabled(true);
        repository.save(user);
    }
}
