package fr.epsi.montpellier.Ldap;



// https://www.programcreek.com/java-api-examples/?code=wso2/msf4j/msf4j-master/samples/petstore/microservices/security/src/main/java/org/wso2/msf4j/examples/petstore/security/ldap/LDAPUserStoreManager.java#
// http://www.javafaq.nu/java-example-code-409.html

import fr.epsi.montpellier.Utils.FileUtils;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static fr.epsi.montpellier.Ldap.SHAEncryption.encryptLdapPassword;

public class LdapManager {
    /** The OU (organizational unit) to add users to */
    private static String BASE_DN = "dc=montpellier,dc=lan";

    /** The OU (organizational unit) to add users to */
    private static String USERS_OU = "ou=Utilisateurs,ou=Pedago," + BASE_DN;

    /** The OU (organizational unit) to add groups to */
    private static String GROUPS_OU =  "ou=Groupes," + BASE_DN;

    private static String GROUPE_ETUDIANTS = "Etudiants";
    private static String GROUPE_INTERNES = "Internes";

    /** Attributs utilisés pour la classe */
    private static final String ATTRIBUTE_NAME_CLASSE =  "l";
    private static final String ATTRIBUTE_NAME_ROLE =  "employeeType";
    private static final String ATTRIBUTE_NAME_BTS =  "businessCategory";
    private static final String ATTRIBUTE_NAME_BTS_PARCOURS =  "description";
    private static final String ATTRIBUTE_NAME_BTS_NUMERO =  "departmentNumber";
    private static final String ATTRIBUTE_PASSWORD_COPY =  "street";
    private static final String ATTRIBUTE_ACTIVEUSER =  "roomNumber";

    private static final int PORT = 389;

    private String hostname;
    private DirContext ldapContext;


    private String usersLdapDirectory;

    public LdapManager(String hostname, String username, String password,
                       String baseDN, String usersOU, String groupsOU)  throws NamingException {
        BASE_DN = baseDN;
        USERS_OU = usersOU + "," + BASE_DN;
        GROUPS_OU =  groupsOU + "," + BASE_DN;

        this.hostname = hostname;
        this.ldapContext = getInitialContext("cn=" + username + "," + BASE_DN, password);
    }

    public LdapManager(String hostname, String username, String password)  throws NamingException {
        this.hostname = hostname;
        this.ldapContext = getInitialContext("cn=" + username + "," + BASE_DN, password);
    }

    public String getUsersLdapDirectory() {
        return usersLdapDirectory;
    }

    public void setUsersLdapDirectory(String usersLdapDirectory) {
        this.usersLdapDirectory = usersLdapDirectory;
    }

    public void close() {
        try {
            ldapContext.close();
        } catch (Exception ex) {

        }
    }

