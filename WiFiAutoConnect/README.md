# WiFi Auto Connect

Application Android pour scanner automatiquement les réseaux Wi-Fi et se connecter aux réseaux ouverts.

## Fonctionnalités

- **Scan automatique des réseaux Wi-Fi** : Détecte tous les réseaux disponibles à proximité
- **Connexion automatique aux réseaux ouverts** : Se connecte automatiquement aux réseaux sans mot de passe
- **Historique complet** :
  - Réseaux détectés
  - Tentatives de connexion
  - Connexions réussies/échouées
  - Force du signal
  - Horodatage
- **Service en arrière-plan** : Continue de scanner même quand l'app est fermée
- **Paramètres configurables** :
  - Intervalle de scan (15s à 5min)
  - Signal minimum requis
  - Démarrage automatique au boot
  - Notifications de connexion

## Configuration requise

- **Android 7.0 (Nougat) ou supérieur** (API 24+)
- Permissions requises :
  - Localisation (pour le scan Wi-Fi)
  - Wi-Fi (lecture et modification)
  - Notifications (Android 13+)

## Structure du projet

```
WiFiAutoConnect/
├── app/
│   ├── src/main/
│   │   ├── java/ca/etrak/wifiautoconnect/
│   │   │   ├── data/           # Modèles et base de données Room
│   │   │   ├── service/        # Services et receivers
│   │   │   ├── ui/             # Activities et adapters
│   │   │   └── util/           # Utilitaires (WiFiHelper, Preferences)
│   │   └── res/                # Ressources (layouts, drawables, strings)
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## Installation

### Depuis Android Studio

1. Cloner le repository
2. Ouvrir le projet dans Android Studio
3. Synchroniser Gradle
4. Compiler et installer sur appareil/émulateur

### Compilation APK

```bash
./gradlew assembleDebug
```

L'APK sera généré dans `app/build/outputs/apk/debug/`

## Utilisation

1. **Lancer l'application**
2. **Accorder les permissions** de localisation et Wi-Fi
3. **Activer le service de scan** avec le switch
4. **Activer la connexion automatique** si désiré
5. L'application scannera périodiquement et se connectera aux réseaux ouverts

## Architecture technique

- **Kotlin** comme langage principal
- **Room Database** pour la persistance des données
- **LiveData & ViewModel** pour l'architecture MVVM
- **Coroutines** pour les opérations asynchrones
- **Material Design 3** pour l'interface utilisateur

## Notes importantes

- Sur Android 10+, les restrictions système limitent certaines fonctionnalités de connexion automatique
- La permission de localisation est obligatoire pour scanner les réseaux Wi-Fi (exigence Android)
- Le service en arrière-plan nécessite une notification persistante (Android 8+)

## Développement futur

Cette application sert de base pour tester la connexion automatique aux réseaux Wi-Fi ouverts, avec l'objectif d'intégrer cette fonctionnalité dans des applications de mobilité pour se connecter automatiquement sur la route.

## Licence

Propriété de E-Trak Solutions - Usage interne

## Contact

- **E-Trak Solutions**
- Site web: https://www.e-trak.ca/
- Email: info@e-trak.ca
