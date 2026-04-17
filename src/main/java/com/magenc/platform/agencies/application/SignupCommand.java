// Exists because signup inputs travel from controller to service as an immutable record, not scattered method parameters.
package com.magenc.platform.agencies.application;

import com.magenc.platform.iam.domain.Email;
import com.magenc.platform.iam.domain.RawPassword;

public record SignupCommand(
        String tenantSlug,
        String agencyDisplayName,
        Email email,
        RawPassword password,
        String displayName
) {}
