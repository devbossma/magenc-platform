// Exists because auth audit entries must be hash-chained and append-only: JDBC gives us atomic INSERT with hash computation.
package com.magenc.platform.iam.infrastructure;

import com.magenc.platform.iam.application.AuthEventLogger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcAuthEventLogger implements AuthEventLogger {

    private final JdbcTemplate jdbc;

    public JdbcAuthEventLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void logSuccess(String eventType, UUID userId, UUID agencyId, UUID sessionId,
                           String ipAddress, String userAgent) {
        insertEvent(eventType, userId, agencyId, sessionId, true, null, ipAddress, userAgent);
    }

    @Override
    public void logFailure(String eventType, UUID userId, UUID agencyId,
                           String reason, String ipAddress, String userAgent) {
        insertEvent(eventType, userId, agencyId, null, false, reason, ipAddress, userAgent);
    }

    private void insertEvent(String eventType, UUID userId, UUID agencyId, UUID sessionId,
                             boolean success, String failureReason, String ipAddress, String userAgent) {
        // Fetch previous hash for chaining
        String previousHash = jdbc.query(
                "SELECT current_hash FROM admin.auth_event ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("current_hash"))
                .stream().findFirst().orElse("GENESIS");

        // Compute current hash
        String payload = String.join("|", eventType, String.valueOf(userId),
                String.valueOf(agencyId), String.valueOf(success), String.valueOf(failureReason));
        String currentHash = sha256(previousHash + "|" + payload);

        jdbc.update("""
                INSERT INTO admin.auth_event
                    (occurred_at, event_type, platform_user_id, agency_id, session_id,
                     ip_address, user_agent, success, failure_reason, previous_hash, current_hash)
                VALUES (NOW(), ?, ?, ?, ?, ?::inet, ?, ?, ?, ?, ?)
                """,
                eventType, userId, agencyId, sessionId,
                ipAddress, userAgent, success, failureReason, previousHash, currentHash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
