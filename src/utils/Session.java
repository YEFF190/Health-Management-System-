package utils;

import models.User;

/**
 * Session — holds the currently logged-in user.
 * Volatile ensures visibility across the FX thread and ReminderService threads.
 */
public class Session {

    private static volatile User currentUser;

    public static synchronized void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser()                        { return currentUser; }
    public static synchronized void logout()                   { currentUser = null; }
}