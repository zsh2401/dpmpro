# dpmpro
1.在 dpm 工具原有的基础上, 增加 remove-all-users 与 remove-all-accounts 功能.

2.防止某些手机无法使用 ```dpm``` 命令.
### 使用方法
1. 使用 adb 工具把编译好的 dpmpro 二进制文件 push 到手机上(例如: /data/local/tmp/dpmpro).

2. 将原来的 ``` adb shell dpm COMMAND``` 替换为 ``` adb shell CLASSPATH=/data/local/tmp/dpmpro app_process /system/bin com.android.commands.dpm.Dpm COMMAND```

### usage

usage: dpm [subcommand] [options]
usage: dpm set-active-admin [ --user <USER_ID> | current ] <COMPONENT>
usage: dpm set-device-owner [ --user <USER_ID> | current *EXPERIMENTAL* ] [ --name <NAME> ] <COMPONENT>
usage: dpm set-profile-owner [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>
usage: dpm remove-active-admin [ --user <USER_ID> | current ] [ --name <NAME> ] <COMPONENT>
usage: dpm remove-all-users
usage: dpm remove-all-accounts

dpm set-active-admin: Sets the given component as active admin for an existing user.

dpm set-device-owner: Sets the given component as active admin, and its package as device owner.

dpm set-profile-owner: Sets the given component as active admin and profile owner for an existing user.

dpm remove-active-admin: Disables an active admin, the admin must have declared android:testOnly in the application in its manifest. This will also remove device and profile owners

dpm remove-all-users: Removes all existing users.

dpm remove-all-accounts: Removes all existing accounts. Device should not have screen lock.
