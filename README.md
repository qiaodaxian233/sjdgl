MOD 名称：时间段管理插件
作者：乔大仙
适用版本：Minecraft 版本（1.21）
前置要求：Fabric Loader 和 Fabric API。
支持语言：简体中文

时间段管理
可以设置多个时间段，在这些时间段内，非白名单玩家将无法进入服务器或被自动踢出。

白名单支持
添加白名单玩家，白名单中的玩家可以在禁止时间段内正常游戏。

自定义踢出消息
通过配置文件 config.json 自定义踢出玩家时显示的消息文本。

自动重载配置文件
当管理员修改 config.json 并保存时，MOD 会自动检测到修改并重新加载配置，无需重启服务器。

下载并安装 Fabric Loader 和 Fabric API（请确保版本匹配）。
将 时间段管理插件.jar 放入服务器的 mods 文件夹中。
启动服务器，插件会自动生成配置文件 config/sjdgl/config.json。

配置文件说明
详细说明配置文件 config.json 的结构和如何修改：
{
  "block_times": [
    { "start": "12:00", "end": "15:00" },
    { "start": "19:00", "end": "22:00" }
  ],
  "whitelist": [
    "Admin",
    "Steve"
  ],
  "kick_message": "服务器维护中，暂时不可用！请稍后再试。"
}
