package br.com.williamfranco.resonance.src.routes

import android.net.Uri

object Routes {
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val COLLECTION = "collection"

    const val ARG_TYPE = "type"
    const val ARG_KEY = "key"

    const val COLLECTION_PATTERN = "$COLLECTION/{$ARG_TYPE}/{$ARG_KEY}"

    fun collection(type: CollectionType, key: String): String =
        "$COLLECTION/${type.name}/${Uri.encode(key)}"
}

enum class CollectionType {
    ALBUM,
    ARTISTA,
    PLAYLIST,
    FAVORITAS,
}
