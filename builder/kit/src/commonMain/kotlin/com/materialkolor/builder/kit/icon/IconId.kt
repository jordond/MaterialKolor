package com.materialkolor.builder.kit.icon

/**
 * Every icon the builder draws, named for what it means rather than what it looks like.
 *
 * Widgets ask for an [IconId] and the skin's [BuilderIcons] picks the glyph, so a Material3 undo
 * and a Lucide undo come out of the same call.
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
}
