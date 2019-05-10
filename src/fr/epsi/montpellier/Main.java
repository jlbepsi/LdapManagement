package fr.epsi.montpellier;

import fr.epsi.montpellier.Ldap.InternalUserLdap;
import fr.epsi.montpellier.Ldap.LdapManager;
import fr.epsi.montpellier.Ldap.UserLdap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;


// Créer une libraririe JAR
// https://www.jetbrains.com/help/idea/creating-and-running-your-first-java-application.html

public class Main {
    private static LdapManager manager = null;

    public static void main(String[] args) {

        try {
            ResourceBundle rb = ResourceBundle.getBundle("config");

            manager = new LdapManager(rb.getString("adresse_ip"), rb.getString("admin_login"),
                    rb.getString("admin_password"),
                    rb.getString("base_dn"), rb.getString("ou_utilisateurs"), rb.getString("ou_groups"));


            if (args.length > 0) {

                for(int i=0; i < args.length; i++) {
                    String oneArg = args[i];
                    String[] oneArgValues = oneArg.split("=");

                    boolean erreur = false;
                    if (oneArgValues.length == 2) {
                        if (oneArgValues[0].startsWith("-")) {
                            String cmd = oneArgValues[0].substring(1);
                            String cmdOption = oneArgValues[1];
                            System.out.println(String.format("cmd=%s, cmdOption=%s", cmd, cmdOption));

                            switch (cmd) {
                                case "filename":
                                    System.out.println(String.format("createUserFromFile(%s);", cmdOption));
                                    createUserFromFile(cmdOption);
                                    break;

                                case "showClasses":
                                    System.out.println(String.format("showClasses(%s);", cmdOption));
                                    showClasses(cmdOption);
                                    break;

                            }
                        } else {
                            erreur = true;
                        }
                    } else {
                        erreur = true;
                    }
                    if (erreur) {
                        System.out.println(String.format("Un argument est malformé [%s], exemple: -file=fichier.txt", oneArgValues[0]));
                    }
                }
            } else {
                System.out.println("Aucun argument");

                // TODO Commenter les tests
                //showClasses("B2");

                String login = "test.v11", password = "123ABC";

                manager.updateUserPassword("test.v1", password);
                manager.updateUserPassword("test.v11", password);
                manager.updateUserPassword("test.v2", password);

                /*UserLdap user =  manager.authenticateUser(login, password);
                if (user == null)
                    System.out.println(login + ": NON Authentifié");
                else
                    System.out.println("Utilisateur '" + login + "' authentifié");

                System.out.println("Desativate user");
                manager.deactivateUser(login);
                user =  manager.authenticateUser(login, password);
                if (user == null)
                    System.out.println(login + ": NON Authentifié");
                else
                    System.out.println("Utilisateur '" + login + "' authentifié");
                System.out.println("Ativate user");
                manager.activateUser(login);
                user =  manager.authenticateUser(login, password);
                if (user == null)
                    System.out.println(login + ": NON Authentifié");
                else
                    System.out.println("Utilisateur '" + login + "' authentifié");*/
            }


        } catch (Exception ex) {
            System.out.println("Erreur : " + ex.getMessage());
        } finally {
            if (manager != null)
                manager.close();
        }
    }

    private static void createUserFromFile(String filename) {

        try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = in.readLine()) != null) {
                // process line here

                try {
                    String[] values = line.split(";");
                    if (values.length == 6) {
                        System.out.println(String.format("login=%s, nom=%s, prenom=%s, motDePasse=%s, email=%s, classe=%s",
                                values[2], values[0], values[1], values[3], values[4], values[5]));
                        manager.addUser(values[2], values[0], values[1], values[3], values[4], values[5], "ROLE_USER");
                    } else  if (values.length == 7) {
                        System.out.println(String.format("login=%s, nom=%s, prenom=%s, motDePasse=%s, email=%s, classe=%s, role=%s",
                                values[2], values[0], values[1], values[3], values[4], values[5], values[6]));
                        manager.addUser(values[2], values[0], values[1], values[3], values[4], values[5], values[6]);
                    } else {
                        System.err.println("Fichier mal formé, la ligne doit être: nom;prenom;login;motdepasse;mail;classe[;role]");
                    }
                } catch (Exception e) {
                    System.err.println("Fichier mal formé, la ligne doit être: nom;prenom;login;motdepasse;mail;classe[;role]");
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Fichier non trouvé");
        } catch (IOException e) {
            System.err.println("impossible de lire le fichier");
        } catch (Exception e) {
            System.err.println("Fichier mal formé, la ligne doit être: nom;prenom;login;motdepasse;mail;classe[;role]");
        }
    }

    private static void showClasses(String classe) {

        List<UserLdap> liste = null;
        if (classe.equals("all")) {
            System.out.println("*****        Tous les utilisateurs");
            liste = manager.listUsers();
        } else {
            System.out.println("*****        La classe " + classe);
            liste = manager.listUsers(classe);
        }

        if (liste == null) {
            System.out.println("Aucun résultat");
        } else {

            for (UserLdap user : liste) {
                System.out.println("Utilisateur : " + user.getLogin());
                System.out.println("  - Nom : " + user.getNom());
                System.out.println("  - Prénom : " + user.getPrenom());
                System.out.println("  - Nom complet : " + user.getNomComplet());
                System.out.println("  - Mail : " + user.getMail());
                System.out.println("  - Classe : " + user.getClasse());
                if (user.isBts()) {
                    System.out.println("  - BTS : oui");
                    System.out.println("  -   Parcours : " + user.getBtsParcours());
                    System.out.println("  -   Numéro : " + user.getBtsNumero());

                } else
                    System.out.println("  - BTS : non");

            }
        }
    }

    /*
        System.out.println("*****        Test ajout user B3");
        manager.addUser("test.java", "JAVA", "Test", "123ABC", "B3");
        manager.deleteUser("test.java");

        boolean valide = manager.isValidUser("test.java", "123ABC");
        System.out.println("isValid(test.java) = " + valide);

        List<UserLdap> liste = new ArrayList<UserLdap>();
        liste.add(manager.getUser("test.java"));
        showList(liste);
    */
}
