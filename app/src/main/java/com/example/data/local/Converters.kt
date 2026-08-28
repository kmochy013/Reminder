package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Priority
import com.example.data.model.RecurrenceType

class Converters {

    @TypeConverter
    fun fromRecurrence(value: RecurrenceType?): String = value?.name ?: RecurrenceType.NONE.name

    @TypeConverter
    fun toRecurrence(value: String?): RecurrenceType = try {
        RecurrenceType.valueOf(value ?: "NONE")
    } catch (e: Exception) {
        RecurrenceType.NONE
    }

    @TypeConverter
    fun fromPriority(value: Priority?): String = value?.name ?: Priority.MEDIUM.name

    @TypeConverter
    fun toPriority(value: String?): Priority = try {
        Priority.valueOf(value ?: "MEDIUM")
    } catch (e: Exception) {
        Priority.MEDIUM
    }
}
