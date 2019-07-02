package fr.epsi.montpellier.Ldap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class SHAEncryption {

    public static String encryptLdapPassword(String clearPassword) throws NoSuchAlgorithmException {

        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[4];
        secureRandom.nextBytes(salt);

        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(clearPassword.getBytes());
        crypt.update(salt);
        byte[] hash = crypt.digest();

        byte[] hashPlusSalt = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, hashPlusSalt, 0, hash.length);
        System.arraycopy(salt, 0, hashPlusSalt, hash.length, salt.length);

        return "{SSHA}" +
                Base64.getEncoder().encodeToString(hashPlusSalt);
    }
}
