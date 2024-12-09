package com.example.spotiplus

import Tag
import LastFmApiService
import TopTagsResponse
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import androidx.work.workDataOf


class FetchGenresWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    // Initialiser Retrofit et l'API
    private val lastFmApiService: LastFmApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ws.audioscrobbler.com/") // URL de l'API Last.fm
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmApiService::class.java)
    }

    override suspend fun doWork(): Result {
        return try {
            // Récupérer le token passé via inputData
            val token = inputData.getString("token") ?: return Result.failure()

            // Récupérer les genres depuis l'API
            val genres = fetchGenresFromApi(token)

            // Retourner les genres comme une donnée de sortie
            val outputData = workDataOf("genres" to genres.toTypedArray())
            Result.success(outputData)
        } catch (e: Exception) {
            // Gérer l'erreur
            Result.failure()
        }
    }

    // Méthode pour récupérer les genres depuis l'API
    private suspend fun fetchGenresFromApi(token: String): List<Tag> {
        return withContext(Dispatchers.IO) {
            try {
                val response = lastFmApiService.getTopTags(token)  // Assurez-vous que la méthode API utilise le token
                response.tags?.tag ?: emptyList()  // Retourner les tags ou une liste vide en cas d'échec
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()  // Retourner une liste vide en cas d'erreur
            }
        }
    }
}
