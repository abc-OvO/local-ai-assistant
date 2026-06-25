const streamTarget = document.querySelector("[data-stream]");

if (streamTarget) {
  const fullText = streamTarget.dataset.stream || "";
  let index = 0;
  let paused = 0;

  function tick() {
    if (paused > 0) {
      paused -= 1;
      window.setTimeout(tick, 90);
      return;
    }

    streamTarget.textContent = fullText.slice(0, index);
    index += 1;

    if (index > fullText.length) {
      paused = 12;
      index = 0;
    }

    window.setTimeout(tick, index === 1 ? 280 : 34);
  }

  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    streamTarget.textContent = fullText;
  } else {
    streamTarget.textContent = "";
    tick();
  }
}
