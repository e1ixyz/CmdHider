# CmdHider

Paper 1.21.10 plugin that hides commands, namespaced aliases, and subcommand suggestions based on LuckPerms permissions.

## Building
- Requires Java 21.
- `mvn package` (artifact lands in `target/cmdhider-1.0.0.jar`).

## Config
- `messages.unknown-command` / `messages.no-permission` customize the responses.
- `options.hide-namespaced` strips `plugin:command` aliases from tab-complete.
- `options.hide-subcommand-suggestions` clears completions after the first argument when the base command is hidden.
- `options.filter-by-permission` uses LuckPerms to only show usable commands.
- `exceptions.per-group.<group>.always-hide` / `.always-show` apply to a LuckPerms primary group (e.g., `default`).
- `exceptions.always-show` / `exceptions.always-hide` are global overrides applied after group rules. Entries in any `always-hide` are blocked from running and return the configured no-permission message.

Use `/cmdhider reload` after editing `config.yml`.
