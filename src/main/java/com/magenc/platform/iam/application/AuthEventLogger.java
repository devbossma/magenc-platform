// Exists because SOC 2 auditors ask for a tamper-evident log of every auth event. Hash chaining makes tampering detectable.
package com.magenc.platform.iam.application;

import java.util.UUID;

public interface AuthEventLogger {

    void logSuccess(String eventType, UUID userId, UUID agencyId, UUID sessionId,
                    String ipAddress, String userAgent);

    void logFailure(String eventType, UUID userId, UUID agencyId,
                    String reason, String ipAddress, String userAgent);
}
