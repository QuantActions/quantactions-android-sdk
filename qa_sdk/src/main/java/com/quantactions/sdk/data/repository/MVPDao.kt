/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdk.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import com.quantactions.sdk.data.entity.ActivityTransitionEntity
import com.quantactions.sdk.data.entity.CodeOfApp
import com.quantactions.sdk.data.entity.Cohort
import com.quantactions.sdk.data.entity.HourlyTapsEntity
import com.quantactions.sdk.data.entity.JournalEntryEntity
import com.quantactions.sdk.data.entity.JournalEntryJoinsJournalEventEntity
import com.quantactions.sdk.data.entity.JournalEventEntity
import com.quantactions.sdk.data.entity.Questionnaire
import com.quantactions.sdk.data.entity.QuestionnaireResponseEntity
import com.quantactions.sdk.data.entity.SleepSummaryEntity
import com.quantactions.sdk.data.entity.StatisticEntity
import com.quantactions.sdk.data.entity.StatisticStringEntity
import com.quantactions.sdk.data.entity.TrendEntity
import com.quantactions.sdk.data.model.ResolvedJournalEntries
import kotlinx.coroutines.flow.Flow


@Dao
interface MVPDao {

    // STATS
    @Query("SELECT * from stat_table WHERE stat = :statName and timestamp >= :from and timestamp <= :to ORDER BY timestamp")
    fun getMetricStatistic(statName: String, from: Long, to: Long): Flow<List<StatisticEntity>>

    @Query("SELECT * from trend_table WHERE trend = :trendCode and timestamp >= :from and timestamp <= :to ORDER BY timestamp")
    fun getTrend(trendCode: String, from: Long, to: Long): Flow<List<TrendEntity>>

    @Query("SELECT * from sleep_summary_table where timestamp >= :from and timestamp <= :to ORDER BY timestamp")
    fun getMetricSleepSummary(from: Long, to: Long): Flow<List<SleepSummaryEntity>>

