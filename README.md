# CmdHider

Paper 1.21.10 plugin that hides commands, namespaced aliases, and subcommand suggestions based on LuckPerms permissions.

## Building
- Requires Java 21.
- `mvn package` (artifact lands in `target/cmdhider-1.0.0.jar`).

## Config
- `messages.unknown-command` / `messages.no-permission` customize the responses.
- `options.hide-namespaced` strips `plugin:command` aliases from tab-complete.
- `options.hide-subcommand-suggestions` clears completions after the first argument.
- `options.filter-by-permission` uses LuckPerms to only show usable commands.
- `exceptions.always-show` / `exceptions.always-hide` override visibility.
- Entries in `exceptions.always-hide` are blocked from running and return the configured no-permission message.

Use `/cmdhider reload` after editing `config.yml`.
