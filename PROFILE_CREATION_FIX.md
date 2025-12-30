# Profile Creation Fix - Account Registration Issue

## Problem
Users were experiencing "Profile not found" errors after creating accounts because the user profile wasn't being created in the `public.users` table when auth users were created in `auth.users`.

## Root Cause
The account creation process had a gap between:
1. Creating the auth user in `auth.users` (via Supabase Auth)
2. Creating the corresponding profile in `public.users` table

This led to orphaned auth users without profiles, causing the app to fail when trying to load user profiles.

## Solution Implemented

### 1. Database Trigger (Primary Fix)
Created an automatic database trigger that creates user profiles whenever auth users are created:

```sql
-- Function to automatically create user profile when auth user is created
CREATE OR REPLACE FUNCTION create_profile_on_auth_user()
RETURNS TRIGGER AS $$
BEGIN
  -- Insert into public.users table with the auth user's data
  INSERT INTO public.users (
    uid, email, created_at, updated_at, join_date,
    account_premium, verify, banned, followers_count,
    following_count, posts_count, user_level_xp, status
  ) VALUES (
    NEW.id, NEW.email, NOW(), NOW(), NOW(),
    false, false, false, 0, 0, 0, 500, 'offline'
  );
  
  -- Create user settings and presence records
  INSERT INTO public.user_settings (user_id) VALUES (NEW.id) ON CONFLICT DO NOTHING;
  INSERT INTO public.user_presence (user_id) VALUES (NEW.id) ON CONFLICT DO NOTHING;
  
  RETURN NEW;
EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING 'Failed to create user profile for %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger on auth.users table
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION create_profile_on_auth_user();
```

### 2. Updated AuthRepository
Modified the `signUpWithProfile` method to rely on the database trigger:

```kotlin
suspend fun signUpWithProfile(email: String, password: String, username: String): Result<String> {
    // Create auth user - database trigger will automatically create profile
    val user = client.auth.signUpWith(Email) {
        this.email = email
        this.password = password
    }
    
    val userId = user?.id ?: throw Exception("Failed to get user ID from signup")
    
    // Update username if provided (trigger creates profile with default username)
    if (username.isNotBlank()) {
        kotlinx.coroutines.delay(100) // Small delay to ensure trigger completes
        client.from("users").update(mapOf("username" to username)) {
            filter { eq("uid", userId) }
        }
    }
    
    return Result.success(userId)
}
```

### 3. Enhanced Profile Recovery
Updated `ensureProfileExists` method to handle orphaned auth users:

```kotlin
private suspend fun ensureProfileExists(userId: String, email: String) {
    val existingProfile = client.from("users")
        .select { filter { eq("uid", userId) } }
        .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
        
    if (existingProfile == null) {
        // Create missing profile with all required fields
        val userMap = mapOf(
            "uid" to userId,
            "username" to email.substringBefore("@"),
            "email" to email,
            "created_at" to java.time.Instant.now().toString(),
            "updated_at" to java.time.Instant.now().toString(),
            "join_date" to java.time.Instant.now().toString(),
            "account_premium" to false,
            "verify" to false,
            "banned" to false,
            "followers_count" to 0,
            "following_count" to 0,
            "posts_count" to 0,
            "user_level_xp" to 500,
            "status" to "offline"
        )
        
        client.from("users").insert(userMap)
        // Also create related records...
    }
}
```

### 4. AuthViewModel Recovery Logic
Added orphaned profile detection and recovery in AuthViewModel:

```kotlin
private fun checkForOrphanedProfile() {
    viewModelScope.launch {
        // Check if current user exists but has no profile
        val currentUserId = authRepository.getCurrentUserId()
        val currentEmail = authRepository.getCurrentUserEmail()
        
        if (currentUserId != null && currentEmail != null) {
            authRepository.ensureProfileExistsPublic(currentUserId, currentEmail)
        }
        
        // Handle any pending profile data from previous failed attempts
        // ... recovery logic
    }
}
```

## Benefits

1. **Automatic Profile Creation**: Database trigger ensures profiles are always created when auth users are created
2. **Fault Tolerance**: Exception handling prevents auth user creation from failing if profile creation has issues
3. **Recovery Mechanism**: Multiple fallback systems to handle orphaned auth users
4. **Consistency**: All new users will have complete profiles with required related records

## Testing

The fix has been tested and verified:
- Database trigger is properly installed and functional
- Existing orphaned user (157efe5c-58ca-4bbb-8fb6-7434d80d0bda) now has a complete profile
- Related records (user_settings, user_presence) are created automatically

## Future Registrations

All new user registrations will now:
1. Create auth user in `auth.users`
2. Automatically trigger profile creation in `public.users`
3. Create related records in `user_settings` and `user_presence`
4. Handle any failures gracefully without breaking the registration process

The "Profile not found" error should no longer occur for new users.
