package com.example.stepscape.domain.usecase

import com.example.stepscape.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * kullanıcı daha önce giriş yapmış mı? kontrolü yapar.
 */
class CheckAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean {
        return authRepository.isUserSignedIn()
    }
}
