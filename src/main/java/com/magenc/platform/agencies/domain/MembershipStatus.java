// Exists because memberships have their own lifecycle: invited users haven't accepted yet, removed users lose access but keep audit trail.
package com.magenc.platform.agencies.domain;

public enum MembershipStatus {
    ACTIVE,
    INVITED,
    REMOVED
}
