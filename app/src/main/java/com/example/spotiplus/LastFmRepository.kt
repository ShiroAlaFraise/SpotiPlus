package com.example.spotiplus

import LastFmApiService
import Track
import TrackTag
import java.util.Locale


class LastFmRepository(private val apiService: LastFmApiService) {

    // Fonction pour récupérer les 3 top tracks d'un genre avec leur pays
    suspend fun getTopTracksWithCountry(
        genre: String,
        country: String?, // Pays de l'utilisateur (si disponible)
        apiKey: String
    ): List<TrackWithCountry> {

        // Récupérer les 10 top tracks du genre
        val topTracksResponse = apiService.getTopTracks(apiKey = apiKey, genre = genre)
        println("Réponse de getTopTracks: $topTracksResponse")  // Vérifie ce que tu reçois

        // Récupérer les informations des artistes pour chaque track
        val tracksWithCountry = mutableListOf<TrackWithCountry>()

        for (track in topTracksResponse.tracks.track) {
            val artistInfoResponse = apiService.getArtistInfo(artistName = track.artist.name, apiKey = apiKey)
            val artistCountry = artistInfoResponse.artist.country

            tracksWithCountry.add(TrackWithCountry(track, artistCountry))
        }

        // Trier les morceaux en fonction de la correspondance avec le pays de l'utilisateur
        if (country != null) {
            // Placer les morceaux avec un pays similaire en haut
            return tracksWithCountry.sortedByDescending { it.artistCountry == country }
        }

        // Si aucun pays de l'utilisateur, on retourne simplement les top tracks
        return tracksWithCountry
    }


}


// Classe pour ajouter le pays de l'artiste à chaque morceau
data class TrackWithCountry(
    val track: Track,
    val artistCountry: String? // Le pays de l'artiste
)