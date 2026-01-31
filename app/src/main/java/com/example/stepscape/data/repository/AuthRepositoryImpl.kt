package com.example.stepscape.data.repository

import android.util.Log
import com.example.stepscape.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase ile giriş yapmak için repository.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override fun isUserSignedIn(): Boolean {
        val isSignedIn = firebaseAuth.currentUser != null
        Log.d(TAG, "isUserSignedIn: $isSignedIn")
        return isSignedIn
    }

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Signing in with Google...")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Sign in successful: ${user.email}")
                Result.success(user)
            } else {
                Log.e(TAG, "Sign in failed: user is null")
                Result.failure(Exception("User is null after sign in"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        Log.d(TAG, "Signing out...")
        firebaseAuth.signOut()
    }
}
