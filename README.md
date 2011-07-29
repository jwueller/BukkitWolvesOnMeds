# WolvesOnMeds

This plugin heals tamed wolves over time and restores their health after being wounded<sup>1</sup>.

<sup>[1]</sup> You can tell how much health a wolf has by looking at the angle of its tail. The higher the tail, the more healthy it is. The maximum health is shown as 100 degrees.

## configuration

The configuration ist stored in plugins/WolvesOnMeds/config.yml and is automatically created when the plugin is first used. These options are currently available:

 * `heal.duration` - number of seconds it takes to heal a wolf from 0-100% health (defaults to 60)
 * `debug` - wether to display debug information in the server log (defaults to false)

## planned features

 * healing delay, so that wolves start to regain health after not being attacked for `x` seconds
 * option that allows to specify wether a wolf regains health while its owner is sleeping

Feel free to suggest something!
