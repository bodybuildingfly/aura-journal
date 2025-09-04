package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Partner
import com.mabbology.aurajournal.domain.repository.PartnersRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


data class MainScreenState(
    val isLoading: Boolean = true,
    val scopes: List<Scope> = listOf(Scope.Personal),
    val selectedScope: Scope = Scope.Personal,
    val error: String? = null,
    val userId: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val partnersRepository: PartnersRepository,
    private val userProfilesRepository: UserProfilesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state

    init {
        viewModelScope.launch {
            val userProfileResult = userProfilesRepository.getCurrentUserProfile()
            if (userProfileResult is DataResult.Success) {
                val userId = userProfileResult.data?.userId
                _state.update { it.copy(userId = userId) }
                if (userId != null) {
                    partnersRepository.getPartners().collect { partners ->
                        val partnerScopes = partners.map { partner ->
                            val partnerName = if (partner.dominantId == userId) partner.submissiveName else partner.dominantName
                            Scope.PartnerScope(partner, partnerName)
                        }
                        val allScopes = listOf(Scope.Personal) + partnerScopes
                        _state.update { it.copy(isLoading = false, scopes = allScopes) }
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Could not retrieve user ID") }
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Could not retrieve user profile") }
            }
        }
    }

    fun selectScope(scope: Scope) {
        _state.update { it.copy(selectedScope = scope) }
    }
}
