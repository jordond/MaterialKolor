// The words a link preview shows for a theme. Style names match the app's `style_name_*` strings
// and target names its `projects_target_*` strings.
import type { Library, SharedTheme } from './code';

const LIBRARY_NAMES: Record<Library, string> = {
  Material3: 'Material 3',
  Unstyled: 'Unstyled',
  Fluent: 'Fluent',
  Custom: 'Custom',
};

/** The export target the theme was shared for, as the projects list names it. */
export function targetName(theme: SharedTheme): string {
  return theme.library === 'Material3' && theme.expressive ? 'Material 3 Expressive' : LIBRARY_NAMES[theme.library];
}

/** The page title and `og:title`, or null when the theme has no project name and the page keeps its own. */
export function themeTitle(theme: SharedTheme): string | null {
  return theme.projectName === null ? null : `${theme.projectName}, a MaterialKolor theme`;
}

export function themeDescription(theme: SharedTheme): string {
  return (
    `Seed ${theme.seed}, ${theme.style} style, for ${targetName(theme)}. ` +
    'Open this theme in MaterialKolor Builder to preview it and export Compose code.'
  );
}

/** The alt text of the card `og.ts` draws for the theme. */
export function cardAlt(theme: SharedTheme): string {
  const lead = theme.projectName === null ? 'A MaterialKolor theme card' : `${theme.projectName}, a MaterialKolor theme card`;
  const extras = theme.keyColors.length + theme.accents.length + (theme.cmfTertiarySeed === null ? 0 : 1);
  const swatches = extras === 0 ? '' : ` and ${extras} more ${extras === 1 ? 'color' : 'colors'} from the theme`;
  return `${lead}, with the seed color ${theme.seed}${swatches}, ${theme.style} style, for ${targetName(theme)}`;
}
