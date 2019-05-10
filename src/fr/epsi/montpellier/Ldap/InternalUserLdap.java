package fr.epsi.montpellier.Ldap;

public class InternalUserLdap extends UserLdap {

    private String motDePasseCopie;

    public InternalUserLdap(UserLdap user) {
        super(user.getLogin(), user.getNom(), user.getPrenom(), null, user.getClasse(), user.getMail(), user.getRole());

        this.setUserDN(user.getUserDN());
    }

    public String getMotDePasseCopie() {
        return motDePasseCopie;
    }

    public void setMotDePasseCopie(String motDePasseCopie) {
        this.motDePasseCopie = motDePasseCopie;
    }
}
