package com.mabbology.aurajournal.domain.model

data class ConnectionRequest(
    val id: String,
    val requesterId: String,
    val recipientId: String,
    val status: String,
    val counterpartyName: String,
    val counterpartyRole: String
)