    public List<UserLdap> listUsers(String classe) {
        List<UserLdap> liste = new ArrayList<UserLdap>();


        try {
            SearchControls searchCtrls = new SearchControls();
            searchCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchCtrls.setReturningAttributes(new String[] {"*", "memberOf"});

            String filter = classe == null ?
                    "(objectClass=inetOrgPerson)" :
                    "(&(objectClass=inetOrgPerson)("+ ATTRIBUTE_NAME_CLASSE +"=" + classe + "))";
            NamingEnumeration items = ldapContext.search(USERS_OU, filter, searchCtrls);

            while (items.hasMoreElements())
            {
                // Each item is a SearchResult object
                SearchResult result = (SearchResult) items.next();

                liste.add(buildUserFromAttributes(result.getNameInNamespace(), result.getAttributes()));
            }

        } catch (NamingException e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<UserLdap> listUsers() {
        return listUsers(null);
    }


    public UserLdap getUser(String login) {
        return getUser(login, false);
    }
    public UserLdap getUserFromDN(String dn) {
        UserLdap user = null;

        try {
            Attributes attributes = ldapContext.getAttributes(dn, new String[] {"*", "memberOf"});

            if (attributes != null) {
                user = buildUserFromAttributes(dn, attributes);
            }

        } catch (NamingException e) {
            e.printStackTrace();
        }

        return user;
    }

    private UserLdap getUser(String login, boolean userInternalUser) {
        UserLdap user = null;


        try {
            SearchControls searchCtrls = new SearchControls();
            searchCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchCtrls.setReturningAttributes(new String[] {"*", "memberOf"});

            String filter = "(&(objectClass=inetOrgPerson)(uid=" + login + "))";
            NamingEnumeration items = ldapContext.search(USERS_OU, filter, searchCtrls);

            if (items.hasMoreElements())
            {
                // Each item is a SearchResult object
                SearchResult result = (SearchResult) items.next();

                user = userInternalUser ?
                        buildInternalUserFromAttributes(result.getNameInNamespace(), result.getAttributes()) :
                        buildUserFromAttributes(result.getNameInNamespace(), result.getAttributes());
            }

        } catch (NamingException e) {
            e.printStackTrace();
        }

        return user;
    }

    public void addUser(String login, String nom, String prenom, String motDePasse,
                        String email, String classe, String role) throws NamingException {
        addUser(new UserLdap(login, nom, prenom, motDePasse, classe, email, role));
    }

    public void addUser(UserLdap user) throws NamingException {

        if (user==null) {
            throw new NamingException("L'utilisateur est non renseigné");
        }
        if (user.getNom() == null || user.getPrenom() == null) {
            throw new NamingException("Les attributs de l'utilisateurs sont vides");
        }

        // Create a container set of attributes
        Attributes container = new BasicAttributes();

        // Create the objectclass to add
        Attribute objClasses = new BasicAttribute("objectClass");
        objClasses.add("top");
        objClasses.add("person");
        objClasses.add("organizationalPerson");
        objClasses.add("inetOrgPerson");

        Attribute cn = new BasicAttribute("cn", user.getPrenom() + " " + user.getNom());
        Attribute givenName = new BasicAttribute("givenName", user.getPrenom());
        Attribute sn = new BasicAttribute("sn", user.getNom());
        Attribute uid = new BasicAttribute("uid", user.getLogin());
        Attribute mail = new BasicAttribute("mail", user.getMail());
        Attribute classeAttr = new BasicAttribute(ATTRIBUTE_NAME_CLASSE, user.getClasse());
        Attribute roleAttr = new BasicAttribute(ATTRIBUTE_NAME_ROLE, user.getRole());

        // Add password
        String pwdCrypt;
        try {
            pwdCrypt = encryptLdapPassword(user.getMotDePasse());
        } catch (NoSuchAlgorithmException e) {
            throw new NamingException("Bad password encryption");
        }
        Attribute userPassword = new BasicAttribute("userpassword", pwdCrypt);

        // Add these to the container
        container.put(objClasses);
        container.put(cn);
        container.put(sn);
        container.put(givenName);
        container.put(uid);
        container.put(mail);
        container.put(userPassword);
        // On ajoute l'attribut qui est la copie du mdp
        container.put(new BasicAttribute(ATTRIBUTE_PASSWORD_COPY, pwdCrypt));
        container.put(classeAttr);
        container.put(roleAttr);
        // On ajoute l'attribut identifiant si l'utilisateur est actif (1) ou non (0)
        container.put(new BasicAttribute(ATTRIBUTE_ACTIVEUSER, "1"));


        // Fixe le BTS
        if (user.isBts()) {
            Attribute btsAttr = new BasicAttribute(ATTRIBUTE_NAME_BTS, "BTS");
            Attribute btsParcoursAttr = new BasicAttribute(ATTRIBUTE_NAME_BTS_PARCOURS, user.getBtsParcours());
            Attribute btsNumeroAttr = new BasicAttribute(ATTRIBUTE_NAME_BTS_NUMERO, user.getBtsNumero());

            container.put(btsAttr);
            container.put(btsParcoursAttr);
            container.put(btsNumeroAttr);
        }

        // Create the entry
        String userDN = "uid=" + user.getLogin() + "," + USERS_OU;
        ldapContext.createSubcontext(userDN, container);


        // Fixe le groupe
        // Soit le groupe INTERNES si la classe commence par INT_, soit le groupe ETUDIANTS SINON
        modifyGroupUser(
                userDN,
                user.getClasse().startsWith("INT_") ? LdapManager.GROUPE_INTERNES : LdapManager.GROUPE_ETUDIANTS,
                true
        );

        // Création du répertoire home de l'utilisateur
        createHomeDirectory(user);
    }

    public boolean updateUser(String username, UserLdap userToUpdate) throws NamingException {

        if (userToUpdate == null)
            return false;

        // Recherche de l'utilisateur à modifier
        UserLdap userInitial = getUser(username);
        if (userInitial == null)
            return false;

        try {
            ModificationItem[] mods = new ModificationItem[7];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("sn", userToUpdate.getNom()));
            mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("givenName", userToUpdate.getPrenom()));
            mods[2] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_CLASSE, userToUpdate.getClasse()));
            mods[3] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_ROLE, userToUpdate.getRole()));
            // BTS
            if (userToUpdate.isBts()) {
                mods[4] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_BTS, "BTS"));
                mods[5] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_BTS_PARCOURS, userToUpdate.getBtsParcours()));
                mods[6] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_BTS_NUMERO, userToUpdate.getBtsNumero()));
            } else {
                mods[4] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_BTS, null));
                mods[5] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_BTS_PARCOURS, null));
                mods[6] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_BTS_NUMERO, null));

            }

            ldapContext.modifyAttributes(userInitial.getUserDN(), mods);

            // Sil l'utilisateur a changé de groupe, on le met à jour
            // Soit le groupe INTERNES si la classe commence par INT_, soit le groupe ETUDIANTS SINON
            String oldGroup = userInitial.getClasse().startsWith("INT_") ? LdapManager.GROUPE_INTERNES : LdapManager.GROUPE_ETUDIANTS;
            String newGroup = userToUpdate.getClasse().startsWith("INT_") ? LdapManager.GROUPE_INTERNES : LdapManager.GROUPE_ETUDIANTS;

            if (! oldGroup.equals(newGroup)) {
                // Suppression de l'utilisateur de l'ancien groupe
                modifyGroupUser(userToUpdate.getUserDN(), oldGroup, false);
                // Ajout de l'utilisateur au nouveau groupe
                modifyGroupUser(userToUpdate.getUserDN(), newGroup, true);
            }

            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public boolean updateUserPassword(String login, String password) throws NamingException {

        UserLdap userInitial = getUser(login);
        if (userInitial == null)
            return false;

        userInitial.setMotDePasse(password);
        return updateUserPassword(userInitial);
    }

    public boolean updateUserPassword(UserLdap userToUpdate) throws NamingException {
        if (userToUpdate == null || userToUpdate.getUserDN() == null)
            return false;

        try {
            ModificationItem[] mods = new ModificationItem[1];

            String userPasswordEncrypt = encryptLdapPassword(userToUpdate.getMotDePasse());
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userpassword", userPasswordEncrypt));
            /*
            On place le mot de passe actuel dans l'attribut de copie du mdp
            pour pouvoir l'utiliser dans les méthodes activateUser et deactivateUser
             */
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_PASSWORD_COPY, userPasswordEncrypt));

            ldapContext.modifyAttributes(userToUpdate.getUserDN(), mods);
            return true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
            e.printStackTrace();
        }
        return false;
    }

    public boolean deactivateUser(String username) throws NamingException {

        UserLdap userToUpdate = getUser(username, true);
        if (userToUpdate == null)
            return false;

        try {
            // Le type du user est InternalUserLdap
            InternalUserLdap internalUserLdap = (InternalUserLdap) userToUpdate;

            ModificationItem[] mods = new ModificationItem[3];

            /*
             On lui attribue un mot de passe aléatoire pour que l'utilisateur ne puisse plus se connecter
            */
            UUID uuid = UUID.randomUUID();
            Attribute userPassword = null;
            try {
                userPassword = new BasicAttribute("userpassword", encryptLdapPassword(uuid.toString()));
            } catch (NoSuchAlgorithmException e) {
                throw new NamingException("Bad password encryption");
            }
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, userPassword);

            // Par défaut la classe est 'NA' pour les utilisateurs désactivés
            mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_CLASSE, "NA"));

            // On modifie l'attribut identifiant si l'utilisateur est actif (1) ou non (0)
            mods[2] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_ACTIVEUSER, "0"));

            ldapContext.modifyAttributes(userToUpdate.getUserDN(), mods);
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public boolean activateUser(String username) throws NamingException {

        UserLdap userToUpdate = getUser(username, true);
        if (userToUpdate == null)
            return false;

        try {
            // Le type du user est InternalUserLdap
            InternalUserLdap internalUserLdap = (InternalUserLdap) userToUpdate;

            /*
             On récupère le mot de passe initial depuis l'attribut de copie du mdp
             */
            String motDePasseCopie = internalUserLdap.getMotDePasseCopie();
            /*
             On replace le mot de passe avec la copie de celui-ci
             */
            ModificationItem[] mods = new ModificationItem[2];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userpassword", motDePasseCopie));

            // On modifie l'attribut identifiant si l'utilisateur est actif (1) ou non (0)
            mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_ACTIVEUSER, "1"));

            ldapContext.modifyAttributes(userToUpdate.getUserDN(), mods);
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public boolean deleteUser(String username) throws NamingException {

        UserLdap userInitial = getUser(username);
        if (userInitial == null)
            return false;

        try {
            ldapContext.destroySubcontext(userInitial.getUserDN());

            // Suppression du répertoire home de l'utilisateur
            deleteHomeDirectory(userInitial);
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }


    public boolean setUsersToNA() throws NamingException {

        // IMPORTANT: Seuls les utilisateurs du groupe Etudiants sont concernés !

        // Obtention de tous les utilisateurs du groupe "Etudiants
        List<UserLdap> users = listUsersOfGroups(GROUPE_ETUDIANTS);
        // Bascule dans la classe NA
        for (UserLdap user : users) {

            try {
                ModificationItem[] mods = new ModificationItem[1];
                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_CLASSE, "NA"));
                ldapContext.modifyAttributes(user.getUserDN(), mods);
            } catch (NameNotFoundException e) {
                // If the user is not found, ignore the error
            }
        }

        return true;
    }

    public UserLdap authenticateUser(String username, String password) {

        UserLdap user = getUser(username);
        if (user != null && user.isActive()) {

            Context userContext = null;
            try {
                userContext = getInitialContext(user.getUserDN(), password);
                return user;
            } catch (javax.naming.NameNotFoundException e) {
                System.out.println(e.getMessage());
                //
            } catch (NamingException e) {
                // Any other error indicates couldn't log user in
                System.out.println(e.getMessage());
            }
            finally {
                try {
                    if (userContext != null)
                        userContext.close();
                } catch (Exception ex) {
                }
            }
        }

        return null;
    }

    /*
     *********************************
     *
     * Méthodes pour la création / suppression de répertoire des utilisateurs
     */

    private void createHomeDirectory(UserLdap user) {
        if (usersLdapDirectory != null && usersLdapDirectory.length() > 0) {
            String directoryName = this.usersLdapDirectory + "/" + user.getLogin();
            Path path = Paths.get(directoryName);
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteHomeDirectory(UserLdap user) {

            if (usersLdapDirectory != null && usersLdapDirectory.length() > 0) {
                String directoryName = this.usersLdapDirectory + "/" + user.getLogin();
                File directory = new File(directoryName);

                FileUtils.deleteDirectory(directory);
            }
    }

    /**/


    /*
     *********************************
     *
     * Méthodes pour la gestion des groupes
     */

    public List<UserLdap> listUsersOfGroups(String group) {
        List<UserLdap> liste = new ArrayList<UserLdap>();

        String[] searchAttributes = new String[1];
        searchAttributes[0] = "uniqueMember";

        try {
            Attributes attributes = ldapContext.getAttributes("cn=" + group + "," + GROUPS_OU, searchAttributes);
            if (attributes != null) {
                Attribute memberAtts = attributes.get("uniqueMember");
                if (memberAtts != null) {
                    NamingEnumeration items = memberAtts.getAll();
                    while (items.hasMoreElements())
                    {
                        String userDN = (String) items.next();
                        UserLdap user = getUserFromDN(userDN);

                        liste.add(user);
                    }
                }
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }

        return liste;
    }


    private boolean modifyGroupUser(String userDN, String group, boolean addToGroup) {

        if (userDN == null || group == null) {
            return false;
        }

        try {
            ModificationItem[] mods = new ModificationItem[1];
            String groupDN = "cn=" + group + "," + GROUPS_OU;

            Attribute mod = new BasicAttribute("uniqueMember", userDN);
            mods[0] = new ModificationItem(addToGroup ? DirContext.ADD_ATTRIBUTE :  DirContext.REMOVE_ATTRIBUTE, mod);
            ldapContext.modifyAttributes(groupDN, mods);

            return true;
        } catch (NamingException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    private static List<String> getMemberOfFromAttributes(Attribute attribute) {
        List<String> memberOf = new ArrayList<String>();

        try {
            NamingEnumeration items = attribute.getAll();
            while (items.hasMoreElements())
            {
                memberOf.add((String) items.next());
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }

        return memberOf;
    }

    /**/


    /**
     *
     * @param userDn Login de l'utilisateur
     * @param password Mot de passe de l'utilisateur
     * @return DirContext
     * @throws NamingException Erreur d'accès au LDAP
     */
    private DirContext getInitialContext(String userDn, String password)
            throws NamingException {

        String providerURL = "ldap://" + this.hostname + ":" + PORT;

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        props.put(Context.PROVIDER_URL, providerURL);

        if ((userDn != null) && (!userDn.equals(""))) {
            props.put(Context.SECURITY_AUTHENTICATION, "simple");
            props.put(Context.SECURITY_PRINCIPAL, userDn);
            props.put(Context.SECURITY_CREDENTIALS, (password == null) ? "" : password);
        }

        return new InitialDirContext(props);
    }

    private UserLdap buildUserFromAttributes(String dnUser, Attributes attrs) throws NamingException {
        if (attrs == null)
            return null;

        UserLdap user = new UserLdap(attrs.get("uid").get(0).toString(),
                attrs.get("sn").get(0).toString(),
                attrs.get("givenName").get(0).toString(),
                null,
                attrs.get(ATTRIBUTE_NAME_CLASSE).get(0).toString(),
                attrs.get("mail").get(0).toString(),
                getAttributeValue(attrs.get(ATTRIBUTE_NAME_ROLE), "ROLE_USER"));
        // Utilisateur Actif ou non
        user.setActive(getAttributeValue(attrs.get(ATTRIBUTE_ACTIVEUSER), "0").equals("1"));

        // Groupe de l'utilisateur
        Attribute groups = attrs.get("memberof");
        if (groups != null) {
            user.setGroupe(getMemberOfFromAttributes(groups));
        }

        // Fixe le BTS
        String value = getAttributeValue(attrs.get(ATTRIBUTE_NAME_BTS));
        if (value != null && value.equalsIgnoreCase("BTS")) {
            user.setBts(true);

            user.setBtsParcours(getAttributeValue(attrs.get(ATTRIBUTE_NAME_BTS_PARCOURS)));
            user.setBtsNumero(getAttributeValue(attrs.get(ATTRIBUTE_NAME_BTS_NUMERO)));
        }


        // Fixe le DN de l'utilisateur
        user.setUserDN(dnUser);

        return user;
    }
    private UserLdap buildInternalUserFromAttributes(String dnUser, Attributes attrs) throws NamingException {
        // Obtention de l'utilisateur
        UserLdap user = buildUserFromAttributes(dnUser, attrs);

        // Ajout de la copie du mdp
        String passwordCopy = getAttributeValue(attrs.get(ATTRIBUTE_PASSWORD_COPY), null);

        InternalUserLdap internalUserLdap = new InternalUserLdap(user);
        internalUserLdap.setMotDePasseCopie(passwordCopy);

        return internalUserLdap;
    }

    private static String getAttributeValue(Attribute attribute, String defaultValue) throws NamingException {

        Object attributeValue = (attribute == null ? null : attribute.get(0));
        return attributeValue == null ? defaultValue : attributeValue.toString();
    }
    private static String getAttributeValue(Attribute attribute) throws NamingException {
        return getAttributeValue(attribute, null);
    }
}
