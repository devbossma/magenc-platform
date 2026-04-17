// Exists because agency lifecycle states determine what operations are allowed: provisioning agencies can't accept logins.
package com.magenc.platform.agencies.domain;

public enum AgencyStatus {
    PROVISIONING,
    ACTIVE,
    SUSPENDED,
    DELETED
}
