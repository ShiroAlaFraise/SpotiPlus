package com.example.spotiplus

import LastFmApiService
import MyViewModel
import Tag
import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.spotiplus.ui.theme.SpotiPlusTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class MainActivity : ComponentActivity() {

    private val apiKey = "4bf2600dd24141838441d12b1021a3ff"  // Ta clé API Last.fm
    private lateinit var lastFmApiService: LastFmApiService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userCountry: String = "France"  // Pays par défaut
    private var token: String? = null  // Pour stocker le token reçu

    // Demande de permission pour la localisation
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchUserCountry() // Récupérer le pays de l'utilisateur
        } else {
            showToast("Permission de localisation refusée.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation de lastFmApiService ici
        lastFmApiService = Retrofit.Builder()
            .baseUrl("https://ws.audioscrobbler.com/") // Base URL de l'API
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmApiService::class.java)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Vérification de la permission de localisation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserCountry() // Récupérer la localisation directement
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            SpotiPlusTheme {
                val myViewModel: MyViewModel = viewModel()

                // Collecte des genres
                val genresState by myViewModel.genresState.collectAsState(emptyList())

                // Collecte des top tracks avec pays
                val topTracksState by myViewModel.topTracksState.collectAsState(emptyList())

                val navController = rememberNavController()

                // Récupération des genres à partir du token et du pays utilisateur
                LaunchedEffect(userCountry) {
                    if (token == null) {
                        token = getToken()
                        if (token != null) {
                            myViewModel.getSessionKeyAndGenres(token!!)
                        }
                    }

                    // Appel à fetchTopTracksWithCountry après que les genres et le pays sont disponibles
                    if (userCountry.isNotEmpty()) {
                        myViewModel.fetchTopTracksWithCountry("rock", userCountry)
                    }
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController, genresState, myViewModel, userCountry)
                    }
                    composable("playlist/{genre}") { backStackEntry ->
                        val genre = backStackEntry.arguments?.getString("genre") ?: ""
                        PlaylistScreen(navController, genre, topTracksState)
                    }
                }
            }
        }
    }

    // Fonction pour obtenir le token dans onCreate()
    private suspend fun getToken(): String? {
        return try {
            val response = lastFmApiService.getTokenFromLastFM(apiKey = apiKey)
            val token = response.token
            showToast("Token reçu : $token")
            token  // Retourner le token reçu
        } catch (e: Exception) {
            showToast("Erreur réseau : ${e.message}")
            null
        }
    }

    // Fonction pour récupérer le pays à partir de la localisation
    private fun fetchUserCountry() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null) {
                        userCountry = addresses.firstOrNull()?.countryCode ?: "France"
                    }
                    showToast("Pays détecté : $userCountry")

                } catch (e: Exception) {
                    e.printStackTrace()
                    userCountry = "France" // Définir un pays par défaut si erreur
                }
            }
        }.addOnFailureListener {
            it.printStackTrace()
            userCountry = "France" // Par défaut si échec de la localisation
        }
    }

    // Affiche un Toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}




@Composable
fun HomeScreen(
    navController: NavController,
    genresState: List<Tag>,
    myViewModel: MyViewModel,
    userCountry: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fond noir
            .padding(16.dp),
        contentAlignment = Alignment.Center // Centre le contenu
    ) {
        if (genresState.isNotEmpty()) {
            Column(

            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp), // Espacement sous le texte
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Top 4 Genres Populaires:",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White // Texte en blanc pour le contraste
                    )
                }
                Spacer(modifier = Modifier.height(16.dp)) // Espacement

                // Décalage horizontal appliqué à chaque rangée
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacement horizontal
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp) // Décalage vers la droite
                ) {
                    genresState.take(2).forEach { genre -> // Premier rang de 2 boutons
                        GenreButton(
                            genre = genre,
                            navController = navController,
                            modifier = Modifier.width(150.dp) // Largeur fixe pour les boutons
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Espacement entre les rangées

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacement horizontal
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp) // Décalage vers la droite
                ) {
                    genresState.drop(2).take(2).forEach { genre -> // Second rang de 2 boutons
                        GenreButton(
                            genre = genre,
                            navController = navController,
                            modifier = Modifier.width(150.dp) // Largeur fixe pour les boutons
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Aucun genre trouvé",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White // Texte en blanc pour le contraste
            )
        }
    }
}

@Composable
fun GenreButton(genre: Tag, navController: NavController, modifier: Modifier) {
    Button(
        onClick = { navController.navigate("playlist/${genre.name}") },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF99FF99) // Vert pomme clair
        ),
        modifier = modifier
            .padding(8.dp) // Espacement autour des boutons
    ) {
        Text(text = genre.name,color = Color.Black )
    }
}



@Composable
fun PlaylistScreen(
    navController: NavController,
    genre: String,
    topTracks: List<TrackWithCountry>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fond noir
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter // Centrer le contenu
    ) {
        Column {
            // Titre de la playlist
            Text(
                text = "Playlist des Top Tracks pour le genre : $genre",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White // Texte en blanc pour le contraste
            )

            // Afficher les morceaux ou un message indiquant qu'il n'y a pas de morceaux
            if (topTracks.isNotEmpty()) {
                // Pour chaque morceau, afficher son nom, l'artiste et le pays (si disponible)
                topTracks.forEach { trackWithCountry ->
                    Text(
                        text = "\n${trackWithCountry.track.name} - ${trackWithCountry.track.artist.name}, " +
                                "Pays : ${trackWithCountry.artistCountry ?: "Inconnu"}",
                        color = Color.White // Texte en blanc pour le contraste
                    )
                }
            } else {
                // Si aucune chanson n'est trouvée, afficher un message
                Text(
                    text = "Aucun top track trouvé",
                    color = Color.White // Texte en blanc pour le contraste
                )
            }

            // Espacement entre les éléments
            Spacer(modifier = Modifier.height(16.dp))

            // Bouton Retour
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF99FF99) // Vert pomme clair
                )
            ) {
                Text(
                    text = "Retour",
                    color = Color.Black // Texte noir pour le contraste sur le bouton vert
                )
            }
        }
    }
}

