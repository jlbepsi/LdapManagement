package fr.epsi.montpellier.Ldap.integrationtest;

import fr.epsi.montpellier.Ldap.LdapManager;
import fr.epsi.montpellier.Ldap.UserLdap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.NamingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class LdapManagerTest {

    static LdapManager ldapManager;

    @BeforeAll
    static void connectToLDAP() throws NamingException {
        ResourceBundle rb = ResourceBundle.getBundle("test");

        String port = "389";
        try {
            port = rb.getString("ldap_port");
        } catch (Exception ignored) {
        }

        ldapManager = new LdapManager(rb.getString("adresse_ip"), port, rb.getString("admin_login"),
                rb.getString("admin_password"),
                rb.getString("base_dn"), rb.getString("ou_utilisateurs"), rb.getString("ou_groups"),
                rb.getString("groupe_etudiants"));
    }

    @Test
    public void listAllTest() {
        // Given : see connectToLDAP
        // When
        List<UserLdap> list = ldapManager.listUsers();
        // Then
        assertThat(list.size()).isGreaterThan(0);
    }

    @Test
    public void listClasseTest() {
        // Given : see connectToLDAP
        // When
        List<UserLdap> list = ldapManager.listUsers("B2");
        // Then
        assertThat(list).filteredOn( user -> user.getClasse().equals("B2")).hasSize(list.size());
    }

    @Test
    public void listGroupeTest() {
        // Given : see connectToLDAP
        // When
        List<UserLdap> list = ldapManager.listUsersOfGroups("B3");
        // Then
        assertThat(list).filteredOn( user -> user.getClasse().equals("B3")).hasSize(list.size());
    }

    @Test
    public void getUserFromDNTest() {
        // Given : see connectToLDAP
        String dn = "uid=test.v8,ou=Pedago,ou=Utilisateurs,dc=montpellier,dc=lan";
        // When
        UserLdap user = ldapManager.getUserFromDN(dn);
        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUserDN()).isEqualTo(dn);
    }

    @Test
    public void getUserTest() {
        // Given : see connectToLDAP
        String login = "test.v8";
        // When
        UserLdap user = ldapManager.getUser(login);
        // Then
        assertThat(user).isNotNull();
        assertThat(user.getLogin()).isEqualTo(login);
    }

    @Test
    public void addUserTest() {
        // Given : see connectToLDAP
        DateFormat dateFormat = new SimpleDateFormat("DHHmmsS");
        String login = "test." + dateFormat.format(Calendar.getInstance().getTime());
        UserLdap user = null;
        try {
            user = new UserLdap(login, "TEST", "new", "123456", "B3", "etudiant3@test.com", "ROLE_USER");
            // When
            ldapManager.addUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            fail("dddUser");
        }

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getLogin()).isEqualTo(login);
    }


    @Test
    public void authenticateUserTest() {
        // Given : see connectToLDAP
        String login = "test.v8", password = "123ABC";
        UserLdap user;

        // When
        user = ldapManager.authenticateUser(login, password);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getLogin()).isEqualTo(login);
    }

    @AfterAll
    static void disconnectLDAP() {
        ldapManager.close();
    }

}
