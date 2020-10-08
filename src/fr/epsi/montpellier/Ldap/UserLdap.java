package fr.epsi.montpellier.Ldap;


import com.fasterxml.jackson.annotation.JsonIgnore;


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

    public UserLdap(String login, String nom, String prenom, String motDePasse, String classe, String mail, String role) {
        this.login = login;
        this.nom = nom;
        this.prenom = prenom;
        this.motDePasse = motDePasse;
        this.mail = mail;
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
    public void setLogin(String login) {
        this.login = login.toLowerCase();
    }

    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom.toUpperCase();
    }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getNomComplet() { return this.nom + " " + this.prenom; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail.toLowerCase(); }

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
    public void setRole(String role) {
        // Pas d'espace et MAJUSCULE
        role = role.replaceAll("\\s+","").toUpperCase();
        // Pas de virgule à la fin
        if (role.endsWith(",")) {
            role = role.substring(0, role.length() -1);
        }
        // Le rôle ROLE_USER doit toujours se trouver dans le rôle
        if (! role.contains("ROLE_USER")) {
            role += ",ROLE_USER";
        }
        this.role = role.toUpperCase();
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
        if (btsParcours == null || btsParcours.length() == 0) {
            // Le texte ne doit pas être vide (contrainte OpenLDAP)
            btsParcours = "0";
        }
        this.btsParcours = btsParcours.toUpperCase();
    }

    public String getBtsNumero() {
        return btsNumero;
    }
    public void setBtsNumero(String btsNumero) {
        if (btsNumero == null || btsNumero.length() == 0) {
            // Le numéro ne doit pas être vide (contrainte OpenLDAP)
            btsNumero = "0";
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
                login, isActive?"Actif":"Inactif", nom, prenom, genre, classe, groupe, mail, role,
                bts ? "BTS=" + btsParcours + "(" +btsNumero +"), ":"", classePrecedente);
    }
}
