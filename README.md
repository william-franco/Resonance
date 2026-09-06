# Resonance

Reprodutor de música local para Android, escrito em Kotlin com Jetpack Compose. O foco do
projeto é reproduzir arquivos do próprio aparelho com a menor latência possível — tocar,
pausar e trocar de faixa devem responder no mesmo instante do toque — e entregar uma
interface Material You minimalista, com o tema derivado da capa em reprodução.

Tudo roda offline: nada é enviado para fora do dispositivo e a única permissão pedida é a
de leitura de áudio.

## Funcionalidades

- Biblioteca local lida do `MediaStore` com músicas, álbuns, artistas e playlists.
- Busca por título, artista ou álbum.
- Favoritos e playlists próprias, guardadas no dispositivo.
- Player em tela cheia com transição de elemento compartilhado a partir do mini-player.
- Fila de reprodução com reordenação por arraste e remoção de faixas.
- Aleatório, repetição (uma faixa ou todas) e crossfade opcional.
- Sessão de mídia integrada à notificação, tela de bloqueio, Bluetooth/fones e Android Auto.
- Tema claro/escuro e cores vindas da capa do álbum, do Material You ou da paleta do app.
- Widgets de tela inicial em dois formatos, compacto e expandido.

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Kotlin 2.2 (JVM 21) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Arquitetura | MVVM com `StateFlow` |
| Injeção de dependências | Koin |
| Reprodução | Media3 / ExoPlayer + MediaSession |
| Persistência | Room (biblioteca, playlists, favoritos) e DataStore (preferências) |
| Cores dinâmicas | `androidx.palette` |
| Widgets | Glance |
| Testes | JUnit 4 e `kotlinx-coroutines-test` |

As capas são carregadas por um cache LRU próprio em memória, sem biblioteca de imagens: o
app só precisa decodificar `content://` locais em poucos tamanhos fixos.

## Arquitetura

O código segue MVVM organizado por feature. Cada ViewModel é declarada como `interface` mais
`Impl` no mesmo arquivo, expõe estado por `StateFlow` e é consumida nas rotas com
`collectAsStateWithLifecycle`. As `views` são Composables sem estado próprio, e as `routes`
fazem a ponte entre ViewModel e view.

```
app/src/main/java/br/com/williamfranco/resonance/
├── MainActivity.kt                  # edge-to-edge, ResonanceTheme, RoutesApp
├── ResonanceApplication.kt          # startKoin
└── src/
    ├── common/
    │   └── patterns/                # StatePattern, ResultPattern
    ├── design/
    │   ├── components/              # AlbumArt, SongRow, PlayShuffleRow, Dimens
    │   └── theme/                   # Color, Type, Theme, DominantColorScheme
    ├── di/AppModule.kt
    ├── routes/                      # Routes, RoutesApp (NavHost)
    ├── data/local/                  # entidades, DAOs, mappers, ResonanceDatabase
    ├── services/
    │   ├── library/                 # MediaStoreScanner, Artwork
    │   └── playback/                # PlaybackService, PlaybackConnection, Crossfade
    ├── features/
    │   ├── library/                 # models, repositories, view_models, views, routes
    │   ├── player/                  # view_models, views, routes
    │   └── settings/                # models, repositories, view_models, views, routes
    └── widgets/                     # PlayerWidget, receivers, WidgetState
```

O fluxo de dados é unidirecional: a UI observa ViewModels, que observam repositórios; os
repositórios falam com Room, DataStore e a sessão de mídia.

**Padrões de estado**

- `ResultPattern` — retorno tipado de operações que podem falhar (ex.: `LibraryRepository.sync()`)
- `StatePattern` — estado assíncrono da UI com `Initial`, `Loading`, `Success` e `Error`, usado
  nas ViewModels de biblioteca e coleção. Após o sync inicial, os Flows do Room continuam
  atualizando `Success` de forma reativa (favoritos, playlists, busca)

```
Compose UI ──▶ ViewModel ──▶ Repository ──▶ Room / DataStore / MediaStore
     ▲              │              │
     │         StatePattern    ResultPattern (sync)
     └──── StateFlow ─────────────┘

PlayerViewModel ──▶ PlaybackConnection ◀──▶ PlaybackService ──▶ ExoPlayer
                                                   │
                                                   └──▶ Widget Glance
```

## Baixa latência

A latência de reprodução foi tratada como requisito de arquitetura, não como ajuste final:

- **Buffers curtos**: o `DefaultLoadControl` usa `bufferForPlaybackMs = 250` e
  `bufferForPlaybackAfterRebufferMs = 500`, com `setPrioritizeTimeOverSizeThresholds(true)`.
  Para arquivos locais não há motivo para acumular buffer antes de começar.
