package com.barboya.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContactsTest {

    @ParameterizedTest
    @CsvSource({
            "300 123 4567,     +573001234567",
            "607 748 0000,     +576077480000",
            "(607) 748-0000,   +576077480000",
            "57 300 123 4567,  +573001234567",
            "+57 607 748 0000, +576077480000",
            "+1 202 555 0123,  +12025550123"})
    void colombianNumbersGetTheCountryCode(String written, String stored) {
        assertThat(Contacts.phone(written)).isEqualTo(stored);
    }
}
