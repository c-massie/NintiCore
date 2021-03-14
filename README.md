<h1>NintiCore</h1>

Core library mod for other mods. Includes a node-based permissions system.

<h2>Permissions system</h2>

Mods may check whether players have given permissions and possibly what string (if any) is passed as an argument to a permission. This may be used for restricting access to certain functions of mods to given players in a way that's more flexible than simply opping or not opping players.

<h3>Format</h3>

Individual permissions are dot-separated nodes. Permissions are considered to cover all other permissions that start in the same nodes. (e.g. "first.second" covers "first.second.third")

Permissions may be suffixed with ".\*" to cover all sub-permissions, but not the permission itself. (e.g. "first.second.\*" covers "first.second.third", but not "first.second")

\* refers to all permissions.

Permissions may be collated into groups, which may be assigned to players or other groups. A player or group will then be considered to "have" all of the permissions of that group, although their own permissions will override it. Groups are assigned to groups or players in the permissions files as though it were a permission prefixed with "#".

A permission group possibly listed in the permissions file as * is the default group from which all players draw permissions, being overridden by their own permissions and the permissions of any group assigned to them.

Permissions may be negated by prefixing them with "-", which may be used to deny a permission to a player or group that would otherwise have it due to the groups assigned to it or the default permissions.

Permissions may be following by a ":" followed by any arbitrary string. This is a permission argument, which can be accessed by any mods checking for it. A permission argument may be spread across multiple lines, by indenting the following line (and all succeeding lines until the end of the permission argument) by at least 4 spaces more than the permission it's a permission argument of.

<h3>Example</h3>

Example to be added at a later date.

<h2>Commands</h2>

* `permissions save`
  * Saves the current state of the permissions registry to file.
* `permissions load`
  * Loads the contents of the permissions registry file, overwriting the current contents of the permissions registry.
* `permissions initialise blank`
  * Wipes the permissions registry
* `permissions initialise presets`
  * Wipes the permissions registry and replaces the contents with the contents suggested by this and other mods.
* `permissions list [playername or group name]`
  * Lists the permissions of a player or group.
* `permissions listgroups`
  * Lists all groups in the permissions registry.
* `permissions add [playername or group id] [permission as string]`
  * Adds the given permission to the player or group. If the player or group already has the given permission directly, replaces it. This may allow you to replace the permission argument of a permission with a different one.
* `permissions remove [playername or group id] [permission as string]`
  * Removes the given permission from the player or group. The player or group may still have the given permission from a group.
* `permissions has [playername or group id] [permission as string]`
  * Checks whether or not the player or group has the given permission.
* `permissions help`
  * Gets help related to permissions.

<h2>Permissions</h2>

<h3>`ninti.permissions.read.players`</h3>

Allows you to read permissions information about players.

<h3>`ninti.permissions.read.groups`</h3>

Allows you to read permissions information about groups.

<h3>`ninti.permissions.write.players`</h3>

Allows you to modify permissions for players.

<h3>`ninti.permissions.write.groups`</h3>

Allows you to modify permissions for groups.

<h3>`ninti.permissions.files.save`</h3>

Allows you to save the permissions registry to file.

<h3>`ninti.permissions.files.load`</h3>

Allows you to load the permissions registry from file, overwriting the current contents.

<h2>Requirements</h2>

Includes (not requiring them to be included separately):

* [JavaLibrary](https://github.com/c-massie/JavaLibrary)
* [PermissionsSystem](https://github.com/c-massie/PermissionsSystem)