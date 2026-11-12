# Source provenance

FMod's original code is offered under the MIT License in `LICENSE`.
The previous CC0 dedication is preserved in `LICENSE-CC0`. Third-party code
retains its own licensing status; neither FMod's MIT license nor its historical
CC0 dedication relicenses imported code.

The stashhunting port in `src/main/java/addon/fmod/stashhunting` and
`src/main/java/addon/fmod/mixin/stashhunting` is adapted from
[miles352/meteor-stashhunting-addon](https://github.com/miles352/meteor-stashhunting-addon),
branch `26.1.2`, commit `e0970a639c15ce3281d85a9f03556f83dafafad4`.
The original authors include miles352 (jefff), WarriorLost (TrailFollower),
xqyet (AFKVanillaFly, VanityESP, AutoPortal), and g-a-l-a-x-i-a (EntityList).

That upstream revision does not contain a license file. Attribution records
the source of this port; it does not grant or establish permission to redistribute
the upstream work. Confirm permission from the upstream authors before
redistributing their code. `StashHunting.java` and `WebhookClient.java` are new
FMod integration code covered by FMod's MIT license.

`AutoEXPPlus.java` retains its Meteor Development copyright header and is derived
from [Meteor Client](https://github.com/MeteorDevelopment/meteor-client).
Meteor Client is licensed under GNU GPL version 3; the license text is included
in `LICENSE-Meteor`. Source and modifications are available in this repository.

FMod's pre-existing NoTextureRotations feature credits
[rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/), and
its movement commands credit
[JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod).
