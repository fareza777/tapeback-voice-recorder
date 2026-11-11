package com.f7developer.tapeback.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.f7developer.tapeback.di.AppContainer
import com.f7developer.tapeback.domain.model.Cassette
import com.f7developer.tapeback.domain.model.CassetteSkin
import com.f7developer.tapeback.domain.model.CassetteSummary
import com.f7developer.tapeback.domain.model.RecordTarget
import com.f7developer.tapeback.domain.model.RecordingItem
import com.f7developer.tapeback.domain.model.TapeSide
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Draft edited in the cassette editor sheet. */
data class CassetteDraft(
    val id: Long? = null,
    val title: String = "",
    val category: String = "Diary",
    val labelColor: Int = 0xFFF3E6C8.toInt(),
    val skin: CassetteSkin = CassetteSkin.SMOKED_CLEAR,
    val coverPath: String? = null,
    val originalCoverPath: String? = null,
    val createdAt: Long = 0L,
) {
    companion object {
        fun from(c: Cassette) = CassetteDraft(c.id, c.title, c.category, c.labelColor, c.skin, c.coverPath, c.coverPath, c.createdAt)
    }
}

/** Shared editor logic for creating and editing cassettes. */
open class CassetteEditing(private val container: AppContainer) : ViewModel() {

    suspend fun importCover(uri: Uri): String? = container.files.importCover(uri)

    fun discardCover(path: String?) {
        container.files.deleteQuietly(path)
    }

    fun save(draft: CassetteDraft, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        val repo = container.cassetteRepository
        val id = draft.id
        if (id == null) {
            val newId = repo.create(draft.title, draft.category, draft.labelColor, draft.skin, draft.coverPath)
            onSaved(newId)
        } else {
            val existing = repo.get(id) ?: return@launch
            repo.update(
                existing.copy(
                    title = draft.title,
                    category = draft.category,
                    labelColor = draft.labelColor,
                    skin = draft.skin,
                    coverPath = draft.coverPath,
                ),
                replacedCoverPath = draft.originalCoverPath,
            )
            onSaved(id)
        }
    }
}

class LibraryViewModel(container: AppContainer) : CassetteEditing(container) {
    val cassettes: StateFlow<List<CassetteSummary>?> = container.cassetteRepository.observeSummaries()
        .map<List<CassetteSummary>, List<CassetteSummary>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class CassetteDetailState(
    val loading: Boolean = true,
    val cassette: Cassette? = null,
    val sideA: List<RecordingItem> = emptyList(),
    val sideB: List<RecordingItem> = emptyList(),
    val isLast: Boolean = false,
)

class CassetteDetailViewModel(private val container: AppContainer, private val cassetteId: Long) : CassetteEditing(container) {

    val state: StateFlow<CassetteDetailState> = combine(
        container.cassetteRepository.observe(cassetteId),
        container.recordingRepository.observeItems(),
        container.cassetteRepository.observeSummaries(),
    ) { cassette, items, all ->
        val mine = items.filter { it.recording.cassetteId == cassetteId }.sortedBy { it.recording.createdAt }
        CassetteDetailState(
            loading = false,
            cassette = cassette,
            sideA = mine.filter { it.recording.side == TapeSide.A },
            sideB = mine.filter { it.recording.side == TapeSide.B },
            isLast = all.size <= 1,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CassetteDetailState())

    fun playSide(side: TapeSide, startIndex: Int = 0) {
        val items = if (side == TapeSide.A) state.value.sideA else state.value.sideB
        if (items.isEmpty()) return
        container.playerConnection.load(items, startIndex, playWhenReady = true)
    }

    fun recordOnSide(side: TapeSide) = viewModelScope.launch {
        container.settingsRepository.setRecordTarget(RecordTarget(cassetteId, side))
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        (s.sideA + s.sideB).forEach {
            container.playerConnection.onRecordingDeleted(it.recording.id)
            container.capsuleScheduler.cancel(it.recording.id)
        }
        container.cassetteRepository.delete(cassetteId)
        container.cassetteRepository.ensureDefault()
        onDone()
    }
}
