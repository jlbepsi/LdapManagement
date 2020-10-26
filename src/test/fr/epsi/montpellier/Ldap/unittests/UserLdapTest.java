package fr.epsi.montpellier.Ldap.unittests;

import fr.epsi.montpellier.Ldap.LdapException;
import fr.epsi.montpellier.Ldap.UserLdap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class UserLdapTest {

    @Test
    public void loginTest() {
        // Arrange
        String login = "prenom . NOM";
        UserLdap user = new UserLdap();

        // Act
        try {
            user.setLogin(login);
        } catch (LdapException exception) {
            exception.printStackTrace();
            fail("setLogin");
        }

        // Assert
        assertEquals("prenom.nom", user.getLogin());
    }
    @Test
    public void loginEmptyTest() {
        // Given
        String login = null;
        UserLdap user = new UserLdap();

        // When && Then
        assertThatExceptionOfType(LdapException.class)
                .isThrownBy(() -> user.setLogin(login))
                .withNoCause();
    }

    @Test
    public void nomPrenomTest() {
        // Arrange
        String prenom = "preNom ";
        String nom = "n om ";
        UserLdap user = new UserLdap();

        // Act
        try {
            user.setNom(nom);
            user.setPrenom(prenom);
        } catch (LdapException exception) {
            exception.printStackTrace();
            fail("setNom or setPrenom");
        }

        // Assert
        assertEquals("NOM", user.getNom());
        assertEquals("Prenom", user.getPrenom());
    }


    @ParameterizedTest
    @CsvSource(value = {"Prenom.N OM@epsi.FR:prenom.nom@epsi.fr", "prenom@epsi.fr:prenom@epsi.fr", "P@A.fr:p@a.fr"}, delimiter = ':')
    public void mailValidTest(String input, String expected) {
        // Given
        UserLdap user = new UserLdap();

        // When
        try {
            user.setMail(input);
        } catch (LdapException exception) {
            exception.printStackTrace();
            fail("setMail");
        }

        // Then
        assertThat(user.getMail()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalidMail", "invalidMail@", "@invalidMail", "@invalidMail.fr"})
    public void mailInvalidTest(String input) {
        // Given
        UserLdap user = new UserLdap();

        // When && Then
        assertThatExceptionOfType(LdapException.class)
                .isThrownBy(() -> user.setMail(input))
                .withNoCause();
    }


    @ParameterizedTest
    @CsvSource(value = {"role_User:ROLE_USER", "Role_Autre:ROLE_USER,ROLE_AUTRE",
            "ROLE_USER, Role_Autre:ROLE_USER,ROLE_AUTRE", "ROLE_Autre,Role_USER:ROLE_AUTRE,ROLE_USER"}, delimiter = ':')
    public void roleValidTest(String input, String expected) {
        // Given
        UserLdap user = new UserLdap();

        // When
        try {
            user.setRole(input);
        } catch (LdapException exception) {
            exception.printStackTrace();
            fail("setRole");
        }

        // Then
        assertThat(user.getRole()).isEqualTo(expected);
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "ROLE-invalid", "roleInvalid", "invalid"})
    public void roleInvalidTest(String input) {
        // Given
        UserLdap user = new UserLdap();

        // When && Then
        assertThatExceptionOfType(LdapException.class)
                .isThrownBy(() -> user.setRole(input))
                .withNoCause();
    }
}
