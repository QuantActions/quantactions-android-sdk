/*
 * *******************************************************************************
 * Copyright (C) QuantActions AG - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Enea Ceolini <enea.ceolini@quantactions.com>, August 2024
 * *******************************************************************************
 */

@file:Suppress("unused", "ClassName", "MemberVisibilityCanBePrivate")

package com.quantactions.sdk

import androidx.annotation.Keep
import com.hadiyarajesh.flower_core.ApiResponse
import com.hadiyarajesh.flower_core.ApiSuccessResponse
import com.quantactions.sdk.data.api.ApiService
import com.quantactions.sdk.data.entity.SleepSummaryEntity
import com.quantactions.sdk.data.entity.StatisticEntity
import com.quantactions.sdk.data.entity.StatisticStringEntity
import com.quantactions.sdk.data.entity.TimestampedEntity
import com.quantactions.sdk.data.model.ScreenTimeAggregate
import com.quantactions.sdk.data.model.SleepSummary
import com.quantactions.sdk.data.repository.MVPDao
import com.quantactions.sdk.data.stringify
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Enumeration class that holds all of the info for the metrics
 * @property id name of the metric
 * @property code to get it from TIE (e.g. XXX-XXX-XXX-XXX)
 * @property eta number of days before the score is ready
 * @property range [PopulationRange] of the population distribution
 * */
@Suppress("HardCodedStringLiteral")
@Keep
sealed class Metric<P : TimestampedEntity, T>(
    val id: String,
    val code: String,
    val eta: Int,
    internal var range: PopulationRange
) : CanReturnCompiledTimeSeries<P, T> {

    /**
     * A series of detailed information for each night detected. In particular this series gives
     * information about bed time, wake up time and interruptions of sleep.
     * See [com.quantactions.sdk.data.model.SleepSummary] for more information.
     */
    @Keep
    data object SLEEP_SUMMARY : Metric<SleepSummaryEntity, SleepSummary>(
        "sleep_summary",
        "001-002-006-004",
        7,
        PopulationRange()
    ) {

        @Keep
        override fun prepareReturnData(
            values: List<SleepSummaryEntity>,
            from: Long,
            to: Long
        ): TimeSeries<SleepSummary> {
            val sleepSummaries = values.map {
                SleepSummary(
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(it.sleepStart),
                        ZoneId.of(it.timeZoneId)
                    ),
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(it.sleepEnd),
                        ZoneId.of(it.timeZoneId)
                    ),
                    it.interruptionsStart.map { s ->
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(s),
                            ZoneId.of(it.timeZoneId)
                        )
                    },
                    it.interruptionsEnd.map { s ->
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(s),
                            ZoneId.of(it.timeZoneId)
                        )
                    },
                    it.interruptionsNumberOfTaps
                )
            }

            return TimeSeries.SleepSummaryTimeTimeSeries(
                sleepSummaries,
                values.map { statistic ->
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(
                            statistic.timestamp
                        ), ZoneId.of(statistic.timeZoneId)
                    )
                },
                values.map { SleepSummary() },
                values.map { SleepSummary() },
                values.map { Double.NaN },
            )
        }

        /**
         * Use this function to retrieve the reference population values for the current user.
         * You need to provide the [BasicInfo] that you can obtain from [QA.basicInfo].
         * @param basicInfo a [BasicInfo] object.
         * @return [Range] object containing high (75th percentile) and low (25th percentile) for
         * the score in the reference healthy population.
         * */
        @Keep
        override fun getReferencePopulationRange(basicInfo: BasicInfo): Range {
            val high = range.get75thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            val low = range.get25thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            return Range(high, low)
        }

        override fun getMetric(mvpDao: MVPDao, from: Long, to: Long): Flow<List<SleepSummaryEntity>> {
            return mvpDao.getMetricSleepSummary(from, to)
        }

        override suspend fun getStat(
            apiService: ApiService,
            identityId: String,
            participationId: String,
            from: String,
            to: String,
        ): ApiResponse<List<SleepSummaryEntity>> {
            val filter = prepareFilter(code, from, to)
            return apiService.getStatSleepSummaryEntity(identityId, participationId, filter, code.container())
        }

        override fun insertOrUpdateMetric(mvpDao: MVPDao, statistics: List<SleepSummaryEntity>) {
            mvpDao.insertOrUpdateSleepSummary(statistics)
        }

        @Keep
        override fun filterScoreBasedOnTimeZone(timeSeries: TimeSeries<SleepSummary>): TimeSeries<SleepSummary> {
            return timeSeries
        }

        @Keep
        override fun returnEmptyTimeSeries(): TimeSeries<SleepSummary> {
            return TimeSeries.SleepSummaryTimeTimeSeries()
        }

        override suspend fun cacheHealthyRanges(
            apiService: ApiService,
            managePref2: ManagePref2,
            identityId: String
        ) {
            TODO("Not yet defined for this metric")
        }
