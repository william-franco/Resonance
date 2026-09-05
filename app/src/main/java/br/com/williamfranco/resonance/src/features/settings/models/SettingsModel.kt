package br.com.williamfranco.resonance.src.features.settings.models

data class SettingsModel(
    val themeMode: ThemeMode = ThemeMode.SISTEMA,
    val colorSource: ColorSource = ColorSource.CAPA_DO_ALBUM,
    val crossfadeEnabled: Boolean = false,
    val crossfadeSeconds: Int = 4,
    val ignoreShortTracks: Boolean = true,
) {
    val crossfadeMs: Long
        get() = if (crossfadeEnabled) crossfadeSeconds * 1_000L else 0L
}

enum class ThemeMode(val label: String) {
    CLARO("Claro"),
    ESCURO("Escuro"),
    SISTEMA("Seguir o sistema"),
}

enum class ColorSource(val label: String, val description: String) {
    CAPA_DO_ALBUM("Capa do álbum", "As cores acompanham a faixa em reprodução"),
    MATERIAL_YOU("Material You", "As cores vêm do papel de parede do sistema"),
    PADRAO("Padrão do Resonance", "Paleta violeta fixa do aplicativo"),
}
