package com.materialkolor.sample.shared.state

import com.materialkolor.sample.shared.model.AppSection
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.theme.SampleSeed
import com.materialkolor.sample.shared.theme.ThemeMode

public sealed interface SampleAction {
    public data class AddTask(
        public val title: String,
    ) : SampleAction

    public data class ToggleTask(
        public val id: Long,
    ) : SampleAction

    public data class DeleteTask(
        public val id: Long,
    ) : SampleAction

    public data class SelectFilter(
        public val filter: TaskFilter,
    ) : SampleAction

    public data class SelectComposerTag(
        public val tag: TaskTag,
    ) : SampleAction

    public data object RequestClearDone : SampleAction

    public data object ConfirmClearDone : SampleAction

    public data object DismissClearDone : SampleAction

    public data class SelectSeed(
        public val seed: SampleSeed,
    ) : SampleAction

    public data class SelectMode(
        public val mode: ThemeMode,
    ) : SampleAction

    public data class SelectSection(
        public val section: AppSection,
    ) : SampleAction
}
