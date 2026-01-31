package com.example.stepscape.domain.usecase

import com.example.stepscape.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

/**
 * Google ile giriş yapmak için usecase.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<FirebaseUser> {
        return authRepository.signInWithGoogle(idToken)
    }
}
