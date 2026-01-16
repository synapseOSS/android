package com.synapse.social.studioasinc.data.model

enum class RelationshipStatus(val displayName: String) {
    SINGLE("Single"),
    IN_A_RELATIONSHIP("In a relationship"),
    MARRIED("Married"),
    DIVORCED("Divorced"),
    WIDOWED("Widowed"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(name: String?): RelationshipStatus? = values().find { it.displayName == name }
    }
}
