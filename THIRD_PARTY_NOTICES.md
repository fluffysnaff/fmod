# Source provenance

Copyright (C) 2026 fluffysnaff and contributors.

FMod's original code and modifications are licensed under GNU GPL version 3
(`GPL-3.0-only`), using the license text in `LICENSE`. Existing third-party
copyright notices are retained. Earlier versions remain available in Git
history under the MIT or CC0 terms with which they were published.

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
FMod integration code covered by FMod's GPL-3.0-only license. Adopting GPL for
FMod does not itself grant rights to the upstream work without a published license.

`AutoEXPPlus.java` retains its Meteor Development copyright header and is derived
from [Meteor Client](https://github.com/MeteorDevelopment/meteor-client).
Meteor Client is licensed under GNU GPL version 3; the same license text is
included in `LICENSE`. Source and modifications are available in this repository.

FMod's pre-existing NoTextureRotations feature credits
[rfresh2/NoTextureRotations](https://github.com/rfresh2/NoTextureRotations/), and
its movement commands credit
[JorianWoltjer/LiveOverflowMod](https://github.com/JorianWoltjer/LiveOverflowMod).

## BepHax feature adaptations

The container tracker/indexer, shulker overview, stash branding, travel monitor,
sign history/signature tools, pearl operator, stash-mover area controls and
DubCounter HUD use [dekrom/BepHaxAddon](https://github.com/dekrom/BepHaxAddon),
revision `5040d95a391eb4fa3e08341c502ebcafd6613200`, as their feature/design reference.
That revision is published under GNU GPL version 3. These are modified/native
FMod implementations for Minecraft 26.3 and Meteor 26.3, not the original BepHax
binary or its full dependency bundle. Credit dekrom and the BepHax contributors.

BepHax credits [Stardust](https://github.com/0xTas/stardust) by 0xTas for
StashBrander, RoadTrip, SignHistorian and SignatureSign; Stardust also publishes
GNU GPL version 3. Those credits are preserved here. The GPL license text is
included in `LICENSE_fmod` in the distributable.

FMod's new ChestIndex is an original client-snapshot indexer sharing the native
ChestTracker backend. It does not import the JEFF ChestIndex source. The
outstanding permission issue for the existing stashhunting port described above
is unchanged; a GPL declaration on an integrating project does not resolve it.

## VolytraFly

`modules/VolytraFly.java` and `volytra/` are adapted from
[Volizray/VolytraFly-Addon](https://github.com/Volizray/VolytraFly-Addon), revision
`809295685d76eb03e77f6c270ff2d45bd78666e2`. Credit Volizray for the flight
envelope, navigation, avoidance and visual tools. The upstream project publishes
CC0 1.0; its text is preserved in `third_party/licenses/VolytraFly-CC0-1.0.txt`
and the JAR's `META-INF/licenses/VolytraFly-CC0-1.0.txt`. Meteor Development
copyright headers and the GPL terms of the Meteor-derived code are retained.
FMod integration and modifications use this project's GPL-3.0-only license.

The port changes registration, lifecycle/worker limits, flight-controller
coordination, supported coordinate labels and real-time speed measurement.

Autonav's additional swept-body collision checks and reactive route validation
use [Baritone's elytra collision checking](https://github.com/cabaletta/baritone/blob/3ddc8652adaae1500e920d82e26026707a921566/src/main/java/baritone/process/elytra/ElytraBehavior.java)
as a design reference. No Baritone source or binary is included by these changes;
Baritone remains an optional external integration.
