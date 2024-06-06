package com.example.domain.profile

import android.graphics.Bitmap
import android.util.Log
import com.example.data.repository.UserRepo
import com.example.domain.constants.LOG_KEY
import com.example.domain.constants.USERS_COLLECTION
import com.example.domain.nearby.NEAEBY_USERS_COLLECTION
import com.example.model.Profile
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import com.example.utility.Result
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class ProfileController @Inject constructor(
  private val firestore: FirebaseFirestore,
  private val firebaseAuth: FirebaseAuth,
  private val firebaseStorage: FirebaseStorage,
  private val userRepo: UserRepo,
) {

  fun getUserId(): String? {
    return firebaseAuth.currentUser?.uid
  }

  suspend fun getLocalUserProfile(userId: String): Profile {
    return userRepo.getUser(userId)!!
  }

  suspend fun setLocalUserProfile(
    profile: Profile,
    callback: (com.example.utility.Result<String>) -> Unit,
  ) {
    try {
      withContext(Dispatchers.IO) {
        userRepo.insertUser(profile)
      }
      callback(com.example.utility.Result.Success("Success"))
    } catch (e: java.lang.Exception) {
      callback(com.example.utility.Result.Failure("Failed"))
    }
  }

  fun updateUserProfile(profile: Profile, callback: (Result<String>) -> Unit) {
    try {
      val updates = hashMapOf<String, Any>(
        "username" to profile.userName,
        "firstName" to profile.firstName,
        "lastName" to profile.lastName,
        "bio" to profile.bio,
        "imageUrl" to profile.imageUrl
      )
      CoroutineScope(Dispatchers.IO).launch {
        firestore.collection(USERS_COLLECTION).document(profile.userId)
          .update(
            updates
          )
          .addOnSuccessListener {
            callback(com.example.utility.Result.Success("Success"))
          }.addOnFailureListener {
            callback(com.example.utility.Result.Failure("Failed"))

          }
      }
    } catch (e: Exception) {
      callback(com.example.utility.Result.Failure("Failed"))
    }
  }

  fun saveUsers(profile: Profile) {
    val latitude = profile.location.latitude
    val longitude = profile.location.longitude

    val geoHash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))

    val userData = hashMapOf(
      "firstName" to profile.firstName,
      "lastName" to profile.lastName,
      "location" to GeoPoint(latitude, longitude),
      "userId" to profile.userId,
      "userName" to profile.userName,
      "geohash" to geoHash
    )

    // Add the user data to Firestore
    firestore.collection(NEAEBY_USERS_COLLECTION).document(profile.userId)
      .set(userData)
      .addOnSuccessListener {
        println("User data added successfully")
      }
      .addOnFailureListener { e ->
        println("Error adding user data: $e")
      }
  }

  suspend fun getUserProfile(userId: String): com.example.utility.Result<DocumentSnapshot> {
    return try {
      val document = firestore.collection(USERS_COLLECTION).document(userId).get().await()
      com.example.utility.Result.Success(document)
    } catch (e: Exception) {
      com.example.utility.Result.Failure(e.message ?: "Failed to retrieve user profile")
    }
  }

  fun getUserStatus(userId: String, callback: (com.example.utility.Result<String>) -> Unit) {
    try {
      CoroutineScope(Dispatchers.IO).launch {
        Log.e(LOG_KEY, userId)
        val document = firestore.collection(USERS_COLLECTION).document(userId)
        document.addSnapshotListener { snapShot, e ->
          if (snapShot != null) {
            if (snapShot.exists()) {
              callback(com.example.utility.Result.Success(snapShot.get("status").toString()))
            }
          }
        }
      }
    } catch (e: Exception) {
      com.example.utility.Result.Failure(e.message ?: "Failed to retrieve user profile")
    }
  }

  fun uploadImageToStorage(
    bitmap: Bitmap,
    userId: String,
    callback: (Result<String>) -> Unit,
  ) {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val data = baos.toByteArray()

    val imagesRef = firebaseStorage.reference.child("profile_images").child("${userId}.jpg")

    val uploadTask = imagesRef.putBytes(data)
    uploadTask.addOnSuccessListener { taskSnapshot ->
      taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
        val imageUrl = uri.toString()
        callback(com.example.utility.Result.Success(imageUrl))
      }.addOnFailureListener { e ->
        callback(com.example.utility.Result.Failure(e.toString()))

      }
    }.addOnFailureListener { e ->
      callback(com.example.utility.Result.Failure(e.toString()))

    }
  }
}
