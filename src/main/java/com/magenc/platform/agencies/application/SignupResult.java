// Exists because signup produces multiple things (tokens + agency + user) and the controller needs all of them.
package com.magenc.platform.agencies.application;

import com.magenc.platform.iam.application.TokenService;

public record SignupResult(
        TokenService.TokenPair tokens,
        String agencySlug,
        String agencyDisplayName,
        String userId
) {}
