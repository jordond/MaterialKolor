package com.materialkolor.builder.kit.icon

/**
 * Every icon the builder draws, named for what it means rather than what it looks like.
 *
 * Widgets ask for an [IconId] and the skin's [BuilderIcons] picks the glyph, so a Material3 undo
 * and a Lucide undo come out of the same call.
 *
 * [InfoOutline] is the info glyph drawn as an outline in every set, for a quiet button beside a
 * label. [FolderOutline] is the folder drawn the same way, for a folder in a list of files beside
 * [File] and [Archive]. [Progress] says some work is under way, and a `BuilderIcon` turns it while
 * motion is on. [Verified] says something passed a check, such as a compile check.
 */
public enum class IconId {
    Undo,
    Redo,
    Share,
    Export,
    Copy,
    Check,
    Close,
    Search,
    Command,
    Shuffle,
    Eyedropper,
    Image,
    Upload,
    Lock,
    Unlock,
    Pin,
    Info,
    InfoOutline,
    ChevronDown,
    ChevronLeft,
    ChevronRight,
    Sun,
    Moon,
    Split,
    Phone,
    Tablet,
    Desktop,
    Inspect,
    Vision,
    Fullscreen,
    More,
    Plus,
    Trash,
    Folder,
    Download,
    Warning,
    Error,
    Collapse,
    Expand,
    Keyboard,
    Help,
    ExternalLink,
    History,
    Code,
    Sliders,
    Progress,
    File,
    FolderOutline,
    Archive,
    Verified,
}
