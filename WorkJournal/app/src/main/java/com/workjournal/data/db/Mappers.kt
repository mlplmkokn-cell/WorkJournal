package com.workjournal.data.db

import com.workjournal.domain.model.PhotoRecord
import com.workjournal.domain.model.WorkCategory
import com.workjournal.domain.model.WorkRecord

fun WorkRecordEntity.toDomain(photos: List<PhotoEntity>): WorkRecord = WorkRecord(
    id            = id,
    caption       = caption,
    workDate      = workDate,
    createdAt     = createdAt,
    workType      = workType,
    address       = address,
    description   = description,
    categoryName  = categoryName,
    categoryEmoji = categoryEmoji,
    latitude      = latitude,
    longitude     = longitude,
    photos        = photos.map { it.toDomain() }
)

fun PhotoEntity.toDomain(): PhotoRecord = PhotoRecord(
    id            = id,
    recordId      = recordId,
    filePath      = filePath,
    thumbnailPath = thumbnailPath,
    takenAt       = takenAt
)

fun WorkRecord.toEntity(): WorkRecordEntity = WorkRecordEntity(
    id            = id,
    caption       = caption,
    workDate      = workDate,
    createdAt     = createdAt,
    workType      = workType,
    address       = address,
    description   = description,
    categoryName  = categoryName,
    categoryEmoji = categoryEmoji,
    latitude      = latitude,
    longitude     = longitude
)

fun PhotoRecord.toEntity(): PhotoEntity = PhotoEntity(
    id            = id,
    recordId      = recordId,
    filePath      = filePath,
    thumbnailPath = thumbnailPath,
    takenAt       = takenAt
)

fun CategoryEntity.toDomain(): WorkCategory = WorkCategory(
    id        = id,
    name      = name,
    emoji     = emoji,
    isDefault = isDefault
)
