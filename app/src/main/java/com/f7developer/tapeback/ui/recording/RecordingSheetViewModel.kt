package com.f7developer.tapeback.ui.recording

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.CassetteSummary
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.TapeSide
import com.f7developer.tapeback.domain.policy.CapsulePolicy
import com.f7developer.tapeback.domain.policy.DiaryLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordingSheetState(
    val loading: Boolean = true,
    val item: RecordingItem? = null,
    val cassettes: List<CassetteSummary> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
)

class RecordingSheetViewModel(private val container: AppContainer, val recordingId: Long) : ViewModel() {
    private val repo = container.recordingRepository

    val state: StateFlow<RecordingSheetState> = combine(
        repo.observeItem(recordingId),
        container.cassetteRepository.observeSummaries(),
        repo.observeItems(),
    ) { item, cassettes, all ->
        RecordingSheetState(
            loading = false,
            item = item,
            cassettes = cassettes,
            tagSuggestions = DiaryLogic.allTags(all).filter { tag -> item?.recording?.tags?.contains(tag) != true }.take(8),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordingSheetState())

    fun rename(title: String) = viewModelScope.launch { repo.rename(recordingId, title) }

    fun setFavorite(favorite: Boolean) = viewModelScope.launch { repo.setFavorite(recordingId, favorite) }

    fun addTag(tag: String) = viewModelScope.launch {
        val current = state.value.item?.recording?.tags.orEmpty()
        repo.setTags(recordingId, current + tag.split(',', ' ').filter { it.isNotBlank() })
    }

    fun removeTag(tag: String) = viewModelScope.launch {
        val current = state.value.item?.recording?.tags.orEmpty()
        repo.setTags(recordingId, current - tag)
    }

    fun move(cassetteId: Long, side: TapeSide) = viewModelScope.launch { repo.move(recordingId, cassetteId, side) }

    fun seal(unlockAt: Long, note: String?) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        if (!CapsulePolicy.isValidUnlock(unlockAt, now)) return@launch
        container.playerConnection.onRecordingDeleted(recordingId) // pull it out of the deck
        repo.seal(recordingId, unlockAt, now, note)
        container.capsuleScheduler.schedule(recordingId, unlockAt)
    }

    fun breakSeal() = viewModelScope.launch {
        repo.breakSeal(recordingId)
        container.capsuleScheduler.cancel(recordingId)
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        container.playerConnection.onRecordingDeleted(recordingId)
        container.capsuleScheduler.cancel(recordingId)
        repo.delete(recordingId)
        onDone()
    }

    /** Queue this recording's side in the deck, starting at this recording. */
    fun play() = viewModelScope.launch {
        val item = state.value.item ?: return@launch
        val side = repo.bySide(item.recording.cassetteId, item.recording.side)
        val index = side.indexOfFirst { it.recording.id == recordingId }.coerceAtLeast(0)
        container.playerConnection.load(side, index, playWhenReady = true)
    }

    fun shareIntent(): Intent? {
        val item = state.value.item ?: return null
        if (CapsulePolicy.isLocked(item.recording, System.currentTimeMillis())) return null
        return container.exporter.shareIntent(item.recording)
    }

    fun suggestedFileName(): String = state.value.item?.recording?.let(container.exporter::suggestedFileName) ?: "TapeBack.m4a"

    fun saveTo(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val item = state.value.item ?: return@launch onResult(false)
        if (CapsulePolicy.isLocked(item.recording, System.currentTimeMillis())) return@launch onResult(false)
        onResult(container.exporter.saveTo(item.recording, uri))
    }
}
