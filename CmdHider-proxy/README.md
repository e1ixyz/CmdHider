# CmdHider Proxy

Velocity proxy (3.3.x) version of CmdHider that hides commands and namespaced aliases based on LuckPerms permissions and per-group blocklists.

## Building
- Requires Java 21.
- `mvn package` (artifact: `target/cmdhider-proxy-1.0.0.jar`).

## Config
- `messages.unknown-command` / `messages.no-permission` customize responses.
- `options.hide-namespaced`, `options.hide-subcommand-suggestions`, `options.filter-by-permission`, `options.replace-*`, `options.debug` mirror the Paper plugin.
- `exceptions.per-group.<group>.always-hide` / `.always-show` apply to LuckPerms primary groups (e.g., `default`); `exceptions.always-*` are global overrides.
- Commands in any `always-hide` are blocked and return the configured no-permission message.

Drop the jar into Velocity's `plugins/` directory; a default `config.toml` will be generated on first run.
