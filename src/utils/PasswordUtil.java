package utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtil — SHA-256 + salt password hashing.
 * Format stored in DB:  HASH:BASE64(salt):BASE64(SHA256(salt+password))
 * The "HASH:" prefix makes detection 100% reliable.
 */
public class PasswordUtil {

    private static final int SALT_BYTES = 16;
    // Prefix added to every hashed password so we can detect it unambiguously
    private static final String PREFIX = "HASH:";

    /** Hash a plain-text password. Returns a prefixed string safe to store in DB. */
    public static String hash(String plainPassword) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);
            byte[] hash = sha256(salt, plainPassword);
            return PREFIX
                 + Base64.getEncoder().encodeToString(salt) + ":"
                 + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /** Return true if plainPassword matches the stored hash. */
    public static boolean verify(String plainPassword, String stored) {
        try {
            if (!isHashed(stored)) return false;
            // Strip the prefix, then split into salt:hash
            String body = stored.substring(PREFIX.length());
            String[] parts = body.split(":", 2);
            if (parts.length != 2) return false;
            byte[] salt     = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] actual   = sha256(salt, plainPassword);
            if (actual.length != expected.length) return false;
            // Constant-time compare
            int diff = 0;
            for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reliable detection: only strings starting with "HASH:" are hashed.
     * Everything else is treated as legacy plain-text.
     */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    private static byte[] sha256(byte[] salt, String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        md.update(password.getBytes("UTF-8"));
        return md.digest();
    }
}
