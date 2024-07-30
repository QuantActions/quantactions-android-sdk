@file:Suppress("HardCodedStringLiteral")

package com.quantactions.sdktestapp.utils

import java.time.format.DateTimeFormatter

sealed class StringFormatter(val pattern: String){
    object CreateANote : StringFormatter("dd MMMM yyyy")
    object ChartXWeek : StringFormatter("EEE")
    object ChartXYear : StringFormatter("MMM")
    object BasicDate : StringFormatter("dd/MM/yyyy")
    object DottedDate : StringFormatter("dd.MM.yyyy")
    object JournalListItem : StringFormatter("EEEE, MMMM dd")
}

val basicDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(StringFormatter.BasicDate.pattern)