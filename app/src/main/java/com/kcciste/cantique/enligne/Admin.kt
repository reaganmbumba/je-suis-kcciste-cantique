package com.kcciste.cantique.enligne

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Comptes administrateurs : Firebase Authentication (e-mail + mot de passe).
 * Un compte n'est administrateur que si le document admins/{uid} existe dans Firestore ;
 * ce document se crée à la main dans la console Firebase (voir GUIDE_FIREBASE.md).
 * Les règles de sécurité vérifient la même chose côté serveur : l'application ne fait qu'afficher.
 */
object Admin {
    data class Session(val uid: String, val email: String)

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()

    suspend fun connecter(email: String, motDePasse: String): Result<Session> = runCatching {
        val auth = FirebaseAuth.getInstance()
        val user = auth.signInWithEmailAndPassword(email.trim(), motDePasse).await().user ?: error("Connexion refusée")
        val estAdmin = FirebaseFirestore.getInstance().collection("admins").document(user.uid).get().await().exists()
        if (!estAdmin) {
            auth.signOut()
            error("Ce compte n'est pas administrateur.")
        }
        Session(user.uid, user.email ?: email).also { _session.value = it }
    }

    /** Vérifie, au retour dans l'application, qu'une session administrateur est encore valide. */
    suspend fun verifier() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) { _session.value = null; return }
        val ok = runCatching {
            FirebaseFirestore.getInstance().collection("admins").document(user.uid).get().await().exists()
        }.getOrDefault(false)
        _session.value = if (ok) Session(user.uid, user.email ?: "") else null
    }

    fun deconnecter() {
        FirebaseAuth.getInstance().signOut()
        _session.value = null
    }

    fun nom(): String = _session.value?.email ?: "?"
}
