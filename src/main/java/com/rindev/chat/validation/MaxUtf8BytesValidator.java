package com.rindev.chat.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class MaxUtf8BytesValidator implements ConstraintValidator<MaxUtf8Bytes, String> {

    private int maximum;

    @Override
    public void initialize(MaxUtf8Bytes constraint) {
        maximum = constraint.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        // Every UTF-16 code unit requires at least one UTF-8 byte for valid input.
        if (value.length() > maximum) {
            return false;
        }
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        try {
            return encoded.length <= maximum;
        } finally {
            Arrays.fill(encoded, (byte) 0);
        }
    }
}
