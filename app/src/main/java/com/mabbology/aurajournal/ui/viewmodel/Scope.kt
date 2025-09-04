package com.mabbology.aurajournal.ui.viewmodel

import com.mabbology.aurajournal.domain.model.Partner

sealed class Scope(val displayName: String, val id: String) {
    object Personal : Scope("Personal", "personal")
    data class PartnerScope(val partner: Partner, val partnerName: String) : Scope(partnerName, partner.id)
}