- **Fila inteira no player**: `setMediaItems` recebe a lista completa e `prepare()` é chamado
  na montagem da fila. O ExoPlayer pré-carrega a faixa seguinte, então a troca é instantânea
  e gapless.
- **Sem leitura de metadados na reprodução**: o `DefaultExtractorsFactory` desliga o ID3
  (`FLAG_DISABLE_ID3_METADATA`) e liga a busca por bitrate constante. Título, artista e
  duração já vêm do Room, indexados uma vez pelo scan do `MediaStore`.
- **Capas fora do caminho crítico**: a arte é decodificada em tamanho reduzido e servida por
  um cache em memória, nunca durante a preparação do áudio.
- **Relógio de posição sob demanda**: o ticker que atualiza a posição na UI só existe
  enquanto há reprodução ativa.

O crossfade é opcional e implementado como rampa de volume em player único: ao entrar nos
últimos segundos da faixa o volume desce e volta a subir na seguinte. Com o crossfade
desligado o volume fica cheio e a transição segue gapless.

## Widgets de tela inicial

Um único `GlanceAppWidget` com `SizeMode.Responsive` atende os dois formatos:

- **Compacto**: capa, play/pause e próxima faixa.
- **Expandido**: capa maior, título, artista, barra de progresso e controles completos.

O estado é gravado com `updateAppWidgetState` a partir dos callbacks do `Player.Listener` no
serviço — nunca por polling. O progresso também é reenviado por um ticker de baixa
frequência que só roda quando há reprodução ativa **e** pelo menos um widget na tela inicial.
Os toques do widget viram broadcasts que um receiver traduz em comandos da sessão de mídia,
usando um `MediaController` efêmero que é liberado logo depois.

## Permissões

| Permissão | Uso |
| --- | --- |
| `READ_MEDIA_AUDIO` | Ler a biblioteca de músicas no Android 13+ |
| `READ_EXTERNAL_STORAGE` | Mesmo fim até o Android 12 (`maxSdkVersion="32"`) |
| `POST_NOTIFICATIONS` | Notificação de mídia com os controles |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Manter a reprodução com o app em segundo plano |
| `WAKE_LOCK` | Evitar que o aparelho interrompa a reprodução ao dormir |

## Como rodar localmente

Pré-requisitos:

- JDK 21 ou superior
- Android Studio recente ou apenas o Android SDK com o Gradle wrapper do projeto
- Dispositivo ou emulador com Android 10 (API 29) ou superior, com arquivos de áudio

```bash
# gerar o APK de debug
./gradlew :app:assembleDebug

# instalar em um dispositivo conectado
./gradlew :app:installDebug

# rodar os testes unitários
./gradlew :app:testDebugUnitTest
```

Na primeira execução o app pede a permissão de áudio e indexa a biblioteca. A reindexação
manual fica em **Configurações → Biblioteca → Reindexar biblioteca**.

Configuração do módulo: `minSdk 29`, `targetSdk 37`, `compileSdk 37`, `sourceCompatibility` e
`jvmTarget` em 21.

## Testes e CI

- Testes unitários das ViewModels de biblioteca, player e configurações, dos mappers do Room e
  do cálculo de volume do crossfade (`./gradlew :app:testDebugUnitTest`)
- GitHub Actions em todo `push` (`.github/workflows/android-ci.yml`), com JDK 21 e Android SDK,
  rodando os testes unitários. O build do APK permanece comentado no workflow

## Examples of commits

```
git add . && git commit -m ":rocket: Initial commit." && git push
git add . && git commit -m ":building_construction: Added initial project architecture." && git push
git add . && git commit -m ":building_construction: Update project architecture." && git push
git add . && git commit -m ":memo: Updated project documentation." && git push
git add . && git commit -m ":memo: Updated code documentation." && git push
git add . && git commit -m ":white_check_mark: Added feature xyz." && git push
git add . && git commit -m ":wrench: Fixed xyz usage." && git push
git add . && git commit -m ":heavy_minus_sign: Removed xyz." && git push
git add . && git commit -m ":memo: Adjusted project imports." && git push
git add . && git commit -m ":arrow_up: Updated dependencies." && git push
git add . && git commit -m ":arrow_down: Removed dependencies." && git push
git add . && git commit -m ":wastebasket: Removed unused code." && git push
git add . && git commit -m ":test_tube: Added test functionality xyz." && git push
git add . && git commit -m ":construction_worker: Building in progress." && git push
git add . && git commit -m ":construction_worker: Added CI build system." && git push
```

## License

MIT License

Copyright (c) 2026 William Franco

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
