// Exists because plan tiers drive isolation mode and feature gates: the enum makes these decisions compiler-checkable.
package com.magenc.platform.agencies.domain;

public enum PlanTier {
    TRIAL,
    STARTER,
    PRO,
    ENTERPRISE
}
