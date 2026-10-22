package com.workjournal.data.db

import com.workjournal.domain.model.*

fun WorkRecordEntity.toDomain(
    photos: List<PhotoEntity>,
    customFields: List<CustomFieldEntity>
): WorkRecord = WorkRecord(
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
    photos        = photos.map { it.toDomain() },
    customFields  = customFields.map { it.toDomain() }
)

fun PhotoEntity.toDomain(): PhotoRecord = PhotoRecord(
    id = id, recordId = recordId, filePath = filePath,
    thumbnailPath = thumbnailPath, takenAt = takenAt
)

fun CustomFieldEntity.toDomain(): CustomField = CustomField(
    id = id, recordId = recordId,
    fieldName = fieldName, fieldValue = fieldValue
)

fun WorkRecord.toEntity(): WorkRecordEntity = WorkRecordEntity(
    id = id, caption = caption, workDate = workDate, createdAt = createdAt,
    workType = workType, address = address, description = description,
    categoryName = categoryName, categoryEmoji = categoryEmoji,
    latitude = latitude, longitude = longitude
)

fun PhotoRecord.toEntity(): PhotoEntity = PhotoEntity(
    id = id, recordId = recordId, filePath = filePath,
    thumbnailPath = thumbnailPath, takenAt = takenAt
)

fun CustomField.toEntity(): CustomFieldEntity = CustomFieldEntity(
    id = id, recordId = recordId,
    fieldName = fieldName, fieldValue = fieldValue
)

fun CategoryEntity.toDomain(): WorkCategory = WorkCategory(
    id = id, name = name, emoji = emoji, isDefault = isDefault
)