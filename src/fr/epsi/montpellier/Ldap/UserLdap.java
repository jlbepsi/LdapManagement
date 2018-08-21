package fr.epsi.montpellier.Ldap;

public class UserLdap {
    private String login;
    private String nom;
    private String prenom;
    private String motDePasse;
    private String classe;
    private String mail;
    private String role;

    public UserLdap(String login, String nom, String prenom, String classe, String mail) {
        this(login, nom, prenom, classe, mail,"ROLE_USER");
    }

    public UserLdap(String login, String nom, String prenom, String classe, String mail, String role) {

        this.login = login;
        this.nom = nom;
        this.prenom = prenom;
        this.mail = mail;
        this.classe = classe;
        this.role = role;
    }

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }

    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getNomComplet() { return this.nom + " " + this.prenom; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getClasse() {
        return classe;
    }
    public void setClasse(String classe) {
        this.classe = classe;
    }

    public String getRole() { return role; }
    public void setRole(String role) {
        this.role = role;
    }
}
