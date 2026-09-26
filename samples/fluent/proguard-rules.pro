# Fluent v0.1.0 was built for Compose 1.8. Its FluentTextContextMenu calls TextContextMenu.TextManager
# members whose return types changed in Compose 1.12, so it would crash on the first right click.
# FluentSampleTheme swaps in the default text context menu on desktop, which leaves this class unused.
# Remove this once Fluent ships a release built for Compose 1.12.
# See https://github.com/compose-fluent/compose-fluent-ui/issues/164
-dontwarn io.github.composefluent.component.FluentTextContextMenu
