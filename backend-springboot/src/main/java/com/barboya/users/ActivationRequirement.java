package com.barboya.users;

/** Condicion que un modulo exige antes de activar una cuenta. */
public interface ActivationRequirement {

    /** @throws com.barboya.common.ApiException si la cuenta aun no puede operar */
    void verify(User user);
}
