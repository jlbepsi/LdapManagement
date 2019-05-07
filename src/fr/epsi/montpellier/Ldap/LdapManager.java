package fr.epsi.montpellier.Ldap;



// https://www.programcreek.com/java-api-examples/?code=wso2/msf4j/msf4j-master/samples/petstore/microservices/security/src/main/java/org/wso2/msf4j/examples/petstore/security/ldap/LDAPUserStoreManager.java#
// http://www.javafaq.nu/java-example-code-409.html

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LdapManager {
    /** The OU (organizational unit) to add users to */
    private static String BASE_DN = "dc=montpellier,dc=lan";

    /** The OU (organizational unit) to add users to */
    private static String USERS_OU = "ou=Utilisateurs,ou=Pedago," + BASE_DN;

    /** The OU (organizational unit) to add groups to */
    private static String GROUPS_OU =  "ou=Groups," + BASE_DN;

    /** Attributs utilisés pour la classe */
    private static final String ATTRIBUTE_NAME_CLASSE =  "l";
    private static final String ATTRIBUTE_NAME_ROLE =  "employeeType";
    private static final String ATTRIBUTE_NAME_BTS =  "businessCategory";
    private static final String ATTRIBUTE_NAME_BTS_PARCOURS =  "description";
    private static final String ATTRIBUTE_NAME_BTS_NUMERO =  "departmentNumber";

    private static final int PORT = 389;

    private String hostname;
    private DirContext ldapContext;



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

            String filter = classe == null ?
                    "(objectClass=inetOrgPerson)" :
                    "(&(objectClass=inetOrgPerson)("+ ATTRIBUTE_NAME_CLASSE +"=" + classe + "))";
            NamingEnumeration items = ldapContext.search(USERS_OU, filter, searchCtrls);

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
            NamingEnumeration items = ldapContext.search(USERS_OU, filter, searchCtrls);

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

    public void addUser(String login, String nom, String prenom, String motDePasse,
                        String email, String classe, String role) throws NamingException {
        addUser(new UserLdap(login, nom, prenom, motDePasse, classe, email, role));
    }

    public void addUser(UserLdap user) throws NamingException {

        if (user==null) {
            throw new NamingException("Les attributs de l'utilisateurs sont vides");
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
        Attribute userPassword = new BasicAttribute("userpassword", user.getMotDePasse());

        // Add these to the container
        container.put(objClasses);
        container.put(cn);
        container.put(sn);
        container.put(givenName);
        container.put(uid);
        container.put(mail);
        container.put(userPassword);
        container.put(classeAttr);
        container.put(roleAttr);

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
    }

    public boolean updateUser(String username, UserLdap userToUpdate) throws NamingException {

        if (userToUpdate.getUserDN() == null) {
            UserLdap userInitial = getUser(username);
            if (userInitial == null)
                return false;

            userToUpdate.setUserDN(userInitial.getUserDN());
        }

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

            ldapContext.modifyAttributes(userToUpdate.getUserDN(), mods);
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public boolean deactivateUser(String username) throws NamingException {

        UserLdap userToUpdate = getUser(username);
        if (userToUpdate == null)
            return false;

        try {
            /*Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.YEAR, -100);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));*/


            ModificationItem[] mods = new ModificationItem[1];
            //mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("pwdAccountLockedTime", simpleDateFormat.format(calendar.getTime())));


            //mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("pwdAccountLockedTime", "000001010000Z"));
            mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(ATTRIBUTE_NAME_CLASSE, "NA"));
            ldapContext.modifyAttributes(userToUpdate.getUserDN(), mods);
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public boolean activateUser(String username) throws NamingException {

        UserLdap userToUpdate = getUser(username);
        if (userToUpdate == null)
            return false;

        try {
            ModificationItem[] mods = new ModificationItem[1];
            //mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("pwdAccountLockedTime"));
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
            return true;
        } catch (NameNotFoundException e) {
            // If the user is not found, ignore the error
        }

        return false;
    }

    public UserLdap authenticateUser(String username, String password) {

        UserLdap user = getUser(username);
        if (user != null) {

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

    private UserLdap buildUserFromAttributes(SearchResult result) throws NamingException {

        // Get the node's attributes
        Attributes attrs = result.getAttributes();

        UserLdap user = new UserLdap(attrs.get("uid").get(0).toString(),
                attrs.get("sn").get(0).toString(),
                attrs.get("givenName").get(0).toString(),
                null,
                attrs.get(ATTRIBUTE_NAME_CLASSE).get(0).toString(),
                attrs.get("mail").get(0).toString(),
                getAttibuteValue(attrs.get(ATTRIBUTE_NAME_ROLE), "ROLE_USER"));

        // Fixe le BTS
        String value = getAttibuteValue(attrs.get(ATTRIBUTE_NAME_BTS));
        if (value != null && value.equalsIgnoreCase("BTS")) {
            user.setBts(true);

            user.setBtsParcours(getAttibuteValue(attrs.get(ATTRIBUTE_NAME_BTS_PARCOURS)));
            user.setBtsNumero(getAttibuteValue(attrs.get(ATTRIBUTE_NAME_BTS_NUMERO)));
        }


        // Fixe le DN de l'utilisateur
        user.setUserDN(result.getNameInNamespace());

        return user;
    }

    private String getAttibuteValue(Attribute attribute, String defaultValue) throws NamingException {

        if (attribute != null) {
            Object attributeValue = attribute.get(0);
            if (attributeValue != null)
                return attributeValue.toString();
        }
        return defaultValue;
    }
    private String getAttibuteValue(Attribute attribute) throws NamingException {
        return getAttibuteValue(attribute, null);
    }
}