//        @Keep
//        override fun prepareReturnDataStat(
//            values: List<StatisticEntity>,
//            from: Long,
//            to: Long
//        ): TimeSeries<SleepSummary> {
//            throw NotImplementedError()
//        }
//
//        override fun prepareReturnDataStatString(
//            values: List<StatisticString>,
//            from: Long,
//            to: Long
//        ): TimeSeries<SleepSummary> {
//            throw NotImplementedError()
//        }
    }

    /**
     * **Screen time is the time you spend on your smartphone.**
     *
     * Screen time is a measure of the time you spend on your smartphone.
     * */
    @Keep
    data object SCREEN_TIME_AGGREGATE : Metric<StatisticStringEntity, ScreenTimeAggregate>(
        "screen_time_aggregate",
        "003-001-001-005",
        2,
        PopulationRange(),
    ) {

        override fun prepareReturnData(
            values: List<StatisticStringEntity>,
            from: Long,
            to: Long
        ): TimeSeries<ScreenTimeAggregate> {
            return TimeSeries.ScreenTimeAggregateTimeSeries(
                values.map { statistic -> splitOrNaN(statistic.value) },
                values.map { statistic -> statistic.timestamp.localize() },
                values.map { ScreenTimeAggregate() },
                values.map { ScreenTimeAggregate() },
                values.map { Double.NaN },
            )
        }

        private fun splitOrNaN(toSplit: String): ScreenTimeAggregate {
            return if (toSplit == "") ScreenTimeAggregate()
            else {
                val split = toSplit.split(";")
                ScreenTimeAggregate(split[0].toDouble(), split[1].toDouble())
            }
        }

        override fun getReferencePopulationRange(basicInfo: BasicInfo): Range {
            val high = range.get75thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            val low = range.get25thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            return Range(high, low)
        }

        override fun getMetric(mvpDao: MVPDao, from: Long, to: Long): Flow<List<StatisticStringEntity>> {
            return mvpDao.getMetricStatisticString(from, to)
        }

        override suspend fun getStat(
            apiService: ApiService,
            identityId: String,
            participationId: String,
            from: String,
            to: String,
        ): ApiResponse<List<StatisticStringEntity>> {
            val filter = prepareFilter(code, from, to)
            return apiService.getStatStatisticStringEntity(identityId, participationId, filter, code.container())
        }

        override fun insertOrUpdateMetric(mvpDao: MVPDao, statistics: List<StatisticStringEntity>) {
            mvpDao.insertOrUpdateStringMetric(statistics)
        }

        override fun filterScoreBasedOnTimeZone(timeSeries: TimeSeries<ScreenTimeAggregate>): TimeSeries<ScreenTimeAggregate> {
            val evolution =
                timeSeries.values.filterIndexed { i, _ ->
                    timeSeries.timestamps[i].hour == 0
                }

            val timestamps = timeSeries.timestamps.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            val confidenceLow = timeSeries.confidenceIntervalLow.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            val confidenceHigh = timeSeries.confidenceIntervalHigh.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            val confidence = timeSeries.confidence.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            return TimeSeries.ScreenTimeAggregateTimeSeries(
                evolution,
                timestamps,
                confidenceLow,
                confidenceHigh,
                confidence
            )
        }

        override fun returnEmptyTimeSeries(): TimeSeries<ScreenTimeAggregate> {
            return TimeSeries.ScreenTimeAggregateTimeSeries()
        }

        override suspend fun cacheHealthyRanges(
            apiService: ApiService,
            managePref2: ManagePref2,
            identityId: String
        ) {
            TODO("Not yet defined for this metric")
        }
    }

    /**
     * Behavioral Age is a comprehensive measure of your cognitive health, showing how "young" or
     * "old" your behavior appears based on your digital activity. Unlike chronological age,
     * which just counts the years you've lived, Behavioral Age considers how your behavior and
     * cognition change over time.
     *
     * It’s normal to have days where you feel mentally younger or older, but when you face
     * diseases or significant life events—like losing a loved one or dealing with neurological
     * conditions such as stroke or epilepsy—your Behavioral Age can accelerate, widening the
     * gap between your chronological age and your Behavioral Age. This measure offers a
     * comprehensive view of your mental fitness and tracks changes in your cognitive function.
     */
    @Keep
    object BEHAVIOURAL_AGE : DoubleMetricV2(
        "age",
        "003-001-001-007",
        21,
        PopulationRange(),
    )

    /**
     * Action time refers to the amount of time it takes for you to decide and complete a task on
     * your smartphone.
     *
     * Action time is the time it typically takes you to execute simple actions, such as inputting
     * text characters, browsing or navigating between apps. The speed and efficiency with which you
     * are able to perform these actions will impact your action time. Action time can be influenced
     * by a variety of factors, including individual ability, distraction, and environmental factors.
     *
     * Tracking your action time and typing speed can help you monitor your health.
     * Slow action time or typing speed can be a sign of underlying health issues, such physical or
     * neurological disorders. By monitoring your performance, you can identify potential health
     * issues early and seek medical attention if necessary. Additionally, tracking your performance
     * can help you monitor your progress during rehabilitation if you have suffered an injury or
     * illness that has affected your performance. So, take a moment to track your performance and
     * keep an eye on your health.
     */
    @Keep
    object ACTION_SPEED : DoubleMetricV2(
        "action",
        "001-003-003-002",
        5,
        PopulationRange(),
    )

    /**
     * This metric is reported in milliseconds and represents the time efficiency of the user in
     * typing any kind of text on their smartphone.
     */
    @Keep
    object TYPING_SPEED : DoubleMetricV2(
        "typing",
        "001-003-004-002",
        5,
        PopulationRange(),
    )

    /**
     * **Cognitive fitness reflects how quickly you are able to see, understand and act.**
     *
     * Cognitive Processing Speed measures your cognitive fitness, which is a composition of various cognitive skills.
     *
     * Examples of these cognitive skills are:
     *   - The ability to focus your attention on a single stimulation
     *   - A contextual memory, that enables you to recall the source and circumstance of a certain event
     *   - Good hand-eye coordination
     *   - The ability to execute more than one action at a time
     *
     * **The higher your Cognitive Processing Speed, the more efficient your ability to think and learn.**
     *
     * Chronic stress is likely to negatively affect key cognitive functions such as memory, reaction times, attention span, and concentration skills. It is also likely that high stress levels will cause performance variability.
     *
     * When monitoring your cognitive fitness, these components are used (privacy is ensured and there's no tracking in terms of content):
     *  - Your tapping speed
     *  - Your typing speed
     *  - Your unlocking speed
     *  - Your app locating speed (how long it takes you to find apps that you're using)
     *
     * The score doesn't measure your efficiency in using your smartphone or executing one particular task. The focus is on the totality and consistency of your behaviour, not on certain maximum scores. Individual actions do not contribute to an increase or decrease in your score, e.g. if it takes you a little longer to find something because you were busy doing something else at the same time.
     *
     * Checkout our scientific literature about cognitive fitness:
     * - [Can you hear me now? Momentary increase in smartphone usage enhances neural processing of task-irrelevant sound tones](https://www.sciencedirect.com/science/article/pii/S2666956022000551?via%3Dihub)
     * - [Reopening after lockdown: the influence of working-from-home and digital device use on sleep, physical activity, and wellbeing following COVID-19 lockdown and reopening](https://academic.oup.com/sleep/article/45/1/zsab250/6390581)
     * - [Generalized priority-based model for smartphone screen touches](https://journals.aps.org/pre/abstract/10.1103/PhysRevE.102.012307)
     * - [The details of past actions on a smartphone touchscreen are reflected by intrinsic sensorimotor dynamics](https://www.nature.com/articles/s41746-017-0011-3)
     * - [Use-Dependent Cortical Processing from Fingertips in Touchscreen Phone Users](https://www.cell.com/current-biology/fulltext/S0960-9822(14)01487-0?_returnURL=https%3A%2F%2Flinkinghub.elsevier.com%2Fretrieve%2Fpii%2FS0960982214014870%3Fshowall%3Dtrue)
     * */
    @Keep
    object COGNITIVE_FITNESS : DoubleMetricV2(
        "cognitive",
        "003-001-001-003",
        4,
        PopulationRange()
    )


    /**
     * **Good sleep is crucial for pretty much everything we do.**
     *
     * Sleep is a powerful stress-reliever. It improves concentration, regulates mood, and sharpens judgment skills and decision-making.
     * A lack of sleep not only reduces mental clarity but negatively impacts your ability to cope with stressful situations.
     * So getting a good night’s sleep is incredibly important for your health. In fact, it’s just as important as eating a balanced, nutritious diet and exercising.
     *
     * Getting enough sleep has many benefits. It can help you:
     *   - get sick less often
     *   - reduce stress and improve your mood
     *   - get along better with people
     *   - increase concentration abilities and cognitive speed
     *   - make good decisions and avoid injuries
     *
     * **How much sleep is enough sleep?**
     *
     * The amount of sleep each person needs depends on many factors, including age. For most adults, 7 to 8 hours a night appears to be the best amount of sleep, although some people may need as few as 5 hours or as many as 10 hours of sleep each day.
     *
     * Your sleep is not directly recorded, only your taps on your smartphone are captured and analysed by our algorithm. This includes, for example, when you check the time on your smartphone at night.
     *
     * When monitoring your sleep, these components are used:
     *   - Duration of sleep
     *   - Regularity of sleep
     *   - Sleep interruptions (taps during the night)
     *   - Longest sleep session without interruption
     *
     * The data reflects your sleep patterns over the past 7 days and the most weight is placed on the previous night. This data is fed into a validated algorithm that predicts and estimates the likelihood of when you sleep.
     *
     *
     * Checkout our scientific literature about sleep:
     * - [Large cognitive fluctuations surrounding sleep in daily living](https://www.cell.com/iscience/fulltext/S2589-0042(21)00127-9?_returnURL=https%3A%2F%2Flinkinghub.elsevier.com%2Fretrieve%2Fpii%2FS2589004221001279%3Fshowall%3Dtrue)
     * - [Trait-like nocturnal sleep behavior identified by combining wearable, phone-use, and self-report data](https://www.nature.com/articles/s41746-021-00466-9)
     * - [Capturing sleep–wake cycles by using day-to-day smartphone touchscreen interactions](https://www.nature.com/articles/s41746-019-0147-4)
     * */
    @Keep
    object SLEEP_SCORE: DoubleMetricV2 (
        "sleep", "003-001-001-002", 7,
        PopulationRange()
    ) {
        @Keep
        override fun prepareReturnData(
            values: List<StatisticEntity>,
            from: Long,
            to: Long,
        ): TimeSeries<Double> {
            return filterScoreBasedOnTimeZone(
                TimeSeries.DoubleTimeSeries(
                    values.map { statistic -> statistic.value },
                    values.map { statistic -> statistic.timestamp.localize(ZoneId.of(statistic.timeZone)) },
                    values.map { statistic -> statistic.confidenceIntervalLow ?: Double.NaN },
                    values.map { statistic -> statistic.confidenceIntervalHigh ?: Double.NaN },
                    values.map { statistic -> statistic.confidence ?: Double.NaN },
                )
            )
        }

        /**
         * Use this function to retrieve the reference population values for the current user.
         * You need to provide the [BasicInfo] that you can obtain from [QA.basicInfo].
         * @param basicInfo a [BasicInfo] object.
         * @return [Range] object containing high (75th percentile) and low (25th percentile) for
         * the score in the reference healthy population.
         * */
        @Keep
        override fun getReferencePopulationRange(basicInfo: BasicInfo): Range {
            val high = range.get75thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            val low = range.get25thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            return Range(high, low)
        }

        override fun getMetric(mvpDao: MVPDao, from: Long, to: Long): Flow<List<StatisticEntity>> {
            return mvpDao.getMetricStatistic(code, from, to)
        }

        override suspend fun getStat(
            apiService: ApiService,
            identityId: String,
            participationId: String,
            from: String,
            to: String,
        ): ApiResponse<List<StatisticEntity>> {
            val filter = prepareFilter(code, from, to)
            return apiService.getStatStatisticEntity(identityId, participationId, filter, code.container())
        }

        override fun insertOrUpdateMetric(mvpDao: MVPDao, statistics: List<StatisticEntity>) {
            mvpDao.insertOrUpdateMetric(statistics)
        }

        @Keep
        override fun filterScoreBasedOnTimeZone(timeSeries: TimeSeries<Double>): TimeSeries<Double> {
            return timeSeries
        }

        @Keep
        override fun returnEmptyTimeSeries(): TimeSeries.DoubleTimeSeries {
            return TimeSeries.DoubleTimeSeries()
        }
    }

    /**
     * **Social Engagement is the process of engaging in digital activities in a social group. Engaging in social relationships benefits brain health.**
    * While it has been long known that social interactions are good for you, digital social engagement is a new indicator of brain health. Recent studies have linked smartphone social engagement with the production of dopamine—the hormone that helps us feel pleasure as part of the brain’s reward system. Here are some examples of smartphone social interactions:
    *
    * - Text messaging
    * - Checking social media (e.g. Facebook, Instagram etc.)
    * - Playing multi-player smartphone games
    *
    * Other ways we use our smartphones like watching videos, reading news articles, and playing single-player games do not count as social interactions. The level of digital social engagement helps us to probe brain activity (synthesis of dopamine) which consequently helps us understand more about brain health.
    *
    * Checkout our scientific literature about social engagement:
    * - [Striatal dopamine synthesis capacity reflects smartphone social activity](https://www.cell.com/iscience/fulltext/S2589-0042(21)00465-X?_returnURL=https%3A%2F%2Flinkinghub.elsevier.com%2Fretrieve%2Fpii%2FS258900422100465X%3Fshowall%3Dtrue)
    * */
    @Keep
    object SOCIAL_ENGAGEMENT : DoubleMetricV2(
        "social", "003-001-001-004", 2,
        PopulationRange()
    )

    /**
     * **Social taps are the number of taps you make on your smartphone while engaging in social activities.**
     *
     * Social taps are a measure of the number of taps you make on your smartphone while engaging in social activities. This metric is a proxy for the level of social engagement you have with your smartphone.
     *
     * Checkout our scientific literature about social taps:
     * - [Striatal dopamine synthesis capacity reflects smartphone social activity](https://www.cell.com/iscience/fulltext/S2589-0042(21)00465-X?_returnURL=https%3A%2F%2Flinkinghub.elsevier.com%2Fretrieve%2Fpii%2FS258900422100465X%3Fshowall%3Dtrue)
     * */
    @Keep
    object SOCIAL_TAPS : DoubleMetricV2(
        "social_taps", "001-005-005-011", 2,
        PopulationRange(
        )
    )


    open class DoubleMetricV2(
        id: String,
        code: String,
        eta: Int,
        range: PopulationRange
    ) : Metric<StatisticEntity, Double>(id, code, eta, range) {

        @Keep
        override fun returnEmptyTimeSeries(): TimeSeries<Double> {
            return TimeSeries.DoubleTimeSeries()
        }

        override suspend fun cacheHealthyRanges(
            apiService: ApiService,
            managePref2: ManagePref2,
            identityId: String
        ) {
            val filter = prepareFilterCode(code)

            when (val rangesResponse = apiService.getHealthyRanges(identityId, filter)) {
                is ApiSuccessResponse -> {
                    if (rangesResponse.body != null &&
                        rangesResponse.body!!.isNotEmpty()
                        )
                        this.range = rangesResponse.body!![0].ranges
                    managePref2.saveHealthyRanges(code, rangesResponse.body!![0].ranges)
                }
                else -> {
                    this.range = managePref2.getHealthyRanges(code)
                }
            }
        }

        @Keep
        override fun prepareReturnData(
            values: List<StatisticEntity>,
            from: Long,
            to: Long,
        ): TimeSeries<Double> {
            return TimeSeries.DoubleTimeSeries(
                values.map { statistic -> statistic.value },
                values.map { statistic -> statistic.timestamp.localize() },
                values.map { statistic -> statistic.confidenceIntervalLow ?: Double.NaN },
                values.map { statistic -> statistic.confidenceIntervalHigh ?: Double.NaN },
                values.map { statistic -> statistic.confidence ?: Double.NaN },
            )
        }

        /**
         * Use this function to retrieve the reference population values for the current user.
         * You need to provide the [BasicInfo] that you can obtain from [QA.basicInfo].
         * @param basicInfo a [BasicInfo] object.
         * @return [Range] object containing high (75th percentile) and low (25th percentile) for
         * the score in the reference healthy population.
         * */
        @Keep
        override fun getReferencePopulationRange(basicInfo: BasicInfo): Range {
            val high = range.get75thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            val low = range.get25thPercentile(basicInfo.yearOfBirth, basicInfo.gender)
            return Range(low, high)
        }

        override fun getMetric(mvpDao: MVPDao, from: Long, to: Long): Flow<List<StatisticEntity>> {
            return mvpDao.getMetricStatistic(code, from, to)
        }

        override suspend fun getStat(
            apiService: ApiService,
            identityId: String,
            participationId: String,
            from: String,
            to: String,
        ): ApiResponse<List<StatisticEntity>> {

            val filter = prepareFilter(code, from, to)

            return apiService.getStatStatisticEntity(identityId, participationId, filter, code.container())
        }

        override fun insertOrUpdateMetric(mvpDao: MVPDao, statistics: List<StatisticEntity>) {
            mvpDao.insertOrUpdateMetric(statistics)
        }

        @Keep
        override fun filterScoreBasedOnTimeZone(timeSeries: TimeSeries<Double>): TimeSeries<Double> {

            val evolution =
                timeSeries.values.filterIndexed { i, _ ->
                    timeSeries.timestamps[i].hour == 0
                }

            val timestamps = timeSeries.timestamps.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            val confidenceLow = timeSeries.confidenceIntervalLow.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            val confidenceHigh = timeSeries.confidenceIntervalHigh.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            val confidence = timeSeries.confidence.filterIndexed { i, _ ->
                timeSeries.timestamps[i].hour == 0
            }

            return TimeSeries.DoubleTimeSeries(
                evolution,
                timestamps,
                confidenceLow,
                confidenceHigh,
                confidence
            )
        }
    }


}

