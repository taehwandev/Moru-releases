package app.thdev.moru.feature.git.history

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme

/** Below this the rows keep this width and the list scrolls sideways, so a subject is never cut down to a few words. */
internal val GitHistoryContentMinWidth = 880.dp

/**
 * The commit history as a graph list under a row of column names. [rows] come from [layoutGitGraph]. The graph cell
 * is as wide as the widest row on screen, so a long history whose old branches once spread over many lanes does not
 * keep that width while only one or two lanes are in view; it eases to the new width as the list scrolls. It never
 * grows so wide that the subject loses its room (see [gitRowLayout]), and the list is lazy keyed by commit hash so
 * long histories stay light. A column narrower than [GitHistoryContentMinWidth] scrolls sideways with its header
 * instead of cutting subjects short or dropping the author and hash. [nowEpochSeconds] keeps ages deterministic.
 */
@Composable
internal fun GitHistoryList(
    rows: List<GitGraphRow>,
    selectedHash: String?,
    nowEpochSeconds: Long,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val laneColors = gitGraphLaneColors(MoruTheme.colors.isLight)
    val list = rememberLazyListState()
    val visibleLaneCount by remember(rows) {
        derivedStateOf {
            val visible = list.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) rows.take(40).maxOfOrNull { it.laneCount } ?: 1
            else visible.maxOf { item -> rows.getOrNull(item.index)?.laneCount ?: 1 }
        }
    }
    val sideways = rememberScrollState()
    BoxWithConstraints(modifier = modifier) {
        val contentWidth = maxOf(maxWidth, GitHistoryContentMinWidth)
        val target = gitRowLayout(contentWidth, visibleLaneCount)
        val graphWidth by animateDpAsState(target.graphWidth, tween(180), label = "git-graph-width")
        val layout = target.copy(graphWidth = graphWidth)
        Box(Modifier.fillMaxHeight().horizontalScroll(sideways)) {
            Column(Modifier.fillMaxHeight().width(contentWidth).testTag("git-history-list")) {
                GitHistoryHeader(layout)
                LazyColumn(Modifier.fillMaxWidth().weight(1f), state = list) {
                    items(items = rows, key = { row -> row.commit.hash }) { row ->
                        GitCommitRowView(
                            row = row,
                            laneColors = laneColors,
                            graphLaneCount = maxOf(visibleLaneCount, row.laneCount),
                            layout = layout,
                            selected = row.commit.hash == selectedHash,
                            nowEpochSeconds = nowEpochSeconds,
                            onClick = { onSelect(row.commit.hash) },
                        )
                    }
                }
            }
        }
    }
}
