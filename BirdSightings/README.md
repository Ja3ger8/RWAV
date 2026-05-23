# Bird Sightings Android App

An Android app to import, store, and explore bird sighting records from CSV files.

## Features

- **CSV Import** — Pick any CSV file from device storage
- **SQLite Database** — All sightings stored locally on the device using Room
- **Map View** — OpenStreetMap (no API key required), with a tap-able marker for each sighting
- **List View** — Scrollable table of all sightings
- **Filter/Search** — Filter by bird name (with autocomplete) and/or date
- **Clear Data** — Delete all records and start fresh

---

## Project Setup in Android Studio

1. Open Android Studio → **File → Open** → select the `BirdSightings/` folder
2. Wait for Gradle sync to complete (it will download dependencies automatically)
3. Connect a device or start an emulator
4. Click **Run ▶**

### Minimum Requirements
- Android Studio Panda 4 (2025.3.4) or later
- Android SDK 24+ (Android 7.0)
- Internet access on the device (for downloading map tiles)

---

## CSV File Format

The first row must be a header row. Column names are **case-insensitive**.

| Column | Required | Notes |
|---|---|---|
| `Location` | ✅ | Place name or description |
| `Bird Name` | ✅ | Species name |
| `Date` | ✅ | Any format (e.g. 2024-03-15) |
| `X Coordinate` | ✅ | Longitude (decimal degrees) |
| `Y Coordinate` | ✅ | Latitude (decimal degrees) |

### Example CSV

```
Location,Bird Name,Date,X Coordinate,Y Coordinate
Kakadu National Park,Jabiru,2024-03-15,132.9741,-12.6630
Royal Botanic Gardens Sydney,Rainbow Lorikeet,2024-04-01,151.2143,-33.8642
```

A sample file is included at `sample_data.csv`.

---

## Project Structure

```
app/src/main/
├── java/com/birdsightings/app/
│   ├── MainActivity.kt              # Entry point, hosts bottom navigation
│   ├── data/
│   │   ├── BirdSighting.kt          # Room entity (database table model)
│   │   ├── BirdSightingDao.kt       # Database queries
│   │   ├── BirdSightingDatabase.kt  # Room database singleton
│   │   └── BirdRepository.kt       # Data access layer
│   ├── ui/
│   │   ├── BirdViewModel.kt         # Shared ViewModel (state + business logic)
│   │   ├── MapFragment.kt           # OSMDroid map with markers
│   │   ├── ListFragment.kt          # RecyclerView list + filter bar
│   │   └── ImportFragment.kt        # File picker + import status
│   └── util/
│       └── CsvImporter.kt           # CSV parsing logic
└── res/
    ├── layout/                      # XML layouts for each screen
    ├── menu/bottom_nav_menu.xml     # Bottom navigation tabs
    ├── navigation/nav_graph.xml     # Navigation component graph
    └── values/                      # Strings, colors, themes
```

## Key Libraries

| Library | Purpose |
|---|---|
| **Room** | SQLite ORM — stores sightings on-device |
| **OSMDroid** | OpenStreetMap rendering — no API key needed |
| **OpenCSV** | Robust CSV parsing |
| **Navigation Component** | Fragment navigation + bottom nav |
| **ViewModel + LiveData** | Reactive UI state management |
| **Coroutines** | Async database and file operations |

---

## How It Works

1. **Import tab** → tap "Select CSV File" → pick your file from device storage
2. The app parses the CSV, validates columns and coordinates, and bulk-inserts into SQLite
3. **Map tab** → shows all sightings as markers; tap a marker to see bird name, location, and date
4. **List tab** → shows all sightings in a scrollable list; type in the filter boxes to narrow results
