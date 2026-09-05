package br.com.williamfranco.resonance.src.services

object Constants {
    const val DATABASE_NAME = "resonance.db"
    const val SETTINGS_DATASTORE_NAME = "resonance_settings"

    /** Faixas mais curtas que isso costumam ser toques e efeitos, não músicas. */
    const val SHORT_TRACK_THRESHOLD_MS = 30_000L

    /** Cadência do relógio de posição da UI: suave sem custo perceptível. */
    const val POSITION_TICK_MS = 250L

    /** Passo da rampa de volume do crossfade. */
    const val CROSSFADE_STEP_MS = 50L

    /** Atualização de progresso do widget: rara de propósito, para poupar bateria. */
    const val WIDGET_PROGRESS_TICK_MS = 5_000L
}
