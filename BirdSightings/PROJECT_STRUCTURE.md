# Bird Sightings App - Project Structure

```
BirdSightings/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/birdsightings/app/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── BirdSighting.kt
│       │   │   ├── BirdSightingDao.kt
│       │   │   ├── BirdSightingDatabase.kt
│       │   │   └── BirdRepository.kt
│       │   ├── ui/
│       │   │   ├── MapFragment.kt
│       │   │   ├── ListFragment.kt
│       │   │   └── ImportFragment.kt
│       │   └── util/
│       │       └── CsvImporter.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── fragment_map.xml
│           │   ├── fragment_list.xml
│           │   ├── fragment_import.xml
│           │   └── item_sighting.xml
│           ├── menu/
│           │   └── bottom_nav_menu.xml
│           ├── navigation/
│           │   └── nav_graph.xml
│           └── values/
│               ├── strings.xml
│               └── themes.xml
└── build.gradle
```
