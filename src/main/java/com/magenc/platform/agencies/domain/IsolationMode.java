// Exists because starter/pro use schema isolation while enterprise uses database isolation: this enum drives the routing decision.
package com.magenc.platform.agencies.domain;

public enum IsolationMode {
    SCHEMA,
    DATABASE
}
