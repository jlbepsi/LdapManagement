package fr.epsi.montpellier.Ldap;

public class InternalUserLdap extends UserLdap {

    private String motDePasseCopie;

    public InternalUserLdap(UserLdap user) {
        super(user.getLogin(), user.getNom(), user.getPrenom(), null, user.getClasse(), user.getMail(), user.getRole());

        // Copie des autres attributs
        this.setGroupe(user.getGroupe());
        this.setBts(user.isBts());
        this.setBtsNumero(user.getBtsNumero());
        this.setBtsParcours(user.getBtsParcours());
        this.setActive(user.isActive());
        this.setUserDN(user.getUserDN());
    }

    public String getMotDePasseCopie() {
        return motDePasseCopie;
    }

    public void setMotDePasseCopie(String motDePasseCopie) {
        this.motDePasseCopie = motDePasseCopie;
    }
}
