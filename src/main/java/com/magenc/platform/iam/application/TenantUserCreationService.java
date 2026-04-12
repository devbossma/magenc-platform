package com.magenc.platform.iam.application;

import com.magenc.platform.iam.domain.EmailAlreadyTakenException;
import com.magenc.platform.iam.domain.HashedPassword;
import com.magenc.platform.iam.domain.PasswordHasher;
import com.magenc.platform.iam.domain.User;
import com.magenc.platform.iam.domain.UserRepository;
import com.magenc.platform.iam.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUserCreationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public TenantUserCreationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    @Transactional
    public TokenService.TokenPair createOwnerAndIssueTokens(SignupService.SignupCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyTakenException(command.email());
        }
        HashedPassword hashed = passwordHasher.hash(command.password());
        User owner = User.create(
                command.email(),
                hashed,
                command.displayName(),
                UserRole.OWNER);
        owner = userRepository.save(owner);
        return tokenService.issueTokens(owner, command.tenantSlug());
    }
}