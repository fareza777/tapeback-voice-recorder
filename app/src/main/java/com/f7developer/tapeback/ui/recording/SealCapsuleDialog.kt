package com.f7developer.tapeback.ui.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.f7developer.tapeback.R
import com.f7developer.tapeback.domain.policy.CapsulePolicy
import com.f7developer.tapeback.domain.policy.TimeFormat
import com.f7developer.tapeback.ui.theme.Retro
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Choose an unlock date (quick picks or a custom date) and an optional letter to your future self. */
@Composable
fun SealCapsuleDialog(onDismiss: () -> Unit, onSeal: (unlockAt: Long, note: String?) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val now = remember { System.currentTimeMillis() }
    val earliest = remember { CapsulePolicy.earliestDate(now, zone) }
    var dateEpochDay by rememberSaveable { mutableLongStateOf(CapsulePolicy.quickPickDate(CapsulePolicy.QuickPick.ONE_YEAR, now, zone).toEpochDay()) }
    var pick by rememberSaveable { mutableStateOf<String?>(CapsulePolicy.QuickPick.ONE_YEAR.name) }
    var note by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val unlockAt = CapsulePolicy.unlockInstant(date, zone)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.seal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.seal_body), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CapsulePolicy.QuickPick.entries.forEach { qp ->
                        FilterChip(
                            selected = pick == qp.name,
                            onClick = {
                                pick = qp.name
                                dateEpochDay = CapsulePolicy.quickPickDate(qp, now, zone).toEpochDay()
                            },
                            label = { Text(stringResource(quickPickLabel(qp))) },
                        )
                    }
                    FilterChip(
                        selected = pick == null,
                        onClick = { showPicker = true },
                        label = { Text(stringResource(R.string.seal_custom_date)) },
                    )
                }
                Text(
                    stringResource(R.string.seal_unlocks_on, TimeFormat.dayHeader(unlockAt, zone)),
                    style = MaterialTheme.typography.titleSmall,
                    color = Retro.palette.accent,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 500) note = it },
                    label = { Text(stringResource(R.string.seal_note_label)) },
                    placeholder = { Text(stringResource(R.string.seal_note_hint)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                )
                Text(
                    stringResource(R.string.seal_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSeal(unlockAt, note.ifBlank { null }) }, enabled = date >= earliest) {
                Text(stringResource(R.string.seal_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (showPicker) {
        val earliestUtc = earliest.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = earliest.year..(earliest.year + 50),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= earliestUtc
                override fun isSelectableYear(year: Int) = year >= earliest.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        dateEpochDay = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        pick = null
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun quickPickLabel(qp: CapsulePolicy.QuickPick): Int = when (qp) {
    CapsulePolicy.QuickPick.ONE_MONTH -> R.string.seal_1_month
    CapsulePolicy.QuickPick.SIX_MONTHS -> R.string.seal_6_months
    CapsulePolicy.QuickPick.ONE_YEAR -> R.string.seal_1_year
    CapsulePolicy.QuickPick.FIVE_YEARS -> R.string.seal_5_years
}
