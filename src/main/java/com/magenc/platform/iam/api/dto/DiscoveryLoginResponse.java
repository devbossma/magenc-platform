// Exists because the discovery login returns available agencies as structured data, not just a JWT.
package com.magenc.platform.iam.api.dto;
import java.util.List;

public record DiscoveryLoginResponse(
        UserInfo user,
        List<AgencyInfo> memberships,
        String discoveryToken
) {
    public record UserInfo(String id, String email, String displayName) {}
    public record AgencyInfo(String agencySlug, String agencyDisplayName, String role, String loginUrl) {}
}
