import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.MessageDigest
import java.net.URL
import org.json.JSONObject

interface LastFmApiService {

    // Récupère les 4 genres les plus populaires
    @GET("2.0/?method=chart.getTopTags")
    suspend fun getTopTags(
        @Query("api_key") apiKey: String,
        //@Query("sk") sessionKey: String, SI BESOIN POUR DES REQUETES + POUSSEES
        @Query("format") format: String = "json"
    ): TopTagsResponse

    // Fonction pour récupérer un token d'authentification
    @GET("2.0/?method=auth.getToken")
    suspend fun getTokenFromLastFM(
        @Query("method") method: String = "auth.gettoken",
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): TokenResponse

    // Récupérer les 3 top tracks d'un genre
    @GET("2.0/?method=chart.getTopTracks")
    suspend fun getTopTracks(
        @Query("api_key") apiKey: String,
        @Query("tag") genre: String, // Genre spécifique
        @Query("limit") limit: Int = 10, // Limiter à 10 morceaux
        @Query("format") format: String = "json"
    ): TopTracksResponse

    // Récupérer les informations d'un artiste, y compris le pays
    @GET("2.0/")
    suspend fun getArtistInfo(
        @Query("method") method: String = "artist.getinfo",
        @Query("artist") artistName: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): ArtistInfoResponse

    @GET("2.0/")
    suspend fun getTrackTopTags(
        @Query("method") method: String = "track.gettoptags",
        @Query("artist") artist: String,
        @Query("track") track: String,
        @Query("api_key") apiKey: String = "4bf2600dd24141838441d12b1021a3ff",
        @Query("format") format: String = "json"
    ): TrackTagsResponse

}

// Classes de réponse pour les genres
data class TokenResponse(
    val token: String
)

data class TopTagsResponse(
    val tags: Tags?
)

data class Tags(
    val tag: List<Tag>?
)

data class Tag(
    val name: String,
    val count: Int
)

data class ArtistInfoResponse(
    val artist: ArtistDetails
)

data class Artist(
    val name: String,
    val mbid: String? = null, // ID MusicBrainz de l'artiste
    val url: String
)

data class ArtistDetails(
    val name: String,
    val country: String? // Le pays de l'artiste, s'il est disponible
)

data class TopTracksResponse(
    val tracks: Tracks
)
data class Tracks(
    val track: List<Track>
)

data class Track(
    val name: String,
    val artist: Artist,
    val url: String,
    val listeners: Int,
    val playcount: Int
)
data class TrackTag(
    val name: String,
    val count: Int
)

data class TrackTagsResponse(
    val tags: List<TrackTag>
)


// Fonction pour générer une signature API
fun generateApiSignature(apiKey: String, method: String, token: String, sharedSecret: String): String {
    val stringToHash = "api_key$apiKey&method$method&token$token$sharedSecret"
    return md5(stringToHash)
}

// Fonction pour générer un hash MD5
fun md5(input: String): String {
    return MessageDigest.getInstance("MD5").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
}

// Fonction pour récupérer la session key
fun getSessionKey(apiKey: String, token: String, sharedSecret: String): String {
    val method = "auth.getSession"
    val apiSig = generateApiSignature(apiKey, method, token, sharedSecret)

    // Effectuer l'appel HTTP pour récupérer la session key
    val url = "https://ws.audioscrobbler.com/2.0/?method=$method&api_key=$apiKey&token=$token&api_sig=$apiSig&format=json"
    val response = URL(url).readText()
    val jsonObject = JSONObject(response)

    return jsonObject.getString("session_key")
}