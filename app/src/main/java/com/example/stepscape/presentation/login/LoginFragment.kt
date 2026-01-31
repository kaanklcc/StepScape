package com.example.stepscape.presentation.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stepscape.R
import com.example.stepscape.databinding.FragmentLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@AndroidEntryPoint
class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "LoginFragment"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        credentialManager = CredentialManager.create(requireContext())

        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.btnGoogleSignIn.isEnabled = !state.isLoading

                if (state.error != null) {
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = state.error
                } else {
                    binding.tvError.visibility = View.GONE
                }

                if (state.isSignedIn) {
                    Log.d(TAG, "User signed in, navigating to home...")
                    findNavController().navigate(R.id.action_login_to_home)
                }
            }
        }
    }

    private fun signInWithGoogle() {
        viewModel.setLoading(true)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = requireContext()
                )
                handleSignInResult(result)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential error: ${e.message}")
                viewModel.onSignInError(e.message ?: "Google ile giriş başarısız")
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        Log.d(TAG, "Got Google ID token, signing in with Firebase...")
                        viewModel.onGoogleSignInResult(idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Google ID token parsing failed: ${e.message}")
                        viewModel.onSignInError("Google token ayrıştırılamadı")
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    viewModel.onSignInError("Beklenmeyen kimlik bilgisi türü")
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential: $credential")
                viewModel.onSignInError("Beklenmeyen kimlik bilgisi")
            }
        }
    }

    private fun generateNonce(): String {
        val ranNonce = UUID.randomUUID().toString()
        val bytes = ranNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
