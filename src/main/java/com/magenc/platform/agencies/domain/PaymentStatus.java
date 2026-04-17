// Exists because payment gates feature access: SIMULATED_PAID bypasses Stripe for dev/demo while keeping the check path identical.
package com.magenc.platform.agencies.domain;

public enum PaymentStatus {
    NONE,
    SIMULATED_PAID,
    STRIPE_ACTIVE,
    STRIPE_PAST_DUE
}
