package com.magenc.platform.iam.domain;

public enum UserStatus {

    /** Active and can sign in. */
    ACTIVE,

    /** Invited but has not yet completed signup. Reserved for future. */
    INVITED,

    /** Suspended by an admin. Cannot sign in but data is preserved. */
    SUSPENDED
}
