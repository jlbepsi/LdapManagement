package fr.epsi.montpellier.Ldap;


import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.regex.Pattern;


public class UserLdap {
    @JsonIgnore
    private String userDN;

    private boolean isActive;

    private String login;
    private String nom;
    private String prenom;
    private String genre = "0";
    private String motDePasse;
    private String classe;
    private String groupe;
    private String mail;
    private String role;
    private boolean bts;
    private String btsParcours = "0";
    private String btsNumero = "0";

    @JsonIgnore
    private String classePrecedente;

    public UserLdap() {

    }

    public UserLdap(String login, String nom, String prenom, String motDePasse, String classe, String mail, String role)
        throws LdapException {

        setLogin(login);
        setNom(nom);
        setPrenom(prenom);
        setMotDePasse(motDePasse);
        setMail(mail);
        this.classe = classe;
        setRole(role);
        this.bts = false;
    }

    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean active) {
        isActive = active;
    }

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) throws LdapException {
        this.login = sanitizeString("Login", login);
    }

    public String getNom() {
        return nom;
    }
    public void setNom(String nom) throws LdapException {
        this.nom = sanitizeString("Nom", nom, -1);
    }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) throws LdapException {
        String value = sanitizeString("Prénom", prenom);
        this.prenom = value.substring(0, 1).toUpperCase() + value.substring(1);
    }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getNomComplet() { return this.nom + " " + this.prenom; }

    public String getMail() { return mail; }
    public void setMail(String mail) throws LdapException {
        String value = sanitizeString("Mail", mail, 1);
        if (Pattern.matches("[a-z0-9._%-]+@[a-z0-9._%-]+\\.[a-z]{2,4}", value)) {
            this.mail = value;
        } else {
            throw new LdapException("Mail invalide");
        }
    }

    public String getClasse() {
        return classe;
    }
    public void setClasse(String classe) {
        this.classe = classe.toUpperCase();
    }

    public String getGroupe() {
        return groupe;
    }

    public void setGroupe(String groupe) {
        this.groupe = groupe;
    }

    public String getRole() { return role; }
    public void setRole(String role) throws LdapException {
        // Pas d'espace et MAJUSCULE
        role = sanitizeString("Rôle", role, -1);
        // Pas de virgule à la fin
        if (role.endsWith(",")) {
            role = role.substring(0, role.length() -1);
        }
        // Le rôle ROLE_USER doit toujours se trouver dans le rôle
        if (! role.contains("ROLE_USER")) {
            role = "ROLE_USER," + role;
        }
        // Chaque rôle doit commncer par ROLE_
        String[] roles = role.split(",");
        for (String value : roles) {
            if (! value.startsWith("ROLE_")) {
                throw new LdapException("Un rôle doit commencer par ROLE_");
            }
        }
        this.role = role;
    }

    public String getUserDN() { return userDN; }
    public void setUserDN(String userDN) {
        this.userDN = userDN;
    }


    /*
        BTS
     */
    public boolean isBts() {
        return bts;
    }
    public void setBts(boolean bts) {
        // On vérifie que la classe est B1 ou B2
        if (bts && ! (classe.equalsIgnoreCase("B1") || classe.equalsIgnoreCase("B2"))) {
            bts = false;
        }
        this.bts = bts;
    }

    public String getBtsParcours() {
        return btsParcours;
    }
    public void setBtsParcours(String btsParcours) {
        if (btsParcours == null || btsParcours.isEmpty()) {
            // Le texte ne doit pas être vide (contrainte OpenLDAP)
            btsParcours = "0";
        }
        this.btsParcours = btsParcours.toUpperCase();
    }

    public String getBtsNumero() {
        return btsNumero;
    }
    public void setBtsNumero(String btsNumero) {
        if (btsNumero == null || btsNumero.isEmpty()) {
            // Le numéro ne doit pas être vide (contrainte OpenLDAP)
            btsNumero = "0000000000";
        }
        this.btsNumero = btsNumero;
    }

    /*
        Classe précédente utilisée pour le basculement en début d'année
     */
    public String getClassePrecedente() {
        return classePrecedente;
    }

    public void setClassePrecedente(String classePrecedente) {
        this.classePrecedente = classePrecedente;
    }

    @JsonIgnore
    public String getDescription() {
        return String.format("%s(%s), %s %s (%s), %s, Groupe=(%s), %s, %s, %s classePrecedente=%s",
                login, isActive?"Actif":"Inactif", nom, prenom, genre, classe, groupe, getMail(), role,
                bts ? "BTS=" + btsParcours + "(" +btsNumero +"), ":"", classePrecedente);
    }

    private String sanitizeString(String attributeName, String value) throws LdapException {
        return sanitizeString(attributeName, value, 1);
    }
    private String sanitizeString(String attributeName, String value, int toLower) throws LdapException {
        if (value == null) {
            throw new LdapException(String.format("La valeur de '%s' ne peut pas être nulle", attributeName));
        }
        // Pas d'espace
        value = value.replaceAll("\\s+","");
        // Doit avoir une valeur
        if (value.isEmpty()) {
            throw new LdapException(String.format("La valeur de '%s' ne peut pas être vide", attributeName));
        }
        // En MAJUSCULE ou miniscule ou pas de changement
        switch (toLower) {
            case -1: // toUpper
                value = value.toUpperCase();
                break;
            case 1: // toLower
                value = value.toLowerCase();
                break;
            default:
                // do nothing
                break;
        }
        return value;
    }
}
