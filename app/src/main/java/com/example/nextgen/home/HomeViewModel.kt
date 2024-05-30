package com.example.nextgen.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.domain.chat.ChatController
import com.example.domain.constants.LOG_KEY
import com.example.domain.profile.ProfileController
import com.example.model.Chat
import com.example.model.Profile
import com.example.nextgen.viewmodel.ObservableViewModel

class HomeViewModel(
  private val chatController: ChatController,
  private val userId:String,
  private val chatSummaryClickListener: ChatSummaryClickListener
):ObservableViewModel() {
  private var _nearbyUsers = MutableLiveData<List<HomeItemViewModel>>()
  val nearbyUsers: LiveData<List<HomeItemViewModel>> get() = _nearbyUsers

  init {
      chatController.retrieveChats(userId){result->
        if(result is com.example.utility.Result.Success){
         processData(result.data)
        }
      }
  }

  fun processData(data:MutableList<Chat>){
    val chatViewModelList:MutableList<HomeItemViewModel> = mutableListOf()
    data.forEach {
      chatViewModelList.add(ChatViewModel(it,chatSummaryClickListener))
    }
    _nearbyUsers.value=chatViewModelList
  }
}