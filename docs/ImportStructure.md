How the files are organized in this project:

How the disk accessing basically works :
Access files on  disk via Safe Access Framework (SAF)
copy from SAF with metadata to app private storage
redefine the file as an entity in our app database (room)
Domain model encapsulates how our app logic sees the file 
Room : persists the entity to a local database on disk
        handles migrations
```
data/
    - StoragePaths.kt // Defines paths for storing audio on disk
    - FileImporterImpl.kt //copies from SAF to app private storage
    - MediaRepositoryImpl.kt // Enables entity <-> domain model conversion
    local/ 
        - MediaDb.kt // Room database setup
        - MediaDao.kt // Data Access Object for audio files
        - MediaItemEntity.kt // Room entity representing audio files
domain/
    port/
        - FileImporter.kt // Interface for importing files
        - MediaRepository.kt // Interface for media repository
    usecase/  
        - ImportMediaUseCase.kt // Use case for importing media files 
        - GetLibraryUseCase.kt // Flow of media files in the library 
ui/
    picker/ImportViewModel.kt // ViewModel for handling file import logic
    player/PlayerController.kt // play local files with exo media player
    library/LibraryViewModel.kt // ViewModel for managing the media library UI
app/