package com.example.stepscape.domain.usecase

import com.example.stepscape.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Google ile çıkış yapmak için usecase.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.signOut()
    }
}
