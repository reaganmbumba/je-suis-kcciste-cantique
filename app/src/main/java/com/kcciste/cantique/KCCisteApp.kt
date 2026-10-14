package com.kcciste.cantique

import android.app.Application
import com.kcciste.cantique.data.Recueil
import com.kcciste.cantique.data.ReglagesRepository
import com.kcciste.cantique.enligne.ContenuEnLigne

class KCCisteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReglagesRepository.charger(this)
        Recueil.charger(this)
        // Corrections, traductions et audios publiés par l'administrateur (si Firebase est configuré).
        runCatching { ContenuEnLigne.demarrer(this) }
    }
}
