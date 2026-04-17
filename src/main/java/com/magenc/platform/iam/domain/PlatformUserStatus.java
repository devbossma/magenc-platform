// Exists because user lifecycle states are a closed set that the compiler should enforce.
package com.magenc.platform.iam.domain;

public enum PlatformUserStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
