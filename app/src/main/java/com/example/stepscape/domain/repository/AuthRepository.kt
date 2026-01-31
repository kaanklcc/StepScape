package com.example.stepscape.domain.repository

import com.google.firebase.auth.FirebaseUser


interface AuthRepository {
    
    //kullanıcının güncel bir kullanıcı mı değil mi onu kontrol eder.
    fun getCurrentUser(): FirebaseUser?
    
   //kullanıcı giriş yapmış mı onu kontrol eder.
    fun isUserSignedIn(): Boolean
    
   //google ile giriş yapmak icin
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    
    //çıkış yapmak için.
    suspend fun signOut()
}