    @Query("SELECT * from stat_string_table WHERE timestamp >= :from and timestamp <= :to ORDER BY timestamp")
    fun getMetricStatisticString(from: Long, to: Long): Flow<List<StatisticStringEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateMetric(stat: List<StatisticEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateTrend(stat: List<TrendEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateStringMetric(stat: List<StatisticStringEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateSleepSummary(stat: List<SleepSummaryEntity>)

    // taps
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateTapDataParsed(tapDataParsed: TapDataParsed)

    @Query("SELECT * from taps_table WHERE sync=0 ORDER BY start DESC")
    fun getTapDataParsedToSync(): List<TapDataParsed>

    @Query("SELECT * from activity_transition_table WHERE sync=0 ORDER BY timestamp DESC")
    fun getActivityToSync(): List<ActivityTransitionEntity>

    @Query("UPDATE taps_table SET sync=1 WHERE start in (:starts)")
    fun updateTapDataParsedSyncStatus(starts: List<Long>)

    @Query("UPDATE activity_transition_table SET sync=1 WHERE timestamp in (:timestamps)")
    fun updateActivitySyncStatus(timestamps: List<Long>)

    @Query("DELETE FROM taps_table WHERE id in (:idsToDelete)")
    fun removeInvalidTapSessions(idsToDelete: List<Int>)

    @Query("DELETE FROM taps_table WHERE start in (:startsToDelete)")
    fun removeInvalidTapSessionsFromStart(startsToDelete: List<Long>)

    // health
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateDeviceHealthParsed(deviceHealthParsed: DeviceHealthParsed)

    @Query("SELECT * from health_table WHERE sync=0 ORDER BY start DESC")
    fun getDeviceHealthParsedToSync(): List<DeviceHealthParsed>

    @Query("UPDATE health_table SET sync=1 WHERE start in (:starts)")
    fun updateDeviceHealthParsedSyncStatus(starts: List<Long>)

    // QUESTIONNAIRES
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT q.*, s.studyTitle FROM questionnaires q INNER JOIN studies s on q.qStudy = s.studyId ORDER BY s.studyTitle ASC;")
    fun getQuestionnaires(): List<Questionnaire>

    @Query("SELECT * FROM questionnaire_responses")
    fun getQuestionnaireResponses(): List<QuestionnaireResponseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateQuestionnaire(questionnaireEntities: List<Questionnaire>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertQuestionnaireResponse(questionnaireResponseEntity: QuestionnaireResponseEntity) : Long

    @Query("DELETE FROM questionnaires WHERE qStudy = :studyId")
    fun deleteQuestionnaires(studyId: String)

    @Query("DELETE FROM questionnaire_responses WHERE id = :id")
    fun deleteQuestionnaireResponse(id: Long)

    // STUDIES
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateStudy(cohort: Cohort)

    @Query("SELECT * from studies")
    fun getStudies(): List<Cohort>

    @Query("SELECT * from studies")
    fun getStudiesSingle(): List<Cohort>

    @Query("DELETE FROM studies WHERE studyId = :studyId")
    fun deleteStudy(studyId: String)

    // JOURNAL ENTRIES
    @Query("SELECT jjj.id as standalone_event_id, jjj.journal_event_id, jjj.journal_entry_id, jjj.rating, jen.created, jen.note, jev.public_name, jev.icon_name, jen.id as standalone_id, sleep_score.value as sleep_score_value, st_cog.value as cog_value, st_se.value as social_eng_value FROM (SELECT * from journal_entry where deleted=0) jen  LEFT JOIN journal_entry_joins_journal_event jjj  ON jjj.journal_entry_id = jen.id\n" +
            "LEFT JOIN journal_event jev ON jjj.journal_event_id = jev.id \n" +
            "LEFT JOIN (SELECT strftime('%d - %m - %Y', datetime(timestamp, 'unixepoch', 'localtime')) AS t, value\n" +
            "FROM stat_table where stat = '003-001-001-002' ORDER by timestamp desc) sleep_score ON t == strftime('%d - %m - %Y', datetime(jen.created / 1000, 'unixepoch', 'localtime'))\n" +
            "LEFT JOIN (SELECT * from stat_table where stat = '003-001-001-003') st_cog ON strftime('%d - %m - %Y', datetime(st_cog.timestamp, 'unixepoch', 'localtime')) == strftime('%d - %m - %Y', datetime(jen.created / 1000, 'unixepoch', 'localtime'))\n" +
            "LEFT JOIN (SELECT * from stat_table where stat = '003-001-001-004') st_se ON strftime('%d - %m - %Y', datetime(st_se.timestamp, 'unixepoch', 'localtime')) == strftime('%d - %m - %Y', datetime(jen.created / 1000, 'unixepoch', 'localtime')) ORDER by jen.created desc\n;")
    fun getJournal(): Flow<List<ResolvedJournalEntries>>

    @Query("SELECT * from journal_entry_joins_journal_event where journal_entry_id = :journalEntryId")
    fun getJournalEventsFromJournalEntry(journalEntryId: String): Flow<List<JournalEntryJoinsJournalEventEntity>>

    @Query("SELECT * from journal_entry_joins_journal_event where journal_entry_id = :journalEntryId")
    fun getJournalEventsFromJournalEntryAsync(journalEntryId: String): List<JournalEntryJoinsJournalEventEntity>

    @Query("SELECT * from journal_entry WHERE sync=0 and deleted=0")
    fun getPendingJournalEntries(): List<JournalEntryEntity>

    @Query("SELECT * from journal_entry WHERE sync=0 and deleted=1")
    fun getPendingJournalEntriesToDelete(): List<JournalEntryEntity>

    @Query("SELECT * from journal_entry_joins_journal_event WHERE journal_entry_id=:journalEntryId")
    fun getJournalEventsOfJournalEntry(journalEntryId: String): List<JournalEntryJoinsJournalEventEntity>

    @Query("SELECT * from journal_event ORDER BY created asc")
    fun getEvents(): List<JournalEventEntity>

    @Query("SELECT * from journal_event where id = :id")
    fun getEvent(id: String): JournalEventEntity?

    @Query("SELECT * from journal_entry where id = :id")
    fun getEntry(id: String): JournalEntryEntity?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT jjj.id as standalone_event_id, jjj.journal_event_id, jjj.journal_entry_id, jjj.rating, jen.created, jen.note, jev.public_name, jev.icon_name, jen.id as standalone_id FROM journal_entry jen\n" +
            "            LEFT JOIN journal_entry_joins_journal_event jjj  ON jjj.journal_entry_id = jen.id\n" +
            "            LEFT JOIN journal_event jev ON jjj.journal_event_id = jev.id WHERE jen.deleted=0 AND jen.id=:journalEntryId ORDER by jen.created desc;")
    fun getResolvedEventsFromEntry(journalEntryId: String): List<ResolvedJournalEntries>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateEvents(events: List<JournalEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateEntries(entries: List<JournalEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateEntry(entry: JournalEntryEntity)

    @Query("UPDATE journal_entry SET deleted=1, sync=0 WHERE id = :journalEntryId")
    fun deleteJournalEntry(journalEntryId: String)

    @Query("DELETE from journal_entry WHERE id = :journalEntryId")
    fun permanentlyDeleteJournalEntry(journalEntryId: String)

    @Query("DELETE from journal_entry_joins_journal_event WHERE id in (:journalEventIds)")
    fun permanentlyDeleteJournalEvents(journalEventIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateJournalEntryJoinsJournalEventEntity(events: List<JournalEntryJoinsJournalEventEntity>)

    @Query("UPDATE journal_entry_joins_journal_event SET id=:newId WHERE journal_entry_id=:journalEntryId and journal_event_id=:journalEventId")
    fun updateJournalEntryJoinsJournalEvent(newId: String, journalEntryId: String, journalEventId: String)

    @Query("UPDATE journal_entry SET sync=:syncStatus WHERE id=:localId")
    fun updateJournalEntry(localId: String, syncStatus: Int)

    @Query("UPDATE journal_entry SET old_id=:oldId WHERE id=:localId")
    fun updateJournalEntryOldId(localId: String, oldId: String)

    @Query("DELETE FROM taps_table where id in (:idList)")
    fun deleteWrongTapSessions(idList: List<String>)

    @Query("DELETE FROM health_table where id in (:idList)")
    fun deleteWrongHealthSessions(idList: List<String>)

    // Taps aggregator
    @Query("SELECT * FROM hourly_taps WHERE date_tap=:date AND hour=:hour")
    fun getTapsInDateAndHour(date: String, hour: Int): HourlyTapsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateHourlyTapsEntity(hourlyTapsEntities: List<HourlyTapsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAppCode(appCodes: List<CodeOfApp>): List<Long>

    @Query("SELECT * from code_of_app WHERE sync=0")
    fun getPendingAppCodes(): List<CodeOfApp>

    @Query("SELECT * from code_of_app")
    fun getListOfApps(): List<CodeOfApp>

    @Query("UPDATE code_of_app SET sync=:syncStatus WHERE id=:appId")
    fun updateCodeOfAppStatus(appId: Int, syncStatus: Int)

    @Query("SELECT * FROM hourly_taps WHERE date_tap >= :rollBackDate AND date_tap < :endDate")
    fun getTapsForStats(rollBackDate: String, endDate: String): List<HourlyTapsEntity>

    @Query("SELECT * FROM hourly_taps WHERE date_tap > :rollBackDate")
    fun getLatestTaps(rollBackDate: String): List<HourlyTapsEntity>
    @Query("UPDATE code_of_app SET sync=0 WHERE sync=1")
    fun resetAppCodesSyncStatus()

    @Query("UPDATE journal_entry SET sync=0 WHERE sync=1")
    fun resetJournalSyncStatus()

    @Query("DELETE FROM studies")
    fun deleteStudies()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActivityTransition(action: ActivityTransitionEntity)

}