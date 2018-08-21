package fr.epsi.montpellier.Ldap;



// https://www.programcreek.com/java-api-examples/?code=wso2/msf4j/msf4j-master/samples/petstore/microservices/security/src/main/java/org/wso2/msf4j/examples/petstore/security/ldap/LDAPUserStoreManager.java#
// http://www.javafaq.nu/java-example-code-409.html

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;

public class LdapManager {
    /** The OU (organizational unit) to add users to */
    private static String BASE_DN = "dc=montpellier,dc=lan";

    /** The OU (organizational unit) to add users to */
    private static String USERS_OU = "ou=Utilisateurs," + BASE_DN;

    /** The OU (organizational unit) to add groups to */
    private static String GROUPS_OU =  "ou=Groups," + BASE_DN;

    /** Attribut utilisé pour la classe */
    private static final String ATTRIBUTE_NAME_CLASSE =  "description";
    //private static final String ATTRIBUTE_NAME_CLASSE =  "localityName";

    private static final int PORT = 389;

    private String hostname;
    private DirContext context;



    public LdapManager(String hostname, String username, String password,
                       String baseDN, String usersOU, String groupsOU)  throws NamingException {
        this(hostname, username, password);

        BASE_DN = baseDN;
        USERS_OU = "ou=" + usersOU + "," + BASE_DN;
        GROUPS_OU =  "ou=" + groupsOU + "," + BASE_DN;
    }

    public LdapManager(String hostname, String username, String password)  throws NamingException {
        this.hostname = hostname;
        this.context = getInitialContext("cn=" + username + "," + BASE_DN, password);
    }

    public void close() {
        try {
            context.close();
        } catch (Exception ex) {

        }
    }

    public List<UserLdap> listUsers(String classe) {
        List<UserLdap> liste = new ArrayList<UserLdap>();


        try {
            SearchControls searchCtrls = new SearchControls();
            searchCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String filter = classe == null ?
                    "(objectClass=inetOrgPerson)" :
                    "(&(objectClass=inetOrgPerson)(description=" + classe + "))";
            NamingEnumeration items = context.search(USERS_OU, filter, searchCtrls);

            while (items.hasMoreElements())
            {
                // Each item is a SearchResult object
                SearchResult result = (SearchResult) items.next();

                liste.add(buildUserFromAttributes(result));
            }

        } catch (NamingException e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<UserLdap> listUsers() {
        return listUsers(null);
    }


    public UserLdap getUser(String userName) {
        UserLdap user = null;


        try {
            SearchControls searchCtrls = new SearchControls();
            searchCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String filter = "(&(objectClass=inetOrgPerson)(uid=" + userName + "))";
            NamingEnumeration items = context.search(USERS_OU, filter, searchCtrls);

            if (items.hasMoreElements())
            {
                // Each item is a SearchResult object
                SearchResult result = (SearchResult) items.next();

                user = buildUserFromAttributes(result);
            }

        } catch (NamingException e) {
            e.printStackTrace();
        }

        return user;
    }

    public void addUser(UserLdap user) throws NamingException {
        addUser(user.getLogin(), user.getNom(), user.getPrenom(), user.getMotDePasse(), user.getMail(), user.getClasse());
    }

    public void addUser(String login, String nom, String prenom, String motDePasse,
            String email, String classe) throws NamingException {

        if (nom == null || prenom == null) {
            throw new NamingException("Les attributs de l'utilsateurs sont vides");
        }

        // Create a container set of attributes
        Attributes container = new BasicAttributes();

        // Create the objectclass to add
        Attribute objClasses = new BasicAttribute("objectClass");
        objClasses.add("top");
        objClasses.add("person");
        objClasses.add("organizationalPerson");
        objClasses.add("inetOrgPerson");

        Attribute cn = new BasicAttribute("cn", prenom + " " + nom);
        Attribute givenName = new BasicAttribute("givenName", prenom);
        Attribute sn = new BasicAttribute("sn", nom.toUpperCase());
        Attribute uid = new BasicAttribute("uid", login);
        Attribute mail = new BasicAttribute("mail", email);
        Attribute classeAttr = new BasicAttribute(ATTRIBUTE_NAME_CLASSE, classe.toUpperCase());

        // Add password
        Attribute userPassword = new BasicAttribute("userpassword", motDePasse);

        // Add these to the container
        container.put(objClasses);
        container.put(cn);
        container.put(sn);
        container.put(givenName);
        container.put(uid);
        container.put(mail);
        container.put(userPassword);
        container.put(classeAttr);

        // Create the entry
        context.createSubcontext(getUserDN(login), container);
    }

    public boolean modifyUser(String username, UserLdap userToUpdate) throws NamingException {
        try {
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_CLASSE, userToUpdate.getClasse()));

            context.modifyAttributes(getUserDN(username), mods);
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public boolean deleteUser(String username) throws NamingException {
        try {
            context.destroySubcontext(getUserDN(username));
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public UserLdap authenticateUser(String username, String password) {

        if (isValidUser(username, password)) {
            return getUser(username);
        }

        return null;
    }

    public boolean isValidUser(String username, String password) {

        try {
            DirContext context = getInitialContext(getUserDN(username), password);
            return true;
        } catch (javax.naming.NameNotFoundException e) {
            //
        } catch (NamingException e) {
            // Any other error indicates couldn't log user in
        }
        finally {
            try {
                context.close();
            } catch (Exception ex) {

            }
        }

        return false;
    }

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


    private String getUserDN(String username) {
        return "uid=" + username + "," + USERS_OU;
    }

    private UserLdap buildUserFromAttributes(SearchResult result) throws NamingException {

        // Get the node's attributes
        Attributes attrs = result.getAttributes();

        return new UserLdap(attrs.get("uid").get(0).toString(),
                attrs.get("sn").get(0).toString(),
                attrs.get("givenName").get(0).toString(),
                attrs.get(ATTRIBUTE_NAME_CLASSE).get(0).toString(),
                attrs.get("mail").get(0).toString());
    }
}
