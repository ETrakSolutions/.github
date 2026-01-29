# Speed Sign Detector

Application Android de détection en temps réel des panneaux de vitesse du Québec utilisant la vision par ordinateur et l'apprentissage automatique.

## Fonctionnalités

- **Détection en temps réel** : Utilise la caméra du téléphone pour détecter les panneaux de vitesse
- **OCR avancé** : Reconnaissance optique de caractères avec ML Kit pour lire les valeurs de vitesse
- **Support Quebec** : Optimisé pour les panneaux de vitesse québécois (30, 40, 50, 60, 70, 80, 90, 100, 110 km/h)
- **Interface moderne** : UI Jetpack Compose avec thème sombre pour une meilleure visibilité
- **Overlay de détection** : Affichage visuel des zones de détection sur l'aperçu caméra
- **Historique** : Suivi des détections récentes

## Architecture

L'application suit une architecture **Clean Architecture** avec **MVVM** :

```
app/
├── data/
│   ├── camera/         # Module CameraX
│   ├── detection/      # Détection ML Kit
│   └── repository/     # Implémentations des repositories
├── domain/
│   ├── model/          # Modèles de données
│   ├── repository/     # Interfaces des repositories
│   └── usecase/        # Cas d'utilisation
├── di/                 # Modules Hilt (injection de dépendances)
└── ui/
    ├── components/     # Composants Compose réutilisables
    ├── screens/        # Écrans et ViewModels
    └── theme/          # Thème Material 3
```

## Technologies utilisées

| Technologie | Utilisation |
|-------------|-------------|
| **Kotlin** | Langage de programmation |
| **Jetpack Compose** | UI déclarative moderne |
| **CameraX** | API caméra unifiée |
| **ML Kit Text Recognition** | OCR pour lire les panneaux |
| **Hilt** | Injection de dépendances |
| **Coroutines & Flow** | Programmation asynchrone |
| **Material 3** | Design system |

## Prérequis

- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17
- Android SDK 34
- Appareil Android 9+ (API 28+)

## Installation

### Cloner le dépôt

```bash
git clone https://github.com/ETrakSolutions/.github.git
cd .github/SpeedSignDetector
```

### Ouvrir dans Android Studio

1. Ouvrir Android Studio
2. **File > Open** et sélectionner le dossier `SpeedSignDetector`
3. Attendre la synchronisation Gradle

### Compiler l'APK

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

Les APKs sont générés dans `app/build/outputs/apk/`

## Utilisation

1. **Lancer l'application** sur un appareil Android
2. **Accorder la permission caméra** lorsque demandé
3. **Pointer la caméra** vers un panneau de vitesse
4. **La vitesse détectée** s'affiche en bas de l'écran

### Conseils pour une meilleure détection

- Tenir le téléphone stable
- S'assurer que le panneau est bien éclairé
- Centrer le panneau dans le cadre de guidage
- Éviter les reflets sur le panneau

## Configuration

### Sensibilité de détection

La configuration se trouve dans `DetectionConfig` :

```kotlin
DetectionConfig(
    minConfidence = 0.6f,        // Confiance minimale (0.0 - 1.0)
    enableOverlay = true,        // Afficher l'overlay
    processingInterval = 150L    // Intervalle entre les traitements (ms)
)
```

## Évolution future

Cette application est conçue pour évoluer vers la détection de tous types de panneaux routiers :

### Phase 2 - Panneaux avec texte
- Détection de panneaux avec logos spécifiques
- Lecture du texte associé (ex: "ÉCOLE", "HÔPITAL")

### Phase 3 - Panneaux standards
- Panneaux STOP
- Panneaux de céder le passage
- Panneaux directionnels

### Phase 4 - Analyse avancée
- Alertes sonores
- Intégration GPS
- Historique des trajets

## CI/CD

Le projet utilise GitHub Actions pour la compilation automatique :

- **Build automatique** sur push vers `main` et branches `feature/**`, `claude/**`
- **Génération d'APK** debug et release
- **Tests unitaires** automatisés
- **Analyse Lint** du code

Les artefacts (APK) sont disponibles dans l'onglet Actions de GitHub.

## Structure des fichiers clés

| Fichier | Description |
|---------|-------------|
| `SpeedSignDetector.kt` | Classe principale de détection ML Kit |
| `CameraManager.kt` | Gestion de CameraX |
| `DetectionViewModel.kt` | ViewModel principal |
| `DetectionScreen.kt` | Écran Compose principal |
| `SpeedDisplay.kt` | Composant d'affichage de la vitesse |

## Permissions

L'application requiert :

- `CAMERA` - Pour la capture vidéo en temps réel
- `INTERNET` - Pour le téléchargement des modèles ML Kit (optionnel)

## Compatibilité

| Appareil | Status |
|----------|--------|
| Samsung Galaxy S23 | ✅ Testé |
| Android 9+ (API 28+) | ✅ Supporté |
| Android 14 (API 34) | ✅ Cible |

## Contribution

Les contributions sont les bienvenues ! Pour contribuer :

1. Forker le projet
2. Créer une branche feature (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Commiter les changements (`git commit -m 'Ajout nouvelle fonctionnalité'`)
4. Pusher la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Ouvrir une Pull Request

## Licence

Ce projet est sous licence propriétaire ETrak Solutions.

## Contact

ETrak Solutions - [https://github.com/ETrakSolutions](https://github.com/ETrakSolutions)
