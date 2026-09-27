// Predefined 20 authorized user accounts as specified in PROJECT_REQUIREMENTS.txt

export interface UserAccount {
  id: number;
  username: string;
  displayName: string;
  password: string; // Stored offline inside app only
}

export const AUTHORIZED_USERS: UserAccount[] = [
  { id: 1, username: 'Sami', displayName: 'Sami', password: '12345678' },
  { id: 2, username: 'Hani Alqadasi', displayName: 'Hani Alqadasi', password: 'Hani33334' },
  { id: 3, username: 'Eslam Alqadasi', displayName: 'Eslam Alqadasi', password: '55555555' },
  { id: 4, username: 'Omar Ali', displayName: 'Omar Ali', password: 'Omar1234' },
  { id: 5, username: 'Ahmed Saleh', displayName: 'Ahmed Saleh', password: 'Ahmed123' },
  { id: 6, username: 'Mohammed Ali', displayName: 'Mohammed Ali', password: 'Mo2025' },
  { id: 7, username: 'Khaled Nasser', displayName: 'Khaled Nasser', password: 'Khaled22' },
  { id: 8, username: 'Yasser Ahmed', displayName: 'Yasser Ahmed', password: 'Yasser11' },
  { id: 9, username: 'Ali Hassan', displayName: 'Ali Hassan', password: 'Ali2025' },
  { id: 10, username: 'Abdullah Sami', displayName: 'Abdullah Sami', password: 'Abd12345' },
  { id: 11, username: 'Faisal Omar', displayName: 'Faisal Omar', password: 'Faisal88' },
  { id: 12, username: 'Mahmoud Adel', displayName: 'Mahmoud Adel', password: 'Mahmoud7' },
  { id: 13, username: 'Tareq Salem', displayName: 'Tareq Salem', password: 'Tareq123' },
  { id: 14, username: 'Zaid Ahmed', displayName: 'Zaid Ahmed', password: 'Zaid2025' },
  { id: 15, username: 'Noor Ali', displayName: 'Noor Ali', password: 'Noor1234' },
  { id: 16, username: 'Amjad Sami', displayName: 'Amjad Sami', password: 'Amjad555' },
  { id: 17, username: 'Saleh Omar', displayName: 'Saleh Omar', password: 'Saleh2025' },
  { id: 18, username: 'Nasser Ali', displayName: 'Nasser Ali', password: 'Nasser99' },
  { id: 19, username: 'Tariq90', displayName: 'Tariq90', password: 'Tariq4141' },
  { id: 20, username: 'Yazan Sami', displayName: 'Yazan Sami', password: 'Yazan2025' },
];

const SESSION_STORAGE_KEY = 'sami_auth_user_session';

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  loginTimestamp: number;
}

/**
 * Validates login credentials against the offline list of 20 accounts.
 */
export function authenticateUser(usernameInput: string, passwordInput: string): { success: boolean; user?: CurrentUser; error?: string } {
  const trimmedUser = usernameInput.trim();
  const trimmedPass = passwordInput.trim();

  if (!trimmedUser || !trimmedPass) {
    return {
      success: false,
      error: 'اسم المستخدم أو كلمة المرور غير صحيحة.',
    };
  }

  // Find user by username (case-insensitive for username convenience, exact password)
  const matched = AUTHORIZED_USERS.find(
    (u) => u.username.toLowerCase() === trimmedUser.toLowerCase() && u.password === trimmedPass
  );

  if (!matched) {
    return {
      success: false,
      error: 'اسم المستخدم أو كلمة المرور غير صحيحة.',
    };
  }

  const currentUser: CurrentUser = {
    id: matched.id,
    username: matched.username,
    displayName: matched.displayName,
    loginTimestamp: Date.now(),
  };

  try {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(currentUser));
  } catch (err) {
    console.error('Failed to save session locally', err);
  }

  return { success: true, user: currentUser };
}

/**
 * Gets currently logged in user from local storage.
 */
export function getSavedUserSession(): CurrentUser | null {
  try {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CurrentUser;
    if (parsed && parsed.username) {
      return parsed;
    }
  } catch (err) {
    console.error('Failed to read session', err);
  }
  return null;
}

/**
 * Clears current session on user logout.
 */
export function logoutUser(): void {
  try {
    localStorage.removeItem(SESSION_STORAGE_KEY);
  } catch (err) {
    console.error('Failed to clear session', err);
  }
}
