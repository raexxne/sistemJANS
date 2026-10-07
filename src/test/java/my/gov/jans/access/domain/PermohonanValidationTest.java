package my.gov.jans.access.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PermohonanValidationTest {

    @Test
    void rejectsMissingRequiredApplicationFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<String> invalidFields = validator.validate(new Permohonan()).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertTrue(invalidFields.containsAll(Set.of(
                    "emailWakil", "visitDate", "visitTime", "locationType", "locationName", "purpose")));
        }
    }
}
