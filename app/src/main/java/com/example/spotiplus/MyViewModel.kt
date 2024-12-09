import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.spotiplus.TrackWithCountry
import com.example.spotiplus.LastFmRepository


class MyViewModel : ViewModel() {

    private val apiKey = "4bf2600dd24141838441d12b1021a3ff"

    // Initialisation du LastFmApiService avec Retrofit
    private val lastFmApiService: LastFmApiService = Retrofit.Builder()
        .baseUrl("https://ws.audioscrobbler.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LastFmApiService::class.java)

    // Création du LastFmRepository
    private val lastFmRepository = LastFmRepository(lastFmApiService)

    // StateFlow pour les genres
    private val _genresState = MutableStateFlow<List<Tag>>(emptyList())
    val genresState: StateFlow<List<Tag>> get() = _genresState

    // StateFlow pour les top tracks avec pays
    private val _topTracksState = MutableStateFlow<List<TrackWithCountry>>(emptyList())
    val topTracksState: StateFlow<List<TrackWithCountry>> get() = _topTracksState

    /**
     * Récupérer les genres populaires.
     */
    fun getSessionKeyAndGenres(token: String) {
        viewModelScope.launch {
            try {
                val genres = fetchGenresFromApi()
                println("Genres reçus dans ViewModel: $genres")
                _genresState.value = genres
            } catch (e: Exception) {
                _genresState.value = emptyList()
            }
        }
    }

    /**
     * Appelle l'API pour récupérer les genres populaires.
     */
    private suspend fun fetchGenresFromApi(): List<Tag> {
        return try {
            val response = lastFmApiService.getTopTags(apiKey = apiKey)
            println("Réponse complète de l'API: $response")  // Log complet de la réponse
            val allTags = response.tags?.tag ?: emptyList()
            allTags.take(4)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Appelle l'API via le repository pour récupérer les top tracks avec pays.
     */

    fun fetchTopTracksWithCountry(genre: String, country: String) {
        viewModelScope.launch {
            try {
                // Utilisation de lastFmRepository pour récupérer les top tracks avec pays
                val tracksWithCountry = lastFmRepository.getTopTracksWithCountry(apiKey = apiKey, genre = genre, country = country)
                println("Réponse complète de getTopTracksWithCountry: $tracksWithCountry")  // Log complet de la réponse

                // Extraire uniquement la partie track et le pays, puis mettre à jour l'état
                _topTracksState.value = tracksWithCountry.map { trackWithCountry ->
                    // Créer un Track simple et associer l'artistCountry
                    TrackWithCountry(trackWithCountry.track, trackWithCountry.artistCountry)
                }
            } catch (e: Exception) {
                _topTracksState.value = emptyList()
                e.printStackTrace()
            }
        }
    }

}
