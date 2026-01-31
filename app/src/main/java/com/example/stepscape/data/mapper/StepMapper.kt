package com.example.stepscape.data.mapper

import com.example.stepscape.data.local.entity.StepRecord as StepEntity
import com.example.stepscape.domain.model.StepRecord as StepDomain


fun StepEntity.toDomain(): StepDomain {
    return StepDomain(
        userId = this.userId,
        date = this.date,
        steps = this.steps,
        syncedToFirebase = this.syncedToFirebase
    )
}

fun StepDomain.toEntity(): StepEntity {
    return StepEntity(
        userId = this.userId,
        date = this.date,
        steps = this.steps,
        syncedToFirebase = this.syncedToFirebase
    )
}

fun List<StepEntity>.toDomain(): List<StepDomain> {
    return this.map { it.toDomain() }
}