/**
 * @suppress
 */
fun String.container(): String {
    return this.split("-").first()
}

/**
 * @suppress
 */
fun prepareFilter(code: String, from: String, to: String): String {

    val filter = mutableMapOf<String, Any>()
    filter["where"] = mutableMapOf<String, Any>().apply {
        put("code", code)
        put("start", from)
        put("end", to)
    }
    filter["limit"] = 1000
    return filter.stringify()
}

fun prepareFilterCode(code: String): String {

    val filter = mutableMapOf<String, Any>()
    filter["where"] = mutableMapOf<String, Any>().apply {
        put("code", code)
    }
    return filter.stringify()
}

/**
 * @suppress
 */
@Keep
interface CanReturnCompiledTimeSeries<P : TimestampedEntity, T> {
    @Keep
    fun filterScoreBasedOnTimeZone(timeSeries: TimeSeries<T>): TimeSeries<T>

    @Keep
    fun returnEmptyTimeSeries(): TimeSeries<T>

    @Keep
    fun prepareReturnData(values: List<P>, from: Long, to: Long): TimeSeries<T>

    @Keep
    suspend fun cacheHealthyRanges(apiService: ApiService,
                                   managePref2: ManagePref2,
                           identityId: String)

    @Keep
    fun getReferencePopulationRange(basicInfo: BasicInfo): Range
    @Keep
    fun getMetric(mvpDao: MVPDao, from: Long, to: Long): Flow<List<P>>
    @Keep
    fun insertOrUpdateMetric(mvpDao: MVPDao, statistics: List<P>)
    @Keep
    suspend fun getStat(
        apiService: ApiService,
        identityId: String,
        participationId: String,
        from: String,
        to: String
    ): ApiResponse<List<P>>

}
