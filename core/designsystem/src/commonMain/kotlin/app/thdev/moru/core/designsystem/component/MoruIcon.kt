package app.thdev.moru.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thdev.moru.core.designsystem.theme.MoruTheme
import com.composables.icons.lucide.Brain
import com.composables.icons.lucide.MessageSquarePlus
import com.composables.icons.lucide.ChartColumn
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CircleQuestionMark
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Flag
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Loader
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.ArchiveRestore
import com.composables.icons.lucide.PanelRight
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.ArrowDownLeft
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.SquareCheckBig
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.Target
import com.composables.icons.lucide.X
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.CloudDownload
import com.composables.icons.lucide.ArrowDownToLine
import com.composables.icons.lucide.ArrowUpFromLine
import com.composables.icons.lucide.GitBranch
import com.composables.icons.lucide.GitBranchPlus
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Trees
import com.composables.icons.lucide.GitMerge
import com.composables.icons.lucide.ArrowRightLeft
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.GitCommitHorizontal
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.Apple
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.SlidersVertical
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Terminal
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Cpu
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.ChevronsDownUp
import com.composables.icons.lucide.ChevronsUpDown
import com.composables.icons.lucide.File
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Paperclip
import com.composables.icons.lucide.Send

/**
 * One icon, tinted from the theme so it follows light and dark. Pass [description] only where the icon stands alone
 * without a text label next to it; beside a label it is decoration and stays silent for screen readers.
 */
@Composable
fun MoruIcon(
    name: MoruIconName,
    modifier: Modifier = Modifier,
    description: String? = null,
    size: Dp = 14.dp,
    tint: Color = MoruTheme.colors.textSecondary,
) {
    Image(
        imageVector = name.vector(),
        contentDescription = description,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

private fun MoruIconName.vector(): ImageVector =
    when (this) {
        MoruIconName.ChevronDown -> Lucide.ChevronDown
        MoruIconName.ChevronRight -> Lucide.ChevronRight
        MoruIconName.Close -> Lucide.X
        MoruIconName.Plus -> Lucide.Plus
        MoruIconName.Minus -> Lucide.Minus
        MoruIconName.Check -> Lucide.Check
        MoruIconName.Square -> Lucide.Square
        MoruIconName.SquareChecked -> Lucide.SquareCheckBig
        MoruIconName.Play -> Lucide.Play
        MoruIconName.Flag -> Lucide.Flag
        MoruIconName.Circle -> Lucide.Circle
        MoruIconName.Loader -> Lucide.Loader
        MoruIconName.Target -> Lucide.Target
        MoruIconName.Pencil -> Lucide.Pencil
        MoruIconName.Question -> Lucide.CircleQuestionMark
        MoruIconName.Ellipsis -> Lucide.Ellipsis
        MoruIconName.Tag -> Lucide.Tag
        MoruIconName.FolderPlus -> Lucide.FolderPlus
        MoruIconName.Reopen -> Lucide.ArchiveRestore
        MoruIconName.PanelRight -> Lucide.PanelRight
        MoruIconName.Detach -> Lucide.ExternalLink
        MoruIconName.Dock -> Lucide.ArrowDownLeft
        MoruIconName.Refresh -> Lucide.RefreshCw
        MoruIconName.Fetch -> Lucide.CloudDownload
        MoruIconName.Pull -> Lucide.ArrowDownToLine
        MoruIconName.Push -> Lucide.ArrowUpFromLine
        MoruIconName.Branch -> Lucide.GitBranch
        MoruIconName.BranchPlus -> Lucide.GitBranchPlus
        MoruIconName.Home -> Lucide.House
        MoruIconName.Worktree -> Lucide.Trees
        MoruIconName.Cloud -> Lucide.Cloud
        MoruIconName.Merge -> Lucide.GitMerge
        MoruIconName.Switch -> Lucide.ArrowRightLeft
        MoruIconName.Trash -> Lucide.Trash2
        MoruIconName.Commit -> Lucide.GitCommitHorizontal
        MoruIconName.Search -> Lucide.Search
        MoruIconName.Code -> Lucide.Code
        MoruIconName.Globe -> Lucide.Globe
        MoruIconName.Smartphone -> Lucide.Smartphone
        MoruIconName.Bot -> Lucide.Bot
        MoruIconName.Apple -> Lucide.Apple
        MoruIconName.Settings -> Lucide.Settings
        MoruIconName.Sliders -> Lucide.SlidersVertical
        MoruIconName.Bell -> Lucide.Bell
        MoruIconName.Palette -> Lucide.Palette
        MoruIconName.Terminal -> Lucide.Terminal
        MoruIconName.Download -> Lucide.Download
        MoruIconName.Cpu -> Lucide.Cpu
        MoruIconName.Sparkles -> Lucide.Sparkles
        MoruIconName.About -> Lucide.Info
        MoruIconName.Folder -> Lucide.Folder
        MoruIconName.FolderOpen -> Lucide.FolderOpen
        MoruIconName.File -> Lucide.File
        MoruIconName.CollapseAll -> Lucide.ChevronsDownUp
        MoruIconName.ExpandAll -> Lucide.ChevronsUpDown
        MoruIconName.Chart -> Lucide.ChartColumn
        MoruIconName.Send -> Lucide.Send
        MoruIconName.Copy -> Lucide.Copy
        MoruIconName.Attach -> Lucide.Paperclip
        MoruIconName.Thinking -> Lucide.Brain
        MoruIconName.Comment -> Lucide.MessageSquarePlus
    }
