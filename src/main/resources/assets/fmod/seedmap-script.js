/*
 * Adapted from Meteor Satellite Addon (https://github.com/crazycat256/meteor-satellite-addon).
 * Copyright (c) crazycat256.
 */
(() => {
    if (location.origin !== "https://www.chunkbase.com" || location.pathname !== "/apps/seed-map") {
        throw new Error("Open Chunkbase Seed Map before running the FMod script.");
    }

    window.fmodStopSeedMap?.();
    const controller = new AbortController();
    let timeout;
    let selectedPosition;
    let failures = 0;
    window.fmodStopSeedMap = () => {
        controller.abort();
        clearTimeout(timeout);
    };

    const field = (name) => document.querySelector(`[data-testid="${name}"]`);
    const setValue = (input, value, event) => {
        Object.getOwnPropertyDescriptor(Object.getPrototypeOf(input), "value").set.call(input, value);
        input.dispatchEvent(new Event(event, { bubbles: true }));
    };

    async function follow() {
        try {
            const signal = AbortSignal.any([controller.signal, AbortSignal.timeout(4000)]);
            const response = await fetch("__FMOD_ENDPOINT__", { signal });
            if (!response.ok) throw new Error(`Local Seed Map returned ${response.status}.`);
            if (response.status === 204) {
                selectedPosition = undefined;
            } else {
                const position = await response.json();
                const { x, z, dimension } = position;
                const changeDimension = selectedPosition?.dimension !== dimension;
                if (changeDimension || selectedPosition?.x !== x || selectedPosition?.z !== z) {
                    const select = field("dimension-select");
                    if (!select?.querySelector(`option[value="${dimension}"]`)) {
                        throw new Error("Select a Chunkbase version that supports the current dimension.");
                    }
                    if (changeDimension) {
                        setValue(select, dimension, "change");
                        await new Promise(requestAnimationFrame);
                        if (controller.signal.aborted) return;
                    }
                    const xInput = field("map-goto-x");
                    const zInput = field("map-goto-z");
                    const go = field("map-goto-go");
                    if (!xInput || !zInput || !go) throw new Error("Chunkbase map controls were not found.");
                    setValue(xInput, x, "input");
                    setValue(zInput, z, "input");
                    go.click();
                    selectedPosition = position;
                }
            }
            failures = 0;
        } catch (error) {
            if (!controller.signal.aborted && ++failures === 1) console.warn("FMod Seed Map", error);
        }
        if (!controller.signal.aborted) timeout = setTimeout(follow, failures ? 2000 : 500);
    }

    follow();
})();
