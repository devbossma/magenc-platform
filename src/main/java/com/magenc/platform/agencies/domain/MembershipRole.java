// Exists because roles are per-membership not per-user: Sarah can be OWNER at Acme and MEMBER at Beta simultaneously.
package com.magenc.platform.agencies.domain;

public enum MembershipRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}
