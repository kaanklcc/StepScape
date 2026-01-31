package com.example.stepscape.data.mapper

import com.example.stepscape.data.local.entity.StepSession as StepSessionEntity
import com.example.stepscape.domain.model.StepSession as StepSessionDomain


fun StepSessionEntity.toDomain(): StepSessionDomain {
    return StepSessionDomain(
        userId = this.userId,
        startTime = this.startTime,
        endTime = this.endTime,
        steps = this.steps,
        syncedToFirebase = this.syncedToFirebase
    )
}

fun StepSessionDomain.toEntity(): StepSessionEntity {
    return StepSessionEntity(
        userId = this.userId,
        startTime = this.startTime,
        endTime = this.endTime,
        steps = this.steps,
        syncedToFirebase = this.syncedToFirebase
    )
}

fun List<StepSessionEntity>.toSessionDomain(): List<StepSessionDomain> {
    return this.map { it.toDomain() }
}
