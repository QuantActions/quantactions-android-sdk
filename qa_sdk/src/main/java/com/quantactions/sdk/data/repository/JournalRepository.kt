/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

package com.quantactions.sdk.data.repository

import com.hadiyarajesh.flower_core.ApiEmptyResponse
import com.hadiyarajesh.flower_core.ApiErrorResponse
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.hadiyarajesh.flower_core.Resource
import com.quantactions.sdk.Metric
import com.quantactions.sdk.data.api.NetworkManager
import com.quantactions.sdk.data.api.responses.JournalEntriesResponse
import com.quantactions.sdk.data.entity.JournalEntryEntity
import com.quantactions.sdk.data.entity.JournalEntryJoinsJournalEventEntity
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.local.MVPDao
import com.quantactions.sdk.data.model.JournalEntry
import com.quantactions.sdk.data.model.JournalEntryBody
import com.quantactions.sdk.data.model.JournalEntryEvent
import com.quantactions.sdk.data.model.JournalEventBody
import com.quantactions.sdk.data.model.JournalEventEnterResponse
import com.quantactions.sdk.exceptions.QASDKException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Repository for managing journal entries and journal event kinds.
 */
internal class JournalRepository(
    private val identityRepository: IdentityRepository,
    private val networkManager: NetworkManager,
    private val mvpDao: MVPDao
) {
    fun getJournalFromDAO(): Flow<List<JournalEntry>> {
        return mvpDao.getJournal().map { journalFromDao ->
            val journal = mutableMapOf<String, JournalEntry>()
            journalFromDao.forEach { entry ->
                journal.putIfAbsent(
                    entry.standaloneEntryId,
                    JournalEntry(
                        entry.standaloneEntryId,
                        Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalDate(),
                        entry.note
                    )
                )
                entry.journalEventId?.let {
                    journal[entry.standaloneEntryId]?.events?.add(
                        JournalEntryEvent(entry.standaloneEntryId, it, entry.publicName!!, entry.iconName!!, entry.rating)
                    )
                }
                entry.sleepScore?.let { journal[entry.standaloneEntryId]?.scores?.set(Metric.SLEEP_SCORE.code, it.roundToInt()) }
                entry.cogScore?.let { journal[entry.standaloneEntryId]?.scores?.set(Metric.COGNITIVE_FITNESS.code, it.roundToInt()) }
                entry.socScore?.let { journal[entry.standaloneEntryId]?.scores?.set(Metric.SOCIAL_ENGAGEMENT.code, it.roundToInt()) }
            }
            journal.values.toList()
        }
    }

    suspend fun cacheJournalEvents() {
        when (val response = networkManager.apiService.getJournalEventTypes(identityRepository.identityId, "{}")) {
            is ApiSuccessResponse -> mvpDao.insertOrUpdateEvents(response.body!!)
            else -> Timber.e("Failed to cache journal events")
        }
    }

    suspend fun saveJournalEntry(journalEntry: JournalEntry): JournalEntry {
        val newJournalEntryId = UUID.randomUUID().toString()
        val identityId = identityRepository.identityId

        val journalEntryBody = JournalEntryBody(
            newJournalEntryId,
            journalEntry.note,
            journalEntry.date.atStartOfDay(ZoneId.systemDefault()).toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME),
        )

        val journalEntryEntity = JournalEntryEntity(
            newJournalEntryId,
            journalEntry.note,
            identityId,
            journalEntry.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli().toString(),
            journalEntry.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli().toString(),
            0,
            0,
            ""
        )

        mvpDao.insertOrUpdateEntry(journalEntryEntity)

        val journalEntryToReturn = when (val response = networkManager.apiService.journalEntrySubmit(identityId, journalEntryBody)) {
            is ApiSuccessResponse -> {
                mvpDao.updateJournalEntry(newJournalEntryId, 1)
                journalEntry.copy(id = newJournalEntryId)
            }
            is ApiErrorResponse -> throw QASDKException("API ERROR: ${response.errorMessage}")
            else -> throw QASDKException()
        }

        val journalEventsBody = journalEntry.events.map { JournalEventBody(it.eventKindID, if (it.rating == -1) null else it.rating) }
        val journalEventsEntity = journalEntry.events.map { event ->
            JournalEntryJoinsJournalEventEntity(UUID.randomUUID().toString(), journalEntryEntity.id, event.eventKindID, event.rating ?: -1)
        }

        mvpDao.insertOrUpdateJournalEntryJoinsJournalEventEntity(journalEventsEntity)
        submitJournalEvents(journalEntryToReturn.id!!, journalEventsBody)

        if (journalEntry.id != null) {
            deleteJournalEntry(journalEntry.id)
        }

        return journalEntryToReturn
    }

    suspend fun submitJournalEvents(journalEntryId: String, events: List<JournalEventBody>) {
        when (val response = networkManager.apiService.journalEventsSubmit(identityRepository.identityId, journalEntryId, events)) {
            is ApiSuccessResponse -> response.body?.forEach { eventResponse ->
                mvpDao.updateJournalEntryJoinsJournalEvent(eventResponse.id, journalEntryId, eventResponse.journalId)
            }
            else -> Timber.e("Failed to submit journal events")
        }
    }

    suspend fun deleteJournalEntry(id: String) {
        mvpDao.deleteJournalEntry(id)
        when (val response = networkManager.apiService.journalEntryDelete(identityRepository.identityId, id)) {
            is ApiSuccessResponse, is ApiEmptyResponse -> {
                mvpDao.updateJournalEntry(id, 1)
            }
            is ApiErrorResponse -> {
                Timber.e("Failed to delete journal entry on server: ${response.errorMessage}")
                throw QASDKException("API Error [deleteJournalEntry()]: ${response.errorMessage} || ${response.httpStatusCode}")
            }
        }
    }

    fun getAndResolveJournalEntry(journalEntryId: String): JournalEntry? {
        val entry = mvpDao.getEntry(journalEntryId) ?: return null
        val events = mvpDao.getJournalEventsOfJournalEntry(journalEntryId)
        val journalEntry = JournalEntry(
            entry.id,
            Instant.ofEpochMilli(entry.created.toLong()).atZone(ZoneId.systemDefault()).toLocalDate(),
            entry.description
        )
        events.forEach { event ->
            val eventKind = mvpDao.getEvent(event.journal_event_id)
            if (eventKind != null) {
                journalEntry.events.add(JournalEntryEvent(entry.id, eventKind.id, eventKind.name, eventKind.icon, event.rating))
            }
        }
        return journalEntry
    }

    suspend fun journalEventKinds(): List<JournalEventEntity> = mvpDao.getEvents()

    fun getJournalEventsAssociatedWithEntry(journalEntryId: String): Flow<Resource<List<JournalEntryJoinsJournalEventEntity>>> {
        return mvpDao.getJournalEventsFromJournalEntry(journalEntryId).map { Resource.success(it) }
    }

    suspend fun journalEntrySubmit(body: JournalEntryBody): ApiResponse<JournalEntriesResponse> =
        networkManager.apiService.journalEntrySubmit(identityRepository.identityId, body)

    suspend fun journalEventsSubmit(journalEntryId: String, events: List<JournalEventBody>): ApiResponse<List<JournalEventEnterResponse>> =
        networkManager.apiService.journalEventsSubmit(identityRepository.identityId, journalEntryId, events)

    suspend fun simplyDeleteJournalEntry(journalId: String): ApiResponse<String> {
        return networkManager.apiService.journalEntryDelete(identityRepository.identityId, journalId)
    }

    fun deleteJournalEvent(entryId: String, eventId: String) {
        // Implementation logic for local delete
    }

    fun getPendingJournalEntries(): List<JournalEntryEntity> = mvpDao.getPendingJournalEntries()
    fun getPendingJournalEntriesToDelete(): List<JournalEntryEntity> = mvpDao.getPendingJournalEntriesToDelete()
    fun getJournalEventsOfJournalEntry(id: String): List<JournalEntryJoinsJournalEventEntity> = mvpDao.getJournalEventsOfJournalEntry(id)
    fun updateJournalEntryOldId(oldId: String, newId: String) = mvpDao.updateJournalEntryOldId(oldId, newId)
    fun updateJournalEntry(id: String, sync: Int) = mvpDao.updateJournalEntry(id, sync)
    fun resetJournalSyncStatus() = mvpDao.resetJournalSyncStatus()
    fun permanentlyDeleteJournalEntry(id: String) = mvpDao.permanentlyDeleteJournalEntry(id)
    fun permanentlyDeleteJournalEvents(ids: List<String>) = mvpDao.permanentlyDeleteJournalEvents(ids)
}
